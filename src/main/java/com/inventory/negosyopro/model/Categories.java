package com.inventory.negosyopro.model;

public class Categories {
    private long categoryId;
    private long businessId;
    private String categoryName;
    private boolean isActive;

    public Categories(long categoryId, long businessId, String categoryName, boolean isActive) {
        this.categoryId = categoryId;
        this.businessId = businessId;
        this.categoryName = categoryName;
        this.isActive = isActive;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public boolean isActive(){
        return isActive;
    }
}
