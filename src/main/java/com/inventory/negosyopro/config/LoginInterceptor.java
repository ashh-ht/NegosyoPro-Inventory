package com.inventory.negosyopro.config;

import com.inventory.negosyopro.model.Users;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler)
            throws Exception {

        HttpSession session = req.getSession(false); // false = don't create one
        Users user = (session == null) ? null : (Users) session.getAttribute("currentUser");

        // Rule 1: must be logged in
        if (user == null) {
            res.sendRedirect("/login");
            return false; // stop: the page is not shown
        }

        // Rule 2: employees can't open anything under /admin
        boolean isEmployee = user.getRole() == Users.Role.EMPLOYEE;
        if (isEmployee && req.getRequestURI().startsWith("/admin")) {
            res.sendRedirect("/dashboard?denied");
            return false;
        }

        return true; // allowed: continue to the controller
    }
}