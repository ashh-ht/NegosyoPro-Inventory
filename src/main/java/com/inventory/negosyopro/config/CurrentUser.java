package com.inventory.negosyopro.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.inventory.negosyopro.model.Users;

import jakarta.servlet.http.HttpSession;

// Controllers live in other packages, so every method here must be public.
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Users get(HttpSession session) {
        return (Users) session.getAttribute("currentUser");
    }

    public static boolean isAdmin(Users u) {
        return u.getRole() == Users.Role.ADMIN;
    }

    public static boolean isEmployee(Users u) {
        return u.getRole() == Users.Role.EMPLOYEE;
    }

    // Module guard: pass the business flag (usesExpiry / usesPurchaseOrders).
    // Flag off = 404, so typing the URL cannot bypass a hidden tab.
    public static void requireModule(boolean enabled) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
