package com.example.loyaltypoints.service;

import com.example.loyaltypoints.dto.CustomerRequest;
import com.example.loyaltypoints.dto.CustomerResponse;
import com.example.loyaltypoints.dto.LedgerResponse;
import com.example.loyaltypoints.entity.Customer;
import com.example.loyaltypoints.entity.PointsLedger;
import com.example.loyaltypoints.entity.Tier;
import com.example.loyaltypoints.exception.BusinessRuleException;
import com.example.loyaltypoints.exception.DuplicateResourceException;
import com.example.loyaltypoints.exception.ResourceNotFoundException;
import com.example.loyaltypoints.repository.CustomerRepository;
import com.example.loyaltypoints.repository.PointsLedgerRepository;
import com.example.loyaltypoints.repository.PurchaseRepository;
import com.example.loyaltypoints.repository.TierRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TierRepository tierRepository;
    private final PurchaseRepository purchaseRepository;
    private final PointsLedgerRepository ledgerRepository;

    public CustomerService(CustomerRepository customerRepository, TierRepository tierRepository,
                           PurchaseRepository purchaseRepository, PointsLedgerRepository ledgerRepository) {
        this.customerRepository = customerRepository;
        this.tierRepository = tierRepository;
        this.purchaseRepository = purchaseRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        String email = normalizeEmail(req.email());
        if (customerRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new DuplicateResourceException("A customer with email '" + email + "' already exists");
        }
        Tier startTier = tierRepository.findFirstByOrderByMinPointsAsc()
                .orElseThrow(() -> new BusinessRuleException(
                        "No tiers are configured. Create a tier before adding customers"));
        Customer c = new Customer();
        c.setName(req.name().trim());
        c.setEmail(email);
        c.setPhone(cleanPhone(req.phone()));
        c.setTier(startTier);
        c.setLifetimePoints(0);
        c.setPointsBalance(0);
        return toResponse(customerRepository.save(c));
    }

    /** Updates contact details only; points and tier are changed exclusively by purchases. */
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req) {
        Customer c = getEntity(id);
        String email = normalizeEmail(req.email());
        customerRepository.findByEmailIgnoreCase(email).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new DuplicateResourceException("A customer with email '" + email + "' already exists");
            }
        });
        c.setName(req.name().trim());
        c.setEmail(email);
        c.setPhone(cleanPhone(req.phone()));
        return toResponse(customerRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        getEntity(id);
        ledgerRepository.removeByCustomerId(id);
        purchaseRepository.removeByCustomerId(id);
        customerRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<LedgerResponse> getPointsHistory(Long id) {
        getEntity(id);
        return ledgerRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(id).stream()
                .map(this::toLedgerResponse).toList();
    }

    private Customer getEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String cleanPhone(String phone) {
        return (phone == null || phone.isBlank()) ? null : phone.trim();
    }

    private CustomerResponse toResponse(Customer c) {
        Tier tier = c.getTier();
        long reference = Math.max(c.getLifetimePoints(), tier.getMinPoints());
        Optional<Tier> next = tierRepository.findFirstByMinPointsGreaterThanOrderByMinPointsAsc(reference);
        String nextName = next.map(Tier::getName).orElse(null);
        Long toNext = next.map(t -> t.getMinPoints() - c.getLifetimePoints()).orElse(null);
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone(),
                c.getLifetimePoints(), c.getPointsBalance(), tier.getId(), tier.getName(),
                tier.getDiscountPercent(), nextName, toNext, c.getCreatedAt());
    }

    private LedgerResponse toLedgerResponse(PointsLedger l) {
        Long purchaseId = l.getPurchase() == null ? null : l.getPurchase().getId();
        return new LedgerResponse(l.getId(), purchaseId, l.getEntryType(), l.getPointsChange(),
                l.getBalanceAfter(), l.getDescription(), l.getCreatedAt());
    }
}
