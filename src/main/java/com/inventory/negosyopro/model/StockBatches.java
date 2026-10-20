package com.inventory.negosyopro.model;

import java.math.BigDecimal;
import java.time.*;

public class StockBatches {
    private long batchId;
    private long businessId;
    private long productId;
    private Long supplierId;
    private Long purchaseOrderItemId;
    private String batchNumber;
    private int quantityReceived;
    private int quantityRemaining;
    private BigDecimal unitCost;
    private LocalDate expirationDate;
    private LocalDateTime receivedAt;

    public StockBatches(long batchId, long businessId, long productId, Long supplierId, Long purchaseOrderItemId,
            String batchNumber,
            int quantityReceived, int quantityRemaining, BigDecimal unitCost, LocalDate expirationDate,
            LocalDateTime receivedAt) {
        this.batchId = batchId;
        this.businessId = businessId;
        this.productId = productId;
        this.supplierId = supplierId;
        this.purchaseOrderItemId = purchaseOrderItemId;
        this.batchNumber = batchNumber;
        this.quantityReceived = quantityReceived;
        this.quantityRemaining = quantityRemaining;
        this.unitCost = unitCost;
        this.expirationDate = expirationDate;
        this.receivedAt = receivedAt;
    }

    // getters
    public long getBatchId() {
        return batchId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public long getProductId() {
        return productId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public Long getPurchaseOrderItemId() {
        return purchaseOrderItemId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public int getQuantityRemaining() {
        return quantityRemaining;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

}
