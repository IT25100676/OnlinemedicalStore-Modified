package com.medstore.order.model.payment;

import com.medstore.order.model.PaymentMethod;

/**
 * Cash on Delivery payment — collected when the order arrives.
 * Demonstrates INHERITANCE (extends Payment) and POLYMORPHISM (overrides processPayment).
 */
public class CashOnDeliveryPayment extends Payment {

    private String deliveryAddress;

    public CashOnDeliveryPayment() {}

    public CashOnDeliveryPayment(double amount, String deliveryAddress) {
        this.amount          = amount;
        this.deliveryAddress = deliveryAddress;
    }

    @Override
    public boolean processPayment() {
        // COD: payment is scheduled — no immediate charge
        this.transactionId = generateTransactionId("COD");
        this.processed     = true; // pending physical collection
        return true;
    }

    @Override
    public String getPaymentSummary() {
        return "Cash on Delivery — pay Rs. " + String.format("%.2f", amount)
               + " upon arrival at: " + deliveryAddress;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CASH_ON_DELIVERY;
    }

    public String getDeliveryAddress()               { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
}
