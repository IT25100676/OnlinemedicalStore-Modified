package com.medstore.order.model.payment;

import com.medstore.order.model.PaymentMethod;

/**
 * Online bank transfer payment.
 * Demonstrates INHERITANCE and POLYMORPHISM.
 */
public class OnlineTransferPayment extends Payment {

    private String bankName;
    private String referenceNumber;

    public OnlineTransferPayment() {}

    public OnlineTransferPayment(double amount, String bankName, String referenceNumber) {
        this.amount          = amount;
        this.bankName        = bankName;
        this.referenceNumber = referenceNumber;
    }

    @Override
    public boolean processPayment() {
        this.transactionId = generateTransactionId("TRF");
        this.processed     = true;
        return true;
    }

    @Override
    public String getPaymentSummary() {
        return "Bank Transfer — Rs. " + String.format("%.2f", amount)
               + " via " + bankName + " (Ref: " + referenceNumber + ")";
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.ONLINE_TRANSFER;
    }

    public String getBankName()        { return bankName; }
    public String getReferenceNumber() { return referenceNumber; }

    public void setBankName(String bankName)               { this.bankName = bankName; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
}
