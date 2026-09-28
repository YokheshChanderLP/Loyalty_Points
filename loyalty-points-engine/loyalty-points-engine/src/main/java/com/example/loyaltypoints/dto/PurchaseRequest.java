package com.example.loyaltypoints.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PurchaseRequest(
        @NotNull(message = "Customer is required") Long customerId,
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Amount can have at most 10 digits and 2 decimals") BigDecimal amount,
        @PositiveOrZero(message = "Points to redeem cannot be negative") Long pointsToRedeem,
        @Size(max = 255, message = "Description must be at most 255 characters") String description) {
}
