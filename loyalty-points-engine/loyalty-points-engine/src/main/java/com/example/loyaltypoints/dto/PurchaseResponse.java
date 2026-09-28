package com.example.loyaltypoints.dto;

import com.example.loyaltypoints.entity.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseResponse(
        Long id,
        Long customerId,
        String customerName,
        BigDecimal amount,
        String tierAtPurchase,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        long pointsRedeemed,
        BigDecimal redemptionValue,
        BigDecimal finalAmount,
        long pointsEarned,
        String description,
        PurchaseStatus status,
        LocalDateTime purchaseDate,
        String upgradedToTier) {
}
