package com.medstore.order.model;

import com.medstore.order.model.payment.Payment;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Central domain class representing a customer order.
 *
 * OOP concepts demonstrated:
 *  - ENCAPSULATION  : all fields private; exposed via controlled getters/setters
 *  - ABSTRACTION    : interacts with Payment via abstract type (polymorphic)
 */
public class Order implements Serializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Identity ──────────────────────────────────────────────────────────────
    private String       orderId;
    private String       customerId;
    private String       customerName;

    // ── Content ───────────────────────────────────────────────────────────────
    private List<OrderItem> items = new ArrayList<>();

    // ── Delivery ──────────────────────────────────────────────────────────────
    private String deliveryAddress;

    // ── Payment (abstract reference — polymorphism) ───────────────────────────
    private Payment       payment;
    private PaymentMethod paymentMethod;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    private OrderStatus   status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Order() {
        this.status    = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Order(String orderId, String customerId, String customerName, String deliveryAddress) {
        this();
        this.orderId         = orderId;
        this.customerId      = customerId;
        this.customerName    = customerName;
        this.deliveryAddress = deliveryAddress;
    }

    // ── Business methods ──────────────────────────────────────────────────────

    /** Calculates the total value of all line items. */
    public double getTotalAmount() {
        return items.stream().mapToDouble(OrderItem::getLineTotal).sum();
    }

    /** Updates status and records the timestamp. */
    public void updateStatus(OrderStatus newStatus) {
        this.status    = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /** Attaches a payment object and processes it. Returns true on success. */
    public boolean applyPayment(Payment payment) {
        this.payment = payment;
        this.paymentMethod = payment.getPaymentMethod();
        boolean success = payment.processPayment();
        if (success) {
            this.status = OrderStatus.CONFIRMED;
        }
        this.updatedAt = LocalDateTime.now();
        return success;
    }

    /** Human-readable formatted creation date. */
    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.format(FMT) : "";
    }

    /** Human-readable formatted update date. */
    public String getFormattedUpdatedAt() {
        return updatedAt != null ? updatedAt.format(FMT) : "";
    }

    // ── File serialisation (orders.txt) ──────────────────────────────────────

    /**
     * Serialises the order to a multi-field string for flat-file storage.
     * Format (fields separated by "||"):
     *   orderId || customerId || customerName || deliveryAddress || paymentMethod
     *   || status || createdAt || updatedAt || item1~item2~...
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(orderId).append("||")
          .append(customerId).append("||")
          .append(customerName).append("||")
          .append(deliveryAddress).append("||")
          .append(paymentMethod != null ? paymentMethod.name() : "").append("||")
          .append(status.name()).append("||")
          .append(createdAt.format(FMT)).append("||")
          .append(updatedAt.format(FMT)).append("||");

        List<String> serializedItems = new ArrayList<>();
        for (OrderItem item : items) {
            serializedItems.add(item.serialize());
        }
        sb.append(String.join("~", serializedItems));
        return sb.toString();
    }

    /** Reconstructs an Order from a serialised line. */
    public static Order deserialize(String line) {
        String[] parts = line.split("\\|\\|", -1);
        if (parts.length < 9) throw new IllegalArgumentException("Malformed order record: " + line);

        Order o = new Order();
        o.orderId         = parts[0];
        o.customerId      = parts[1];
        o.customerName    = parts[2];
        o.deliveryAddress = parts[3];
        o.paymentMethod   = parts[4].isEmpty() ? null : PaymentMethod.valueOf(parts[4]);
        o.status          = OrderStatus.valueOf(parts[5]);
        o.createdAt       = LocalDateTime.parse(parts[6], FMT);
        o.updatedAt       = LocalDateTime.parse(parts[7], FMT);

        if (!parts[8].isBlank()) {
            for (String raw : parts[8].split("~")) {
                o.items.add(OrderItem.deserialize(raw));
            }
        }
        return o;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getOrderId()                { return orderId; }
    public void   setOrderId(String orderId)  { this.orderId = orderId; }

    public String getCustomerId()                   { return customerId; }
    public void   setCustomerId(String customerId)  { this.customerId = customerId; }

    public String getCustomerName()                     { return customerName; }
    public void   setCustomerName(String customerName)  { this.customerName = customerName; }

    public List<OrderItem> getItems()                 { return items; }
    public void            setItems(List<OrderItem> items) { this.items = items; }

    public String getDeliveryAddress()                      { return deliveryAddress; }
    public void   setDeliveryAddress(String deliveryAddress){ this.deliveryAddress = deliveryAddress; }

    public Payment       getPayment()                  { return payment; }
    public void          setPayment(Payment payment)   { this.payment = payment; }

    public PaymentMethod getPaymentMethod()                       { return paymentMethod; }
    public void          setPaymentMethod(PaymentMethod m)        { this.paymentMethod = m; }

    public OrderStatus getStatus()                      { return status; }
    public void        setStatus(OrderStatus status)    { this.status = status; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void          setCreatedAt(LocalDateTime t)      { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()                     { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime t)      { this.updatedAt = t; }
}
