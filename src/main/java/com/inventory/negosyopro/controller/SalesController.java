package com.inventory.negosyopro.controller;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventory.negosyopro.config.CurrentUser;
import com.inventory.negosyopro.dao.DiscountsDAO;
import com.inventory.negosyopro.dao.PaymentMethodsDAO;
import com.inventory.negosyopro.dao.ProductsDAO;
import com.inventory.negosyopro.dao.SalesDAO;
import com.inventory.negosyopro.model.Products;
import com.inventory.negosyopro.model.SaleItems;
import com.inventory.negosyopro.model.Sales;
import com.inventory.negosyopro.model.Users;
import com.inventory.negosyopro.service.SalesService;

import jakarta.servlet.http.HttpSession;

// The POS. Employees and admins both use it.
@Controller
public class SalesController {

    private final SalesService salesService;
    private final SalesDAO salesDAO;
    private final ProductsDAO productsDAO;
    private final PaymentMethodsDAO paymentMethodsDAO;
    private final DiscountsDAO discountsDAO;

    public SalesController(SalesService salesService, SalesDAO salesDAO, ProductsDAO productsDAO,
            PaymentMethodsDAO paymentMethodsDAO, DiscountsDAO discountsDAO) {
        this.salesService = salesService;
        this.salesDAO = salesDAO;
        this.productsDAO = productsDAO;
        this.paymentMethodsDAO = paymentMethodsDAO;
        this.discountsDAO = discountsDAO;
    }

    @GetMapping("/sales")
    public String posPage(HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();

        // only things that can be sold: in stock, or not stock-tracked
        List<Products> sellable = new ArrayList<>();
        for (Products p : productsDAO.getProductsByBusiness(bid, true)) {
            if (!p.isTrackInventory() || p.getCurrentStock() > 0) {
                sellable.add(p);
            }
        }
        model.addAttribute("products", sellable);
        model.addAttribute("paymentMethods", paymentMethodsDAO.getPaymentMethodsByBusiness(bid, true));
        model.addAttribute("discounts", discountsDAO.getDiscountsByBusiness(bid, true));
        return "sales";
    }

    // Optional live total: call it with fetch() from the POS page. Returns plain text like "125.50".
    @PostMapping("/sales/preview")
    @ResponseBody
    public String preview(@RequestParam(required = false) Long discountId,
            @RequestParam(required = false) List<Long> productId,
            @RequestParam(required = false) List<Integer> quantity,
            HttpSession session) {
        long bid = CurrentUser.get(session).getBusinessId();
        try {
            List<SaleItems> cart = buildCart(productId, quantity);
            return salesService.previewTotal(bid, discountId, cart).toPlainString();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "0.00"; // empty or invalid cart: just show zero
        }
    }

    @PostMapping("/sales/checkout")
    public String checkout(@RequestParam long paymentMethodId,
            @RequestParam BigDecimal amountPaid,
            @RequestParam(required = false) Long discountId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerContact,
            @RequestParam(required = false) List<Long> productId,
            @RequestParam(required = false) List<Integer> quantity,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        Users user = CurrentUser.get(session);
        long bid = user.getBusinessId();
        long uid = user.getUserId();

        // blank text boxes become null
        if (customerName != null && customerName.isBlank()) {
            customerName = null;
        }
        if (customerContact != null && customerContact.isBlank()) {
            customerContact = null;
        }

        try {
            List<SaleItems> cart = buildCart(productId, quantity);
            // The service fills in prices, tax, discount and totals from the database.
            Sales header = new Sales(0, bid, uid, null, customerName, customerContact,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    amountPaid, BigDecimal.ZERO, paymentMethodId, discountId,
                    Sales.Status.COMPLETED, null, null);
            long saleId = salesService.completeSale(header, cart);
            return "redirect:/sales/" + saleId + "/receipt";
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage()); // nothing was saved
            return "redirect:/sales";
        }
    }

    @GetMapping("/sales/{id}/receipt")
    public String receipt(@PathVariable long id, HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        Sales sale = salesDAO.getSaleById(id, bid); // null if it belongs to another business
        if (sale == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND); // shows error/404
        }
        model.addAttribute("sale", sale);
        model.addAttribute("items", salesDAO.getItemsBySale(id, bid));
        // ${business} (footer, currency symbol) is already in the model from GlobalAdvice
        return "receipt";
    }

    // The service only reads productId and quantity; everything else is filled from the database.
    private List<SaleItems> buildCart(List<Long> productId, List<Integer> quantity) {
        List<SaleItems> cart = new ArrayList<>();
        if (productId == null || quantity == null || productId.size() != quantity.size()) {
            throw new IllegalArgumentException("The cart is empty.");
        }
        for (int i = 0; i < productId.size(); i++) {
            Integer qty = quantity.get(i);
            if (productId.get(i) == null || qty == null || qty <= 0) {
                continue; // skip blank rows
            }
            cart.add(new SaleItems(0, 0, productId.get(i), "", qty,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("The cart is empty.");
        }
        return cart;
    }
}
