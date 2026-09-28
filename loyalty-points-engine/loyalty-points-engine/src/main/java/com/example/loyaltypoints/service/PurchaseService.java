package com.example.loyaltypoints.service;

import com.example.loyaltypoints.config.LoyaltyProperties;
import com.example.loyaltypoints.dto.PurchasePreviewResponse;
import com.example.loyaltypoints.dto.PurchaseRequest;
import com.example.loyaltypoints.dto.PurchaseResponse;
import com.example.loyaltypoints.dto.PurchaseUpdateRequest;
import com.example.loyaltypoints.entity.Customer;
import com.example.loyaltypoints.entity.LedgerType;
import com.example.loyaltypoints.entity.PointsLedger;
import com.example.loyaltypoints.entity.Purchase;
import com.example.loyaltypoints.entity.PurchaseStatus;
import com.example.loyaltypoints.entity.Tier;
import com.example.loyaltypoints.exception.BusinessRuleException;
import com.example.loyaltypoints.exception.InsufficientPointsException;
import com.example.loyaltypoints.exception.ResourceNotFoundException;
import com.example.loyaltypoints.repository.CustomerRepository;
import com.example.loyaltypoints.repository.PointsLedgerRepository;
import com.example.loyaltypoints.repository.PurchaseRepository;
import com.example.loyaltypoints.repository.TierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Core loyalty engine. All business rules are enforced here (service layer) BEFORE anything is saved:
 *  - tier discount is applied automatically using the customer's current tier
 *  - redeemed points can never exceed the customer's available balance
 *  - points are earned on the amount actually paid
 *  - tier upgrades happen the moment lifetime points cross the next threshold; tiers never downgrade
 */
