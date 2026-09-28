package com.example.loyaltypoints.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** Gross purchase amount before any discount. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "tier_at_purchase", nullable = false, length = 30)
    private String tierAtPurchase;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "points_redeemed", nullable = false)
    private long pointsRedeemed;

    @Column(name = "redemption_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal redemptionValue;

    /** Amount the customer actually pays. */
    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "points_earned", nullable = false)
    private long pointsEarned;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseStatus status = PurchaseStatus.COMPLETED;

    @Column(name = "purchase_date", nullable = false, updatable = false)
    private LocalDateTime purchaseDate;

    @JsonIgnore
    @OneToMany(mappedBy = "purchase")
    private List<PointsLedger> ledgerEntries = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (purchaseDate == null) purchaseDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getTierAtPurchase() { return tierAtPurchase; }
    public void setTierAtPurchase(String tierAtPurchase) { this.tierAtPurchase = tierAtPurchase; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public long getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(long pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }
    public BigDecimal getRedemptionValue() { return redemptionValue; }
    public void setRedemptionValue(BigDecimal redemptionValue) { this.redemptionValue = redemptionValue; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public long getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(long pointsEarned) { this.pointsEarned = pointsEarned; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PurchaseStatus getStatus() { return status; }
    public void setStatus(PurchaseStatus status) { this.status = status; }
    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate) { this.purchaseDate = purchaseDate; }
    public List<PointsLedger> getLedgerEntries() { return ledgerEntries; }
    public void setLedgerEntries(List<PointsLedger> ledgerEntries) { this.ledgerEntries = ledgerEntries; }
}
