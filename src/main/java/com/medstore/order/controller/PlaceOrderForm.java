package com.medstore.order.controller;

import com.medstore.order.model.PaymentMethod;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for the Place Order HTML form.
 * Keeps the controller thin and the model clean.
 */
public class PlaceOrderForm {

    private String customerId;
    private String customerName;
    private String deliveryAddress;

    // Comma-separated: "medicineId|medicineName|qty|price,..."
    private String itemsRaw;

    private PaymentMethod paymentMethod;

    // COD field
    private String codAddress;

    // Card fields
    private String cardHolder;
    private String cardNumber;

    // Transfer fields
    private String bankName;
    private String referenceNumber;

    /**
     * Manual validation method.
     * No validation dependency needed.
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (isEmpty(customerId)) {
            errors.add("Customer ID is required");
        }

        if (isEmpty(customerName)) {
            errors.add("Customer name is required");
        }

        if (isEmpty(deliveryAddress)) {
            errors.add("Delivery address is required");
        }

        if (paymentMethod == null) {
            errors.add("Please select a payment method");
        }

        return errors;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getItemsRaw() {
        return itemsRaw;
    }

    public void setItemsRaw(String itemsRaw) {
        this.itemsRaw = itemsRaw;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCodAddress() {
        return codAddress;
    }

    public void setCodAddress(String codAddress) {
        this.codAddress = codAddress;
    }

    public String getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(String cardHolder) {
        this.cardHolder = cardHolder;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
}