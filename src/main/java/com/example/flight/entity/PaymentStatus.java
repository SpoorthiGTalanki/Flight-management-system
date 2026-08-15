package com.example.flight.entity;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUND_PENDING,
    REFUNDED,
    // Preserved for backward compatibility
    PAID,
    NOT_PAID
}