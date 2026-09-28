package com.example.loyaltypoints.dto;

import com.example.loyaltypoints.entity.LedgerType;

import java.time.LocalDateTime;

public record LedgerResponse(
        Long id,
        Long purchaseId,
        LedgerType entryType,
        long pointsChange,
        long balanceAfter,
        String description,
        LocalDateTime createdAt) {
}
