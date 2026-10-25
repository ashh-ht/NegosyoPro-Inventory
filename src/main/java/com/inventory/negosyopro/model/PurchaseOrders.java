package com.inventory.negosyopro.model;

import java.time.*;

public class PurchaseOrders {
    public enum Status {
        DRAFT,
        ORDERED,
        RECEIVED,
        CANCELLED
    }

    private long purchaseOrderId;
    private long businessId;
    private long supplierId;
    private long userId;
    private String poNumber;
    private Status status;
    private LocalDate orderedAt;
    private LocalDate expectedArrival;
    private LocalDateTime receivedAt;
    private String notes;
    private LocalDateTime createdAt;

    public PurchaseOrders(long purchaseOrderId, long businessId, long supplierId, long userId, String poNumber,
            Status status,
            LocalDate orderedAt, LocalDate expectedArrival, LocalDateTime receivedAt, String notes,
            LocalDateTime createdAt) {
        this.purchaseOrderId = purchaseOrderId;
        this.businessId = businessId;
        this.supplierId = supplierId;
        this.userId = userId;
        this.poNumber = poNumber;
        this.status = status;
        this.orderedAt = orderedAt;
        this.expectedArrival = expectedArrival;
        this.receivedAt = receivedAt;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    // getters
    public long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public long getSupplierId() {
        return supplierId;
    }

    public long getUserId() {
        return userId;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDate getOrderedAt() {
        return orderedAt;
    }

    public LocalDate getExpectedArrival() {
        return expectedArrival;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
