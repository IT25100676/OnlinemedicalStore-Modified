package com.medstore.order.model.payment;

import java.io.Serializable;

/**
 * Abstract base for all payment types.
 * Demonstrates ABSTRACTION — the concrete processing logic is deferred to subclasses.
 * Demonstrates POLYMORPHISM — the controller uses Payment references; the actual
 * processPayment() implementation varies by subclass.
 */
public abstract class Payment implements Serializable {

    protected String transactionId;
    protected double amount;
    protected boolean processed;

    // ── Abstract methods (must be implemented by each payment type) ───────────

    /**
     * Executes the payment. Different implementations handle COD acknowledgment,
     * card charging, bank transfer verification, etc.
     *
     * @return true if the payment succeeded
     */
    public abstract boolean processPayment();

    /**
     * Returns a human-readable summary of the payment method used.
     */
    public abstract String getPaymentSummary();

    /**
     * Returns the enum constant identifying this payment type.
     */
    public abstract com.medstore.order.model.PaymentMethod getPaymentMethod();

    // ── Common behaviour ──────────────────────────────────────────────────────

    /** Generates a simple transaction reference. */
    protected String generateTransactionId(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getTransactionId() { return transactionId; }
    public double getAmount()        { return amount; }
    public boolean isProcessed()     { return processed; }

    public void setAmount(double amount)  { this.amount = amount; }
    public void setProcessed(boolean processed) { this.processed = processed; }
}
