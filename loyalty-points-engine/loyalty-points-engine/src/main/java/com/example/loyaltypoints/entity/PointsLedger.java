package com.example.loyaltypoints.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_ledger")
public class PointsLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private LedgerType entryType;

    /** Positive for points added, negative for points removed. */
    @Column(name = "points_change", nullable = false)
    private long pointsChange;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Purchase getPurchase() { return purchase; }
    public void setPurchase(Purchase purchase) { this.purchase = purchase; }
    public LedgerType getEntryType() { return entryType; }
    public void setEntryType(LedgerType entryType) { this.entryType = entryType; }
    public long getPointsChange() { return pointsChange; }
    public void setPointsChange(long pointsChange) { this.pointsChange = pointsChange; }
    public long getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(long balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
