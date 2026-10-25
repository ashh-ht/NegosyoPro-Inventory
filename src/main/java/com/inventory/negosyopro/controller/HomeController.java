package com.inventory.negosyopro.controller;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.inventory.negosyopro.db.dbConnection;

@Controller
public class HomeController {

    // ---------- Start / account ----------
    @GetMapping("/")
    public String home() {
        return "StartingPage";
    }

    @GetMapping("/signin")
    public String signIn() {
        return "StartingPageSignIn";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }

    @GetMapping("/my-account")
    public String myAccount() {
        return "MyAccount";
    }

    // ---------- Create business wizard ----------
    @GetMapping("/create-business/step1")
    public String step1() {
        return "CreateBusinessStep1";
    }

    @GetMapping("/create-business/step2")
    public String step2() {
        return "CreatingBusinessStep2";
    }

    @GetMapping("/create-business/step3")
    public String step3() {
        return "CreatingBusinessStep3";
    }

    @GetMapping("/select-template/{type}") // retail, food, salon
    public String selectTemplate(@PathVariable String type) {
        return "redirect:/create-business/step3";
    }

    // ---------- Main pages ----------
    @GetMapping("/dashboard")
    public String dashboard() {
        return "Dashboard";
    }

    @GetMapping("/products")
    public String products() {
        return "Products";
    }

    @GetMapping("/products/expiring")
    public String expiring() {
        return "ProductsExpiring";
    }

    @GetMapping("/sales")
    public String sales() {
        return "Sales";
    }

    @GetMapping("/sales/checkout")
    public String checkout() {
        return "SalesCheckoutDetails";
    }

    @GetMapping("/sales/transactions")
    public String salesTx() {
        return "SalesTransactions";
    }

    @GetMapping("/suppliers")
    public String suppliers() {
        return "Suppliers";
    }

    @GetMapping("/suppliers/purchase-orders")
    public String pos() {
        return "SuppliersPurchaseOrders";
    }

    @GetMapping("/suppliers/restock-suggestions")
    public String restock() {
        return "SuppliersRestockSuggestions";
    }

    @GetMapping("/reports")
    public String reports() {
        return "Reports";
    }

    @GetMapping("/reports/revenue")
    public String repRevenue() {
        return "ReportsRevenue";
    }

    @GetMapping("/reports/inventory")
    public String repInventory() {
        return "ReportsInventory";
    }

    @GetMapping("/reports/transactions")
    public String repTx() {
        return "ReportsTransactions";
    }

    @GetMapping("/settings")
    public String settings() {
        return "Settings";
    }

    @GetMapping("/settings/inventory")
    public String setInv() {
        return "SettingsInventory";
    }

    @GetMapping("/settings/tax-currency")
    public String setTax() {
        return "SettingsTaxCurrency";
    }

    @GetMapping("/settings/payment-methods")
    public String setPay() {
        return "SettingsPaymentMethods";
    }

    @GetMapping("/settings/discounts")
    public String setDisc() {
        return "SettingsDiscounts";
    }

    @GetMapping("/settings/users")
    public String setUsers() {
        return "SettingsUsers";
    }

    // ---------- DB check ----------
    @GetMapping("/db-test")
    @ResponseBody
    public String dbTest() {
        try (Connection conn = dbConnection.getConnection()) {
            return "Connected to " + conn.getCatalog();
        } catch (SQLException e) {
            return "Failed: " + e.getMessage();
        }
    }
}