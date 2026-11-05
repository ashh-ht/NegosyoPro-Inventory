package com.inventory.negosyopro.controller;

import java.math.BigDecimal;
import java.sql.SQLException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.inventory.negosyopro.dao.TemplateDAO;
import com.inventory.negosyopro.model.Business;
import com.inventory.negosyopro.model.Users;
import com.inventory.negosyopro.service.SetupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/setup")
@Controller
public class SetupController {
    private final SetupService setupService;
    private final TemplateDAO templateDAO;

    public SetupController(SetupService setupService, TemplateDAO templateDAO) {
        this.setupService = setupService;
        this.templateDAO = templateDAO;
    }

    @GetMapping
    public String setupPage(Model model) {
        model.addAttribute("templates", templateDAO.getAllTemplates());
        return "setup";
    }

    @PostMapping
    public String setup(@RequestParam(value = "templateId") long templateId,
            @RequestParam(value = "businessName") String businessName,
            @RequestParam(value = "businessAddress") String businessAddress,
            @RequestParam(value = "businessContact") String businessContact,
            @RequestParam(value = "isTaxEnabled", defaultValue = "false") boolean isTaxEnabled,
            @RequestParam(value = "isTaxInclusive", defaultValue = "false") boolean isTaxInclusive,
            @RequestParam(value = "taxRate") BigDecimal taxRate,
            @RequestParam(value = "currencyCode") String currencyCode,
            @RequestParam(value = "currencySymbol") String currencySymbol,
            @RequestParam(value = "receiptFooter") String receiptFooter,
            @RequestParam(value = "isUsesExpiry", defaultValue = "false") boolean isUsesExpiry,
            @RequestParam(value = "isUsesPurchaseOrders", defaultValue = "false") boolean isUsesPurchaseOrders,
            @RequestParam(value = "expiryAlertDays") int expiryAlertDays,
            @RequestParam(value = "orderingCost") BigDecimal orderingCost,
            @RequestParam(value = "holdingCost") BigDecimal holdingCost,
            @RequestParam(value = "username") String username,
            @RequestParam(value = "firstName") String firstName,
            @RequestParam(value = "lastName") String lastName,
            @RequestParam(value = "password") String password,
            RedirectAttributes flash) {

        Business business = new Business(templateId, businessName, businessAddress, businessContact,
                isTaxEnabled, isTaxInclusive, taxRate, currencyCode, currencySymbol, receiptFooter,
                isUsesExpiry, isUsesPurchaseOrders, expiryAlertDays, orderingCost, holdingCost);
        Users owner = new Users(0, 0, username, null, firstName, lastName, Users.Role.ADMIN, true, true);

        try {
            setupService.createBusinessWithOwner(business, owner, password);
            flash.addFlashAttribute("success", "Business created. You can log in now.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/setup";
        } catch (SQLException e) {
            e.printStackTrace();
            flash.addFlashAttribute("error", "Could not save the business. Please try again.");
            return "redirect:/setup";
        }
    }

}
