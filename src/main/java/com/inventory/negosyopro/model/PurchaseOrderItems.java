package com.inventory.negosyopro.model;

import java.math.BigDecimal;

public class PurchaseOrderItems {
    private long purchaseOrderItemId;
    private long purchaseOrderId;
    private long productId;
    private int quantityOrdered;
    private int quantityReceived;
    private BigDecimal unitCost;

    public PurchaseOrderItems(long purchaseOrderItemId, long purchaseOrderId, long productId, int quantityOrdered,
            int quantityReceived, BigDecimal unitCost) {
        this.purchaseOrderItemId = purchaseOrderItemId;
        this.purchaseOrderId = purchaseOrderId;
        this.productId = productId;
        this.quantityOrdered = quantityOrdered;
        this.quantityReceived = quantityReceived;
        this.unitCost = unitCost;
    }
    
    //getters

    public long getPurchaseOrderItemId() {
        return purchaseOrderItemId;
    }

    public long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantityOrdered() {
        return quantityOrdered;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }
}
