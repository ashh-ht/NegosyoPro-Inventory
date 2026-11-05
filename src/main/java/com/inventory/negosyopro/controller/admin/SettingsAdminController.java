package com.inventory.negosyopro.controller.admin;

import java.math.BigDecimal;
import java.sql.SQLException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventory.negosyopro.config.CurrentUser;
import com.inventory.negosyopro.dao.BusinessDAO;
import com.inventory.negosyopro.dao.DiscountsDAO;
import com.inventory.negosyopro.dao.PaymentMethodsDAO;
import com.inventory.negosyopro.model.Business;
import com.inventory.negosyopro.model.Discounts;
import com.inventory.negosyopro.model.PaymentMethods;
import com.inventory.negosyopro.service.CatalogService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/settings")
public class SettingsAdminController {

    private final BusinessDAO businessDAO;
    private final PaymentMethodsDAO paymentMethodsDAO;
    private final DiscountsDAO discountsDAO;
    private final CatalogService catalogService;

    public SettingsAdminController(BusinessDAO businessDAO, PaymentMethodsDAO paymentMethodsDAO,
            DiscountsDAO discountsDAO, CatalogService catalogService) {
        this.businessDAO = businessDAO;
        this.paymentMethodsDAO = paymentMethodsDAO;
        this.discountsDAO = discountsDAO;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String page(HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        model.addAttribute("settings", businessDAO.getBusinessById(bid));
        model.addAttribute("paymentMethods", paymentMethodsDAO.getPaymentMethodsByBusiness(bid, false));
        model.addAttribute("discounts", discountsDAO.getDiscountsByBusiness(bid, false));
        return "admin/settings";
    }

    @PostMapping("/business")
    public String saveBusiness(@RequestParam String businessName,
            @RequestParam(required = false) String businessAddress,
            @RequestParam(required = false) String businessContact,
            @RequestParam(defaultValue = "false") boolean isTaxEnabled,
            @RequestParam(defaultValue = "false") boolean isTaxInclusive,
            @RequestParam(required = false) BigDecimal taxRate,
            @RequestParam String currencyCode, @RequestParam String currencySymbol,
            @RequestParam(required = false) String receiptFooter,
            @RequestParam(defaultValue = "false") boolean isUsesExpiry,
            @RequestParam(defaultValue = "false") boolean isUsesPurchaseOrders,
            @RequestParam int expiryAlertDays,
            @RequestParam(required = false) BigDecimal orderingCost,
            @RequestParam(required = false) BigDecimal holdingCost,
            HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        Business old = businessDAO.getBusinessById(bid);
        if (old == null) {
            flash.addFlashAttribute("error", "Business not found.");
            return "redirect:/admin/settings";
        }
        if (businessName == null || businessName.isBlank() || expiryAlertDays < 0
                || (taxRate != null && taxRate.signum() < 0)) {
            flash.addFlashAttribute("error", "Enter a business name and non-negative numbers.");
            return "redirect:/admin/settings";
        }

        // Keep the old value when a number box is left blank. templateId and createdAt are never edited here.
        Business updated = new Business(bid, old.getTemplateId(), businessName.trim(), businessAddress,
                businessContact, isTaxEnabled, isTaxInclusive, taxRate == null ? old.getTaxRate() : taxRate,
                currencyCode, currencySymbol, receiptFooter, isUsesExpiry, isUsesPurchaseOrders,
                expiryAlertDays, orderingCost == null ? old.getOrderingCost() : orderingCost,
                holdingCost == null ? old.getHoldingCost() : holdingCost, old.getCreatedAt());
        boolean ok = businessDAO.updateBusiness(updated);
        flash.addFlashAttribute(ok ? "success" : "error", ok ? "Settings saved." : "Could not save the settings.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/payment-methods")
    public String addPaymentMethod(@RequestParam String methodName, HttpSession session,
            RedirectAttributes flash) throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        try {
            catalogService.addPaymentMethod(new PaymentMethods(0, bid, methodName, true));
            flash.addFlashAttribute("success", "Payment method added.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/payment-methods/{id}/active")
    public String setPaymentActive(@PathVariable long id, @RequestParam boolean active,
            HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        boolean ok = paymentMethodsDAO.setPaymentMethodActive(id, bid, active);
        flash.addFlashAttribute(ok ? "success" : "error", ok ? "Payment method updated." : "Not found.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/discounts")
    public String addDiscount(@RequestParam String discountName, @RequestParam BigDecimal discountPercent,
            @RequestParam(defaultValue = "false") boolean requiresId, HttpSession session,
            RedirectAttributes flash) throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        try {
            catalogService.addDiscount(new Discounts(0, bid, discountName, discountPercent, requiresId, true));
            flash.addFlashAttribute("success", "Discount added.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/discounts/{id}/active")
    public String setDiscountActive(@PathVariable long id, @RequestParam boolean active,
            HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        boolean ok = discountsDAO.setDiscountActive(id, bid, active);
        flash.addFlashAttribute(ok ? "success" : "error", ok ? "Discount updated." : "Not found.");
        return "redirect:/admin/settings";
    }
}