@Service
public class PurchaseService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PurchaseRepository purchaseRepository;
    private final CustomerRepository customerRepository;
    private final TierRepository tierRepository;
    private final PointsLedgerRepository ledgerRepository;
    private final LoyaltyProperties props;

    public PurchaseService(PurchaseRepository purchaseRepository, CustomerRepository customerRepository,
                           TierRepository tierRepository, PointsLedgerRepository ledgerRepository,
                           LoyaltyProperties props) {
        this.purchaseRepository = purchaseRepository;
        this.customerRepository = customerRepository;
        this.tierRepository = tierRepository;
        this.ledgerRepository = ledgerRepository;
        this.props = props;
    }

    private record Calculation(BigDecimal amount, Tier tier, BigDecimal discountAmount,
                               BigDecimal afterDiscount, long redeem, BigDecimal redemptionValue,
                               BigDecimal finalAmount, long earned) {
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public List<PurchaseResponse> findAll(Long customerId) {
        List<Purchase> list = customerId == null
                ? purchaseRepository.findAllWithCustomer()
                : purchaseRepository.findByCustomerWithCustomer(customerId);
        return list.stream().map(p -> toResponse(p, null)).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse findById(Long id) {
        return toResponse(getEntity(id), null);
    }

    /** Shows what a purchase would cost / earn without saving anything. */
    @Transactional(readOnly = true)
    public PurchasePreviewResponse preview(PurchaseRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + req.customerId()));
        long redeem = req.pointsToRedeem() == null ? 0L : req.pointsToRedeem();
        Calculation calc = calculate(customer, req.amount(), redeem);
        return new PurchasePreviewResponse(calc.amount(), calc.tier().getName(), calc.tier().getDiscountPercent(),
                calc.discountAmount(), calc.afterDiscount(), calc.redeem(), calc.redemptionValue(),
                calc.finalAmount(), calc.earned(), customer.getPointsBalance());
    }

    // ----------------------------------------------------------------- create

    @Transactional
    public PurchaseResponse create(PurchaseRequest req) {
        // Row lock: concurrent purchases for the same customer are processed one at a time.
        Customer customer = customerRepository.findByIdForUpdate(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + req.customerId()));
        long redeem = req.pointsToRedeem() == null ? 0L : req.pointsToRedeem();

        // 1) validate + calculate everything first (throws before anything is saved)
        Calculation calc = calculate(customer, req.amount(), redeem);
        long balanceAfterRedeem = customer.getPointsBalance() - calc.redeem();
        long newBalance = balanceAfterRedeem + calc.earned();
        long newLifetime = customer.getLifetimePoints() + calc.earned();

        // 2) decide tier upgrade before saving (upgrade only, never downgrade)
        Tier currentTier = customer.getTier();
        Tier upgradedTier = tierRepository.findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(newLifetime)
                .filter(t -> t.getMinPoints() > currentTier.getMinPoints())
                .orElse(null);

        // 3) save
        Purchase purchase = new Purchase();
        purchase.setCustomer(customer);
        purchase.setAmount(calc.amount());
        purchase.setTierAtPurchase(calc.tier().getName());
        purchase.setDiscountPercent(calc.tier().getDiscountPercent());
        purchase.setDiscountAmount(calc.discountAmount());
        purchase.setPointsRedeemed(calc.redeem());
        purchase.setRedemptionValue(calc.redemptionValue());
        purchase.setFinalAmount(calc.finalAmount());
        purchase.setPointsEarned(calc.earned());
        purchase.setDescription(cleanText(req.description()));
        purchase.setStatus(PurchaseStatus.COMPLETED);
        purchase = purchaseRepository.save(purchase);

        if (calc.redeem() > 0) {
            ledgerRepository.save(entry(customer, purchase, LedgerType.REDEEM, -calc.redeem(), balanceAfterRedeem,
                    "Redeemed on purchase #" + purchase.getId()));
        }
        if (calc.earned() > 0) {
            ledgerRepository.save(entry(customer, purchase, LedgerType.EARN, calc.earned(), newBalance,
                    "Earned on purchase #" + purchase.getId()));
        }

        customer.setPointsBalance(newBalance);
        customer.setLifetimePoints(newLifetime);
        if (upgradedTier != null) {
            customer.setTier(upgradedTier);
        }
        customerRepository.save(customer);

        return toResponse(purchase, upgradedTier == null ? null : upgradedTier.getName());
    }

    // ----------------------------------------------------------------- update

    /** Only the description can change; amounts and points are immutable so the ledger always stays correct. */
    @Transactional
    public PurchaseResponse updateDescription(Long id, PurchaseUpdateRequest req) {
        Purchase p = getEntity(id);
        p.setDescription(cleanText(req.description()));
        return toResponse(purchaseRepository.save(p), null);
    }

    // ----------------------------------------------------------------- delete

    /**
     * Deleting a purchase = cancelling it. The record is kept (status CANCELLED) and its points effect
     * is reversed with ledger entries. The customer's tier is never downgraded.
     */
    @Transactional
    public PurchaseResponse cancel(Long id) {
        Purchase p = getEntity(id);
        if (p.getStatus() == PurchaseStatus.CANCELLED) {
            throw new BusinessRuleException("Purchase #" + id + " is already cancelled");
        }
        Customer customer = customerRepository.findByIdForUpdate(p.getCustomer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        long refunded = p.getPointsRedeemed();
        long removed = p.getPointsEarned();
        long newBalance = customer.getPointsBalance() + refunded - removed;
        if (newBalance < 0) {
            throw new BusinessRuleException("Cannot cancel purchase #" + id + ": the customer has already spent "
                    + "points earned from it (balance would become " + newBalance + ")");
        }

        long running = customer.getPointsBalance();
        if (refunded > 0) {
            running += refunded;
            ledgerRepository.save(entry(customer, p, LedgerType.REVERSAL, refunded, running,
                    "Redeemed points refunded - purchase #" + id + " cancelled"));
        }
        if (removed > 0) {
            running -= removed;
            ledgerRepository.save(entry(customer, p, LedgerType.REVERSAL, -removed, running,
                    "Earned points reversed - purchase #" + id + " cancelled"));
        }
        customer.setPointsBalance(newBalance);
        customer.setLifetimePoints(Math.max(0L, customer.getLifetimePoints() - removed));
        customerRepository.save(customer);

        p.setStatus(PurchaseStatus.CANCELLED);
        return toResponse(purchaseRepository.save(p), null);
    }

    // ---------------------------------------------------------------- helpers

    private Calculation calculate(Customer customer, BigDecimal rawAmount, long redeem) {
        if (redeem < 0) {
            throw new BusinessRuleException("Points to redeem cannot be negative");
        }
        BigDecimal amount = rawAmount.setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new BusinessRuleException("Purchase amount must be greater than zero");
        }
        Tier tier = customer.getTier();

        BigDecimal discountAmount = amount.multiply(tier.getDiscountPercent())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal afterDiscount = amount.subtract(discountAmount);

        if (redeem > customer.getPointsBalance()) {
            throw new InsufficientPointsException("Cannot redeem " + redeem + " points: available balance is "
                    + customer.getPointsBalance() + " points");
        }
        BigDecimal redemptionValue = props.getRedemptionValuePerPoint()
                .multiply(BigDecimal.valueOf(redeem)).setScale(2, RoundingMode.HALF_UP);
        if (redemptionValue.compareTo(afterDiscount) > 0) {
            throw new BusinessRuleException("Redemption value (Rs." + redemptionValue.toPlainString()
                    + ") cannot exceed the payable amount after discount (Rs." + afterDiscount.toPlainString() + ")");
        }
        BigDecimal finalAmount = afterDiscount.subtract(redemptionValue);
        long earned = finalAmount.multiply(props.getPointsPerRupee()).setScale(0, RoundingMode.DOWN).longValue();
        return new Calculation(amount, tier, discountAmount, afterDiscount, redeem, redemptionValue,
                finalAmount, earned);
    }

    private PointsLedger entry(Customer customer, Purchase purchase, LedgerType type, long change,
                               long balanceAfter, String description) {
        PointsLedger l = new PointsLedger();
        l.setCustomer(customer);
        l.setPurchase(purchase);
        l.setEntryType(type);
        l.setPointsChange(change);
        l.setBalanceAfter(balanceAfter);
        l.setDescription(description);
        return l;
    }

    private Purchase getEntity(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id " + id));
    }

    private String cleanText(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private PurchaseResponse toResponse(Purchase p, String upgradedTo) {
        return new PurchaseResponse(p.getId(), p.getCustomer().getId(), p.getCustomer().getName(), p.getAmount(),
                p.getTierAtPurchase(), p.getDiscountPercent(), p.getDiscountAmount(), p.getPointsRedeemed(),
                p.getRedemptionValue(), p.getFinalAmount(), p.getPointsEarned(), p.getDescription(),
                p.getStatus(), p.getPurchaseDate(), upgradedTo);
    }
}
