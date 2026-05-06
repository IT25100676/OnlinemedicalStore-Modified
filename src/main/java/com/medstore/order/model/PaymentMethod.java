package com.medstore.order.model;

/**
 * Supported payment methods — drives polymorphic payment processing.
 */
public enum PaymentMethod {
    CASH_ON_DELIVERY("Cash on Delivery"),
    CREDIT_CARD("Credit / Debit Card"),
    ONLINE_TRANSFER("Online Bank Transfer");

    private final String label;

    PaymentMethod(String label) { this.label = label; }

    public String getLabel() { return label; }
}
