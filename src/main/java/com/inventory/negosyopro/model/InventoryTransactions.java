package com.inventory.negosyopro.model;

import java.time.*;

public class InventoryTransactions {
    public enum TransactionType {
        SALES,
        RESTOCK,
        RETURN,
        DAMAGE,
        EXPIRED,
        VOID,
        ADJUSTMENT
    }

    private long transactionId;
    private long productId;
    private Long userId;
    private Long batchId;
    private TransactionType transactionType;
    private LocalDateTime transactionDate;
    private int quantity;
    private int previousStock;
    private int newStock;
    private Long referenceId;
    private String reason;

    public InventoryTransactions(long transactionId, long productId, Long userId, Long batchId,
            TransactionType transactionType,
            LocalDateTime transactionDate, int quantity, int previousStock, int newStock, Long referenceId,
            String reason) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.userId = userId;
        this.batchId = batchId;
        this.transactionType = transactionType;
        this.transactionDate = transactionDate;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.referenceId = referenceId;
        this.reason = reason;
    }

    //getters
    public long getTransactionId() {
        return transactionId;
    }

    public long getProductId() {
        return productId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPreviousStock() {
        return previousStock;
    }
    
    public int getNewStock() {
        return newStock;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public String getReason() {
        return reason;
    }
}
