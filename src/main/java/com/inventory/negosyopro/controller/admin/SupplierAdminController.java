package com.inventory.negosyopro.controller.admin;

import java.sql.SQLException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventory.negosyopro.config.CurrentUser;
import com.inventory.negosyopro.dao.SuppliersDAO;
import com.inventory.negosyopro.model.Suppliers;
import com.inventory.negosyopro.service.CatalogService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/suppliers")
public class SupplierAdminController {

    private final SuppliersDAO suppliersDAO;
    private final CatalogService catalogService;

    public SupplierAdminController(SuppliersDAO suppliersDAO, CatalogService catalogService) {
        this.suppliersDAO = suppliersDAO;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        model.addAttribute("suppliers", suppliersDAO.getSuppliersByBusiness(bid, false));
        return "admin/suppliers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        Suppliers supplier = suppliersDAO.getSupplierById(id, bid, false);
        if (supplier == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("supplier", supplier);
        return "admin/supplier-form";
    }

    @PostMapping
    public String add(@RequestParam String supplierName,
            @RequestParam(required = false) String contactPerson,
            @RequestParam(required = false) String contactNumber,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String address,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        try {
            catalogService.addSupplier(new Suppliers(0, bid, supplierName, contactPerson, contactNumber,
                    email, address, true, null));
            flash.addFlashAttribute("success", "Supplier added.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id, @RequestParam String supplierName,
            @RequestParam(required = false) String contactPerson,
            @RequestParam(required = false) String contactNumber,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String address,
            @RequestParam(defaultValue = "true") boolean active,
            HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        if (supplierName == null || supplierName.isBlank()) {
            flash.addFlashAttribute("error", "Supplier name cannot be blank.");
            return "redirect:/admin/suppliers/" + id + "/edit";
        }
        boolean ok = suppliersDAO.updateSupplier(new Suppliers(id, bid, supplierName.trim(), contactPerson,
                contactNumber, email, address, active, null));
        flash.addFlashAttribute(ok ? "success" : "error", ok ? "Supplier updated." : "Supplier not found.");
        return "redirect:/admin/suppliers";
    }

    @PostMapping("/{id}/active")
    public String setActive(@PathVariable long id, @RequestParam boolean active,
            HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        boolean ok = suppliersDAO.setSupplierActive(id, bid, active);
        flash.addFlashAttribute(ok ? "success" : "error", ok ? "Supplier updated." : "Supplier not found.");
        return "redirect:/admin/suppliers";
    }
}
