package com.inventory.negosyopro.controller;

import java.sql.SQLException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventory.negosyopro.config.CurrentUser;
import com.inventory.negosyopro.model.Users;
import com.inventory.negosyopro.service.AuthService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    // The user is already in the model as ${me} (GlobalAdvice).
    @GetMapping("/account")
    public String accountPage() {
        return "account";
    }

    @PostMapping("/account/password")
    public String changePassword(@RequestParam(value = "oldPassword") String oldPassword,
            @RequestParam(value = "newPassword") String newPassword,
            @RequestParam(value = "confirmPassword") String confirmPassword,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        Users user = CurrentUser.get(session);

        if (!newPassword.equals(confirmPassword)) {
            flash.addFlashAttribute("error", "New password and confirmation do not match.");
            return "redirect:/account";
        }
        try {
            if (authService.changePassword(user.getUserId(), oldPassword, newPassword)) {
                flash.addFlashAttribute("success", "Password changed.");
            } else {
                flash.addFlashAttribute("error", "Old password is incorrect.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account";
    }
}
