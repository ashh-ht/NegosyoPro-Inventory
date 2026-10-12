package com.inventory.negosyopro.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Business {
    private long businessId;
    private long templateId;
    private String businessName;
    private String businessAddress;
    private String businessContact;
    private boolean isTaxEnabled;
    private boolean isTaxInclusive;
    private BigDecimal taxRate;
    private String currencyCode;
    private String currencySymbol;
    private String receiptFooter;
    private boolean isUsesExpiry;
    private boolean isUsesPurchaseOrders;
    private int expiryAlertDays;
    private BigDecimal orderingCost;
    private BigDecimal holdingCost;
    private LocalDateTime createdAt;

    public Business(long businessId, long templateId, String businessName, String businessAddress,
            String businessContact, boolean isTaxEnabled, boolean isTaxInclusive, BigDecimal taxRate,
            String currencyCode, String currencySymbol, String receiptFooter, boolean isUsesExpiry,
            boolean isUsesPurchaseOrders, int expiryAlertDays, BigDecimal orderingCost,
            BigDecimal holdingCost, LocalDateTime createdAt) {
        this.businessId = businessId;
        this.templateId = templateId;
        this.businessName = businessName;
        this.businessAddress = businessAddress;
        this.businessContact = businessContact;
        this.isTaxEnabled = isTaxEnabled;
        this.isTaxInclusive = isTaxInclusive;
        this.taxRate = taxRate;
        this.currencyCode = currencyCode;
        this.currencySymbol = currencySymbol;
        this.receiptFooter = receiptFooter;
        this.isUsesExpiry = isUsesExpiry;
        this.isUsesPurchaseOrders = isUsesPurchaseOrders;
        this.expiryAlertDays = expiryAlertDays;
        this.orderingCost = orderingCost;
        this.holdingCost = holdingCost;
        this.createdAt = createdAt;
    }

    // For a new business
    public Business(long templateId, String businessName, String businessAddress,
            String businessContact, boolean isTaxEnabled, boolean isTaxInclusive,
            BigDecimal taxRate, String currencyCode, String currencySymbol,
            String receiptFooter, boolean isUsesExpiry, boolean isUsesPurchaseOrders,
            int expiryAlertDays, BigDecimal orderingCost, BigDecimal holdingCost) {
        this(0, templateId, businessName, businessAddress, businessContact,
                isTaxEnabled, isTaxInclusive, taxRate, currencyCode, currencySymbol,
                receiptFooter, isUsesExpiry, isUsesPurchaseOrders, expiryAlertDays,
                orderingCost, holdingCost, null);
    }
    //getters
    public long getBusinessId() {
        return businessId;
    }

    public long getTemplateId() {
        return templateId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public String getBusinessContact() {
        return businessContact;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public boolean isTaxEnabled() {
        return isTaxEnabled;
    }

    public boolean isTaxInclusive() {
        return isTaxInclusive;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public String getReceiptFooter() {
        return receiptFooter;
    }

    public boolean isUsesExpiry() {
        return isUsesExpiry;
    }

    public boolean isUsesPurchaseOrders() {
        return isUsesPurchaseOrders;
    }
    
    public int getExpiryAlertDays() {
        return expiryAlertDays;
    }
    
    public BigDecimal getOrderingCost() {
        return orderingCost;
    }
    
    public BigDecimal getHoldingCost() {
        return holdingCost;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    //setter
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public void setBusinessContact(String businessContact) {
        this.businessContact = businessContact;
    }

    public void setTaxEnabled(boolean isTaxEnabled) {
        this.isTaxEnabled = isTaxEnabled;
    }

    public void setTaxInclusive(boolean isTaxInclusive) {
        this.isTaxInclusive = isTaxInclusive;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public void setReceiptFooter(String receiptFooter) {
        this.receiptFooter = receiptFooter;
    }

    public void setUsesExpiry(boolean isUsesExpiry) {
        this.isUsesExpiry = isUsesExpiry;
    }

    public void setUsesPurchaseOrders(boolean v) {
        this.isUsesPurchaseOrders = v;
    }

    public void setExpiryAlertDays(int expiryAlertDays) {
        this.expiryAlertDays = expiryAlertDays;
    }

    public void setOrderingCost(BigDecimal orderingCost) {
        this.orderingCost = orderingCost;
    }

    public void setHoldingCost(BigDecimal holdingCost) {
        this.holdingCost = holdingCost;
    }
}
