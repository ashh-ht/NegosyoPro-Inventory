package com.inventory.negosyopro.controller;

import org.springframework.stereotype.Controller;

import com.inventory.negosyopro.model.Users;
import com.inventory.negosyopro.service.AuthService;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
            HttpSession session, RedirectAttributes flash) {

        Users user = authService.login(username, password); // keep the result

        if (user != null) {
            session.setAttribute("currentUser", user); // semicolon
            return "redirect:/dashboard";
        } else {
            flash.addFlashAttribute("error", "Invalid username or password.");
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession s) {
        s.invalidate(); // destroy the session
        return "redirect:/login";
    }

}
