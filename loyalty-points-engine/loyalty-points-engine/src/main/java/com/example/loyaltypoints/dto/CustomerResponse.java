package com.example.loyaltypoints.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        long lifetimePoints,
        long pointsBalance,
        Long tierId,
        String tierName,
        BigDecimal tierDiscountPercent,
        String nextTierName,
        Long pointsToNextTier,
        LocalDateTime createdAt) {
}
