package com.example.loyaltypoints.dto;

import java.math.BigDecimal;

public record PurchasePreviewResponse(
        BigDecimal amount,
        String tierName,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        BigDecimal amountAfterDiscount,
        long pointsToRedeem,
        BigDecimal redemptionValue,
        BigDecimal finalAmount,
        long pointsToEarn,
        long availableBalance) {
}
