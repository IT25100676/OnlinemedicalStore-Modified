package com.medstore.order.model;

/**
 * Represents all possible lifecycle states of an order.
 */
public enum OrderStatus {
    PENDING("Pending", "⏳"),
    CONFIRMED("Confirmed", "✅"),
    PROCESSING("Processing", "🔄"),
    SHIPPED("Shipped", "🚚"),
    DELIVERED("Delivered", "📦"),
    CANCELLED("Cancelled", "❌");

    private final String label;
    private final String icon;

    OrderStatus(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon()  { return icon; }
}
