package com.inventory.negosyopro.model;

// Result of PurchaseOrderService.getRestockSuggestions (one row). Not a table.
public class RestockSuggestions {
    private final Products product;
    private final int reorderPoint;           // restock when current stock reaches this
    private final int economicOrderQuantity;  // EOQ: how many to order
    private final Long preferredSupplierId;   // null when no preferred supplier is set

    public RestockSuggestions(Products product, int reorderPoint, int economicOrderQuantity,
            Long preferredSupplierId) {
        this.product = product;
        this.reorderPoint = reorderPoint;
        this.economicOrderQuantity = economicOrderQuantity;
        this.preferredSupplierId = preferredSupplierId;
    }

    public Products getProduct() {
        return product;
    }

    public int getReorderPoint() {
        return reorderPoint;
    }

    public int getEconomicOrderQuantity() {
        return economicOrderQuantity;
    }

    public Long getPreferredSupplierId() {
        return preferredSupplierId;
    }
}
