package com.inventory.negosyopro.controller.admin;

import java.sql.SQLException;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
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
import com.inventory.negosyopro.dao.SalesDAO;
import com.inventory.negosyopro.model.Sales;
import com.inventory.negosyopro.model.Users;
import com.inventory.negosyopro.service.SalesService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/sales")
public class SaleAdminController {

    private final SalesDAO salesDAO;
    private final SalesService salesService;

    public SaleAdminController(SalesDAO salesDAO, SalesService salesService) {
        this.salesDAO = salesDAO;
        this.salesService = salesService;
    }

    @GetMapping
    public String history(@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session, Model model) throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        if (to == null) {
            to = LocalDate.now();
        }
        if (from == null) {
            from = to.minusDays(29); // default: last 30 days
        }
        if (to.isBefore(from)) {
            model.addAttribute("error", "The end date is before the start date.");
            to = from;
        }
        model.addAttribute("sales", salesDAO.getSales(bid, from, to)); // includes cancelled and refunded
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/sales-history";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") long id, HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        Sales sale = salesDAO.getSaleById(id, bid);
        if (sale == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("sale", sale);
        model.addAttribute("items", salesDAO.getItemsBySale(id, bid));
        return "admin/sale-detail";
    }

    // cancelSale takes the USER id. Stock goes back into the batches.
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable("id") long id, @RequestParam(value = "reason") String reason,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        Users user = CurrentUser.get(session);
        if (reason == null || reason.isBlank()) {
            flash.addFlashAttribute("error", "Please give a reason for cancelling.");
            return "redirect:/admin/sales/" + id;
        }
        try {
            boolean ok = salesService.cancelSale(id, user.getUserId(), reason.trim());
            flash.addFlashAttribute(ok ? "success" : "error",
                    ok ? "Sale cancelled and stock returned." : "This sale could not be cancelled.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sales/" + id;
    }
}
