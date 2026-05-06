package com.medstore.order.model;

import java.io.Serializable;

/**
 * Encapsulates a single medicine line item within an order.
 * Demonstrates ENCAPSULATION — all fields private, accessed via getters/setters.
 */
public class OrderItem implements Serializable {

    private String medicineId;
    private String medicineName;
    private int    quantity;
    private double unitPrice;

    // ── Constructors ──────────────────────────────────────────────────────────

    public OrderItem() {}

    public OrderItem(String medicineId, String medicineName, int quantity, double unitPrice) {
        this.medicineId   = medicineId;
        this.medicineName = medicineName;
        this.quantity     = quantity;
        this.unitPrice    = unitPrice;
    }

    // ── Business method ───────────────────────────────────────────────────────

    /** Returns the total price for this line (unitPrice × quantity). */
    public double getLineTotal() {
        return unitPrice * quantity;
    }

    // ── Serialization helper (used by file-based repository) ──────────────────

    /** Serialises to pipe-delimited string for flat-file storage. */
    public String serialize() {
        return medicineId + "|" + medicineName + "|" + quantity + "|" + unitPrice;
    }

    /** Deserialises from pipe-delimited string. */
    public static OrderItem deserialize(String raw) {
        String[] p = raw.split("\\|");
        return new OrderItem(p[0], p[1], Integer.parseInt(p[2]), Double.parseDouble(p[3]));
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getMedicineId()   { return medicineId; }
    public void setMedicineId(String medicineId)   { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public int getQuantity()        { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice()    { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
