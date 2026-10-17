package com.inventory.negosyopro.model;

import java.math.BigDecimal;

public class Discounts {
    private long discountId;
    private long businessId;
    private String discountName;
    private BigDecimal discountPercent;
    private boolean requiresId;
    private boolean isActive;

    public Discounts(long discountId, long businessId, String discountName, BigDecimal discountPercent,
            boolean requiresId, boolean isActive) {
        this.discountId = discountId;
        this.businessId = businessId;
        this.discountName = discountName;
        this.discountPercent = discountPercent;
        this.requiresId = requiresId;
        this.isActive = isActive;
    }
    
    public long getDiscountId() {
        return discountId;
    }
    
    public long getBusinessId() {
        return businessId;
    }

    public String getDiscountName() {
        return discountName;
    }
    
    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }
    
    public boolean isRequiresId() {
        return requiresId;
    }

    public boolean isActive() {
        return isActive;
    }
}
