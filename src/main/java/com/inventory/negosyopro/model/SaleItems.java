package com.inventory.negosyopro.model;

import java.math.BigDecimal;

public class SaleItems {
    private long saleItemId;
    private long salesId;
    private long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitCost;
    private BigDecimal costPrice;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal subtotal;

    public SaleItems(long saleItemsId, long salesId, long productId, String productName, int quantity,
            BigDecimal unitCost, BigDecimal costPrice, BigDecimal discountAmount, BigDecimal taxAmount,
            BigDecimal subtotal) {
        this.saleItemId = saleItemsId;
        this.salesId = salesId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.costPrice = costPrice;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.subtotal = subtotal;
    }

    public long getSaleItemsId() {
        return saleItemId;
    }

    public long getSalesId() {
        return salesId;
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitCost;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getSubtotal(){
        return subtotal;
    }
}