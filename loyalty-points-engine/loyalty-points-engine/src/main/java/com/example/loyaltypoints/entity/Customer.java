package com.example.loyaltypoints.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    /** Total points ever earned. Only used for tier qualification; never reduced by redemption. */
    @Column(name = "lifetime_points", nullable = false)
    private long lifetimePoints = 0;

    /** Points currently available to redeem. */
    @Column(name = "points_balance", nullable = false)
    private long pointsBalance = 0;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tier_id", nullable = false)
    private Tier tier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    private List<Purchase> purchases = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    private List<PointsLedger> ledgerEntries = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public long getLifetimePoints() { return lifetimePoints; }
    public void setLifetimePoints(long lifetimePoints) { this.lifetimePoints = lifetimePoints; }
    public long getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(long pointsBalance) { this.pointsBalance = pointsBalance; }
    public Tier getTier() { return tier; }
    public void setTier(Tier tier) { this.tier = tier; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<Purchase> getPurchases() { return purchases; }
    public void setPurchases(List<Purchase> purchases) { this.purchases = purchases; }
    public List<PointsLedger> getLedgerEntries() { return ledgerEntries; }
    public void setLedgerEntries(List<PointsLedger> ledgerEntries) { this.ledgerEntries = ledgerEntries; }
}
