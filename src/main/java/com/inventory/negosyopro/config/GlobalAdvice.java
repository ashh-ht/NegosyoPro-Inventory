package com.inventory.negosyopro.config;

import java.sql.SQLException;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import com.inventory.negosyopro.dao.BusinessDAO;
import com.inventory.negosyopro.model.Business;
import com.inventory.negosyopro.model.Users;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalAdvice {

    private final BusinessDAO businessDAO;

    public GlobalAdvice(BusinessDAO businessDAO) {
        this.businessDAO = businessDAO;
    }

    // Available in every template as ${me}
    @ModelAttribute("me")
    public Users me(HttpSession session) {
        return (Users) session.getAttribute("currentUser"); // null on /login
    }

    // Available in every template as ${business}
    @ModelAttribute("business")
    public Business business(HttpSession session) {
        Users user = (Users) session.getAttribute("currentUser");
        if (user == null) {
            return null; // nobody logged in yet
        }
        return businessDAO.getBusinessById(user.getBusinessId());
    }

    // Any SQLException that a controller doesn't catch lands here
    @ExceptionHandler(SQLException.class)
    public ModelAndView handleSql(SQLException e) {
        e.printStackTrace(); // full detail in the console
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", "Something went wrong with the database. Please try again.");
        return mav;
    }
}