package com.inventory.negosyopro.model;

import java.math.BigDecimal;

public class Products {
    private long productId;
    private long businessId;
    private long categoryId;
    private String productCode;
    private String productName;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private String unit;
    private boolean isTaxable;
    private boolean trackInventory;
    private boolean tracksExpiry;
    private Integer shelfLifeDays;
    private int currentStock;
    private int reorderLevel;
    private int reorderQuantity;
    private boolean isActive;

    public Products(long productId, long businessId, long categoryId, String productCode, String productName,
            BigDecimal costPrice, BigDecimal sellingPrice, String unit, boolean isTaxable, boolean trackInventory,
            boolean tracksExpiry, Integer shelfLifeDays, int currentStock, int reorderLevel, int reorderQuantity,
            boolean isActive) {
        this.productId = productId;
        this.businessId = businessId;
        this.categoryId = categoryId;
        this.productCode = productCode;
        this.productName = productName;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.unit = unit;
        this.isTaxable = isTaxable;
        this.trackInventory = trackInventory;
        this.tracksExpiry = tracksExpiry;
        this.shelfLifeDays = shelfLifeDays;
        this.currentStock = currentStock;
        this.reorderLevel = reorderLevel;
        this.reorderQuantity = reorderQuantity;
        this.isActive = isActive;
    }

    // For a NEW product (before it has an id).
    public Products(long businessId, long categoryId, String productCode, String productName,
            BigDecimal costPrice, BigDecimal sellingPrice, String unit, boolean isTaxable,
            boolean trackInventory, boolean tracksExpiry, Integer shelfLifeDays,
            int reorderLevel, int reorderQuantity) {
        this(0, businessId, categoryId, productCode, productName, costPrice, sellingPrice, unit,
                isTaxable, trackInventory, tracksExpiry, shelfLifeDays, 0, reorderLevel, reorderQuantity, true);
    }

    public long getProductId() {
        return productId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isTaxable() {
        return isTaxable;
    }

    public boolean isTrackInventory() {
        return trackInventory;
    }

    public boolean isTracksExpiry() {
        return tracksExpiry;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }
    
    public int getReorderQuantity() {
        return reorderQuantity;
    }

    public boolean isActive() {
        return isActive;
    }
}
