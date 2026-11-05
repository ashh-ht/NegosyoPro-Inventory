package com.inventory.negosyopro.controller;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Users;

import jakarta.servlet.http.HttpSession;

// Entry points that are not pages of their own: "/" and the old wireframe URLs.
// The real pages are in AuthController (/login), SetupController (/setup) and the
// other controllers.
@Controller
public class PublicController {

    // "/" sends you to the dashboard when logged in, otherwise to the login page.
    // (WebConfig lets "/" through the login check, so the session is read here.)
    @GetMapping("/")
    public String home(HttpSession session) {
        Users user = (Users) session.getAttribute("currentUser");
        return user == null ? "redirect:/login" : "redirect:/dashboard";
    }

    // Old wireframe links. Kept so bookmarks keep working.
    @GetMapping("/signin")
    public String signIn() {
        return "redirect:/login";
    }

    @GetMapping({ "/create-business/step1", "/create-business/step2", "/create-business/step3" })
    public String createBusiness() {
        return "redirect:/setup";
    }

    @GetMapping("/my-account")
    public String myAccount() {
        return "redirect:/account";
    }

    // Quick database check. Not exempt from the login check, so only logged-in users can call it.
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
