package com.inventory.negosyopro.model;

import java.math.BigDecimal;

// Read-only result of ReportsDAO.getTopSellingProducts (one row). Not a table.
public class ProductSalesSummaries {
    private final String productName;
    private final int quantity;
    private final BigDecimal revenue;

    public ProductSalesSummaries(String productName, int quantity, BigDecimal revenue) {
        this.productName = productName;
        this.quantity = quantity;
        this.revenue = revenue;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}