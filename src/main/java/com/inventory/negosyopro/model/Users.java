package com.inventory.negosyopro.model;

public class Users {
    public enum Role {
        ADMIN,
        EMPLOYEE
    }

    private long userId;
    private long businessId;
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private Role role;
    private boolean isActive;
    private boolean isOwner;

    public Users(long userId, long businessId, String username, String passwordHash, String firstName,
            String lastName, Role role, boolean isActive, boolean isOwner) {
        this.userId = userId;
        this.businessId = businessId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.isActive = isActive;
        this.isOwner = isOwner;
    }

    public long getUserId() {
        return userId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setBusinessId(long businessId) {
        this.businessId = businessId;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

}