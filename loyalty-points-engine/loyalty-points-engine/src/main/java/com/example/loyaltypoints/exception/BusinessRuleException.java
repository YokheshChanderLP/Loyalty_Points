package com.example.loyaltypoints.exception;

/** Thrown when a request is well-formed but violates a loyalty business rule. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
