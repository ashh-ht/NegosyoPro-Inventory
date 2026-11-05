package com.inventory.negosyopro.config;

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
}
