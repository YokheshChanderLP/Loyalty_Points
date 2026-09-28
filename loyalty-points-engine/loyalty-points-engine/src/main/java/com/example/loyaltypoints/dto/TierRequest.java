package com.example.loyaltypoints.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TierRequest(
        @NotBlank(message = "Tier name is required") @Size(max = 30, message = "Tier name must be at most 30 characters") String name,
        @NotNull(message = "Minimum points is required") @PositiveOrZero(message = "Minimum points cannot be negative") Long minPoints,
        @NotNull(message = "Discount percent is required")
        @DecimalMin(value = "0.00", message = "Discount percent cannot be negative")
        @DecimalMax(value = "100.00", message = "Discount percent cannot exceed 100") BigDecimal discountPercent) {
}
