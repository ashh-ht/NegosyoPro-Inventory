package com.inventory.negosyopro.config;

import com.inventory.negosyopro.model.Users;

import jakarta.servlet.http.HttpSession;

public class CurrentUser {
    static Users get(HttpSession s) {
        return (Users) s.getAttribute("currentUser");
    }

    static boolean isAdmin(Users u) {
        return u.getRole() == Users.Role.ADMIN;
    }
}
