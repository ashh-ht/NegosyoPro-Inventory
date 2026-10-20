package com.inventory.negosyopro.model;

import java.time.*;

public class Suppliers {
    private long supplierId;
    private long businessId;
    private String supplierName;
    private String contactPerson;
    private String contactNumber;
    private String email;
    private String address;
    private boolean isActive;
    private LocalDateTime createdAt;

    public Suppliers(long supplierId, long businessId, String supplierName, String contactPerson,
            String contactNumber, String email, String address, boolean isActive, LocalDateTime createdAt) {
        this.supplierId = supplierId;
        this.businessId = businessId;
        this.supplierName = supplierName;
        this.contactPerson = contactPerson;
        this.contactNumber = contactNumber;
        this.email = email;
        this.address = address;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    //getters

    public long getSupplierId() {
        return supplierId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
