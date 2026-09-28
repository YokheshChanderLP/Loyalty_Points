package com.example.loyaltypoints.exception;

public class InsufficientPointsException extends BusinessRuleException {
    public InsufficientPointsException(String message) {
        super(message);
    }
}
