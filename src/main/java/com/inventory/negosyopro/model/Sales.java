package com.inventory.negosyopro.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Sales {
    public enum Status {
        COMPLETED,
        CANCELLED,
        REFUNDED
    }

    private long salesId;
    private long businessId;
    private long userId;
    private String orderNumber;
    private String customerName;
    private String customerContact;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal changeAmount;
    private long paymentMethodId;
    private Long discountId;
    private Status saleStatus;
    private String cancelReason;
    private LocalDateTime createdAt;

    public Sales(long salesId, long businessId, long userId, String orderNumber, String customerName,
            String customerContact,
            BigDecimal subtotal, BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal totalAmount,
            BigDecimal amountPaid,
            BigDecimal changeAmount, long paymentMethodId, Long discountId, Status saleStatus,
            String cancelReason, LocalDateTime createdAt) {
        this.salesId = salesId;
        this.businessId = businessId;
        this.userId = userId;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.customerContact = customerContact;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.amountPaid = amountPaid;
        this.changeAmount = changeAmount;
        this.paymentMethodId = paymentMethodId;
        this.discountId = discountId;
        this.saleStatus = saleStatus;
        this.cancelReason = cancelReason;
        this.createdAt = createdAt;
    }

    //getters
    public long getSalesId() {
        return salesId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public long getUserId() {
        return userId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }
    
    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public long getPaymentMethodId() {
        return paymentMethodId;
    }

    public Long getDiscountId() {
        return discountId;
    }

    public Status getStatus() {
        return saleStatus;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
