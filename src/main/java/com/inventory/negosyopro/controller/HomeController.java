package com.inventory.negosyopro.controller;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.inventory.negosyopro.db.dbConnection;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("name", "World");
        return "index";
    }

    @GetMapping("/db-test")
    @ResponseBody
    public String dbTest() {
        try (Connection conn = dbConnection.dbConnection()) {
            return "Connected to " + conn.getCatalog();
        } catch (SQLException e) {
            return "Failed: " + e.getMessage();
        }
    }

}