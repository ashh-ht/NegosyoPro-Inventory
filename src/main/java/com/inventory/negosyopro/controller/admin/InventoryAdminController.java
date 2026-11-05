package com.inventory.negosyopro.controller.admin;

import java.math.BigDecimal;
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
import com.inventory.negosyopro.dao.BusinessDAO;
import com.inventory.negosyopro.dao.InventoryTransactionsDAO;
import com.inventory.negosyopro.dao.ProductsDAO;
import com.inventory.negosyopro.dao.StockBatchesDAO;
import com.inventory.negosyopro.dao.SuppliersDAO;
import com.inventory.negosyopro.model.Business;
import com.inventory.negosyopro.model.Products;
import com.inventory.negosyopro.model.Users;
import com.inventory.negosyopro.service.InventoryService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/inventory")
public class InventoryAdminController {

    private final ProductsDAO productsDAO;
    private final StockBatchesDAO stockBatchesDAO;
    private final InventoryTransactionsDAO transactionsDAO;
    private final SuppliersDAO suppliersDAO;
    private final BusinessDAO businessDAO;
    private final InventoryService inventoryService;

    public InventoryAdminController(ProductsDAO productsDAO, StockBatchesDAO stockBatchesDAO,
            InventoryTransactionsDAO transactionsDAO, SuppliersDAO suppliersDAO, BusinessDAO businessDAO,
            InventoryService inventoryService) {
        this.productsDAO = productsDAO;
        this.stockBatchesDAO = stockBatchesDAO;
        this.transactionsDAO = transactionsDAO;
        this.suppliersDAO = suppliersDAO;
        this.businessDAO = businessDAO;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String overview(HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        Business business = businessDAO.getBusinessById(bid);

        model.addAttribute("products", productsDAO.getProductsByBusiness(bid, true));
        model.addAttribute("lowStock", productsDAO.getLowStockProducts(bid));
        model.addAttribute("expired", stockBatchesDAO.getExpiredBatches(bid));
        if (business != null && business.isUsesExpiry()) {
            model.addAttribute("expiring", stockBatchesDAO.getExpiringBatches(bid, business.getExpiryAlertDays()));
        }
        return "admin/inventory";
    }

    // Stock history of one product, plus the restock / adjust forms.
    @GetMapping("/{productId}")
    public String history(@PathVariable("productId") long productId, HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        Products product = productsDAO.getProductById(productId, bid, false);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("product", product);
        model.addAttribute("transactions", transactionsDAO.getTransactionsByProduct(productId, bid));
        model.addAttribute("suppliers", suppliersDAO.getSuppliersByBusiness(bid, true)); // for the restock dropdown
        return "admin/inventory-product";
    }

    @PostMapping("/{productId}/restock")
    public String restock(@PathVariable("productId") long productId, @RequestParam(value = "quantity") int quantity,
            @RequestParam(value = "unitCost", required = false) BigDecimal unitCost,          // blank = product's cost price
            @RequestParam(value = "expirationDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDate,
            @RequestParam(value = "supplierId", required = false) Long supplierId,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        Users user = CurrentUser.get(session);
        try {
            inventoryService.receiveStock(productId, user.getUserId(), quantity, unitCost, expirationDate,
                    supplierId);
            flash.addFlashAttribute("success", "Stock received.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/inventory/" + productId;
    }

    @PostMapping("/{productId}/adjust")
    public String adjust(@PathVariable("productId") long productId, @RequestParam(value = "quantityChange") int quantityChange,
            @RequestParam(value = "type") String type, @RequestParam(value = "reason") String reason,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        Users user = CurrentUser.get(session);
        try {
            // type must be "ADJUSTMENT" or "DAMAGE"; the service throws for anything else
            inventoryService.adjustStock(productId, user.getUserId(), quantityChange, type, reason);
            flash.addFlashAttribute("success", "Stock adjusted.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/inventory/" + productId;
    }

    @PostMapping("/batches/{batchId}/writeoff")
    public String writeOff(@PathVariable("batchId") long batchId, HttpSession session, RedirectAttributes flash)
            throws SQLException {
        Users user = CurrentUser.get(session);
        Business business = businessDAO.getBusinessById(user.getBusinessId());
        CurrentUser.requireModule(business != null && business.isUsesExpiry()); // 404 when expiry is off
        try {
            inventoryService.writeOffExpired(batchId, user.getUserId());
            flash.addFlashAttribute("success", "Expired batch written off.");
        } catch (IllegalArgumentException | IllegalStateException e) { // e.g. not expired yet
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/inventory";
    }
}
