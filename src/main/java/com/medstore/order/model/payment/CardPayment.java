package com.medstore.order.model.payment;

import com.medstore.order.model.PaymentMethod;

/**
 * Credit / Debit card payment.
 * Demonstrates INHERITANCE and POLYMORPHISM.
 */
public class CardPayment extends Payment {

    private String cardHolderName;
    private String maskedCardNumber; // stores only last 4 digits — security

    public CardPayment() {}

    public CardPayment(double amount, String cardHolderName, String cardNumber) {
        this.amount          = amount;
        this.cardHolderName  = cardHolderName;
        // Store only last 4 digits
        this.maskedCardNumber = "**** **** **** " + cardNumber.substring(Math.max(0, cardNumber.length() - 4));
    }

    @Override
    public boolean processPayment() {
        // In production this would call a payment gateway SDK
        this.transactionId = generateTransactionId("CRD");
        this.processed     = true;
        return true;
    }

    @Override
    public String getPaymentSummary() {
        return "Card Payment — Rs. " + String.format("%.2f", amount)
               + " charged to card " + maskedCardNumber + " (" + cardHolderName + ")";
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CREDIT_CARD;
    }

    public String getCardHolderName()   { return cardHolderName; }
    public String getMaskedCardNumber() { return maskedCardNumber; }

    public void setCardHolderName(String cardHolderName)     { this.cardHolderName = cardHolderName; }
    public void setMaskedCardNumber(String maskedCardNumber) { this.maskedCardNumber = maskedCardNumber; }
}
