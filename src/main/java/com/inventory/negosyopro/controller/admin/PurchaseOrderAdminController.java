package com.inventory.negosyopro.controller.admin;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventory.negosyopro.config.CurrentUser;
import com.inventory.negosyopro.dao.BusinessDAO;
import com.inventory.negosyopro.dao.ProductsDAO;
import com.inventory.negosyopro.dao.PurchaseOrdersDAO;
import com.inventory.negosyopro.dao.SuppliersDAO;
import com.inventory.negosyopro.model.Business;
import com.inventory.negosyopro.model.PurchaseOrderItems;
import com.inventory.negosyopro.model.PurchaseOrders;
import com.inventory.negosyopro.model.ReceivedItems;
import com.inventory.negosyopro.model.Users;
import com.inventory.negosyopro.service.PurchaseOrderService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/purchase-orders")
public class PurchaseOrderAdminController {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrdersDAO purchaseOrdersDAO;
    private final SuppliersDAO suppliersDAO;
    private final ProductsDAO productsDAO;
    private final BusinessDAO businessDAO;

    public PurchaseOrderAdminController(PurchaseOrderService purchaseOrderService,
            PurchaseOrdersDAO purchaseOrdersDAO, SuppliersDAO suppliersDAO, ProductsDAO productsDAO,
            BusinessDAO businessDAO) {
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrdersDAO = purchaseOrdersDAO;
        this.suppliersDAO = suppliersDAO;
        this.productsDAO = productsDAO;
        this.businessDAO = businessDAO;
    }

    // Runs before EVERY handler below (list, new, create, detail, order, cancel,
    // receive, and the restock
    // suggestions on the new-order form). Purchase orders off = 404 for all of
    // them.
    @ModelAttribute
    public void requirePurchaseOrders(HttpSession session) {
        Business business = businessDAO.getBusinessById(CurrentUser.get(session).getBusinessId());
        CurrentUser.requireModule(business != null && business.isUsesPurchaseOrders());
    }

    // ?status=DRAFT|ORDERED|RECEIVED|CANCELLED, or no status for all
    @GetMapping
    public String list(@RequestParam(required = false) PurchaseOrders.Status status,
            HttpSession session, Model model) throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        model.addAttribute("orders", purchaseOrdersDAO.getPurchaseOrders(bid, status));
        model.addAttribute("suppliers", suppliersDAO.getSuppliersByBusiness(bid, false)); // to show supplier names
        model.addAttribute("status", status);
        return "admin/purchase-orders";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        model.addAttribute("suppliers", suppliersDAO.getSuppliersByBusiness(bid, true));
        model.addAttribute("products", productsDAO.getProductsByBusiness(bid, true));
        model.addAttribute("suggestions", purchaseOrderService.getRestockSuggestions(bid));
        return "admin/purchase-order-form";
    }

    @PostMapping
    public String create(@RequestParam long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedArrival,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) List<Long> productId,
            @RequestParam(required = false) List<Integer> quantity,
            @RequestParam(required = false) List<BigDecimal> unitCost,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        Users user = CurrentUser.get(session);

        List<PurchaseOrderItems> items = new ArrayList<>();
        if (productId != null && quantity != null && unitCost != null
                && productId.size() == quantity.size() && productId.size() == unitCost.size()) {
            for (int i = 0; i < productId.size(); i++) {
                if (productId.get(i) == null || quantity.get(i) == null || quantity.get(i) <= 0
                        || unitCost.get(i) == null) {
                    continue; // skip blank rows
                }
                items.add(new PurchaseOrderItems(0, 0, productId.get(i), quantity.get(i), 0, unitCost.get(i)));
            }
        }
        if (items.isEmpty()) {
            flash.addFlashAttribute("error", "Add at least one product with a quantity and cost.");
            return "redirect:/admin/purchase-orders/new";
        }

        try {
            PurchaseOrders header = new PurchaseOrders(0, user.getBusinessId(), supplierId, user.getUserId(),
                    null, PurchaseOrders.Status.DRAFT, null, expectedArrival, null, notes, null);
            long id = purchaseOrderService.createPurchaseOrder(header, items); // numbers it PO-0001 itself
            flash.addFlashAttribute("success", "Purchase order created as a draft.");
            return "redirect:/admin/purchase-orders/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/purchase-orders/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        PurchaseOrders po = purchaseOrdersDAO.getPurchaseOrderById(id, bid);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("order", po);
        model.addAttribute("items", purchaseOrdersDAO.getItemsByPurchaseOrder(id, bid));
        model.addAttribute("products", productsDAO.getProductsByBusiness(bid, false)); // to show product names
        model.addAttribute("supplier", suppliersDAO.getSupplierById(po.getSupplierId(), bid, false));
        return "admin/purchase-order-detail";
    }

    // markOrdered and cancelPurchaseOrder take the BUSINESS id.
    @PostMapping("/{id}/order")
    public String markOrdered(@PathVariable long id, HttpSession session, RedirectAttributes flash)
            throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        try {
            boolean ok = purchaseOrderService.markOrdered(id, bid);
            flash.addFlashAttribute(ok ? "success" : "error",
                    ok ? "Marked as ordered." : "Could not update the order.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/purchase-orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable long id, HttpSession session, RedirectAttributes flash)
            throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        try {
            boolean ok = purchaseOrderService.cancelPurchaseOrder(id, bid);
            flash.addFlashAttribute(ok ? "success" : "error", ok ? "Order cancelled." : "Could not cancel the order.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/purchase-orders/" + id;
    }

    // receivePurchaseOrder takes the USER id. Only an ORDERED order can be
    // received.
    @PostMapping("/{id}/receive")
    public String receive(@PathVariable long id,
            @RequestParam(required = false) List<Long> itemId,
            @RequestParam(required = false) List<Integer> qtyReceived,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) List<LocalDate> expirationDate,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        Users user = CurrentUser.get(session);

        List<ReceivedItems> received = new ArrayList<>();
        if (itemId != null && qtyReceived != null && itemId.size() == qtyReceived.size()) {
            for (int i = 0; i < itemId.size(); i++) {
                Integer qty = qtyReceived.get(i);
                if (itemId.get(i) == null || qty == null || qty <= 0) {
                    continue; // nothing arrived for this line
                }
                LocalDate expiry = (expirationDate != null && i < expirationDate.size()) ? expirationDate.get(i) : null;
                received.add(new ReceivedItems(itemId.get(i), qty, expiry));
            }
        }
        if (received.isEmpty()) {
            flash.addFlashAttribute("error", "Enter the quantity received for at least one item.");
            return "redirect:/admin/purchase-orders/" + id;
        }

        try {
            purchaseOrderService.receivePurchaseOrder(id, user.getUserId(), received);
            flash.addFlashAttribute("success", "Order received. Stock was updated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/purchase-orders/" + id;
    }
}
