package com.inventory.negosyopro.model;

import java.math.BigDecimal;

public class ProductSuppliers {
    private long productSupplierId;
    private long productId;
    private long supplierId;
    private String supplierProductCode;
    private BigDecimal supplierCost;
    private Integer leadTimeDays;
    private boolean isPreferred;

    public ProductSuppliers(long productSupplierId, long productId, long supplierId, String supplierProductCode,
            BigDecimal supplierCost,
            Integer leadTimeDays, boolean isPreffered) {
        this.productSupplierId = productSupplierId;
        this.productId = productId;
        this.supplierId = supplierId;
        this.supplierProductCode = supplierProductCode;
        this.supplierCost = supplierCost;
        this.leadTimeDays = leadTimeDays;
        this.isPreferred = isPreffered;
    }

    //getters

    public long getProductSupplierId() {
        return productSupplierId;
    }

    public long getProductId() {
        return productId;
    }

    public long getSupplierId() {
        return supplierId;
    }

    public String getSupplierProductCode() {
        return supplierProductCode;
    }

    public BigDecimal getSupplierCost() {
        return supplierCost;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public boolean isPreferred() {
        return isPreferred;
    }

}
