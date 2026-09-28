package com.example.loyaltypoints.dto;

import jakarta.validation.constraints.Size;

public record PurchaseUpdateRequest(
        @Size(max = 255, message = "Description must be at most 255 characters") String description) {
}
