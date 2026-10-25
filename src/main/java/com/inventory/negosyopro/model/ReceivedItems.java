package com.inventory.negosyopro.model;

import java.time.LocalDate;

// One line of the Receive screen: which purchase order item arrived, how many, and its expiry date.
public class ReceivedItems {
    private final long purchaseOrderItemId;
    private final int quantityReceived;
    private final LocalDate expirationDate; // null when the product has no expiry

    public ReceivedItems(long purchaseOrderItemId, int quantityReceived, LocalDate expirationDate) {
        this.purchaseOrderItemId = purchaseOrderItemId;
        this.quantityReceived = quantityReceived;
        this.expirationDate = expirationDate;
    }

    public long getPurchaseOrderItemId() {
        return purchaseOrderItemId;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }
}
