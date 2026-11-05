package com.inventory.negosyopro.controller.admin;

import java.math.BigDecimal;
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
import com.inventory.negosyopro.dao.CategoriesDAO;
import com.inventory.negosyopro.dao.ProductSuppliersDAO;
import com.inventory.negosyopro.dao.ProductsDAO;
import com.inventory.negosyopro.dao.SuppliersDAO;
import com.inventory.negosyopro.model.ProductSuppliers;
import com.inventory.negosyopro.model.Products;
import com.inventory.negosyopro.service.CatalogService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/products")
public class ProductAdminController {

    private final ProductsDAO productsDAO;
    private final CategoriesDAO categoriesDAO;
    private final SuppliersDAO suppliersDAO;
    private final ProductSuppliersDAO productSuppliersDAO;
    private final CatalogService catalogService;

    public ProductAdminController(ProductsDAO productsDAO, CategoriesDAO categoriesDAO,
            SuppliersDAO suppliersDAO, ProductSuppliersDAO productSuppliersDAO,
            CatalogService catalogService) {
        this.productsDAO = productsDAO;
        this.categoriesDAO = categoriesDAO;
        this.suppliersDAO = suppliersDAO;
        this.productSuppliersDAO = productSuppliersDAO;
        this.catalogService = catalogService;
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        model.addAttribute("categories", categoriesDAO.getCategoriesByBusiness(bid, true));
        return "admin/product-form";
    }

    @PostMapping
    public String add(@RequestParam long categoryId, @RequestParam String productCode,
            @RequestParam String productName, @RequestParam BigDecimal costPrice,
            @RequestParam BigDecimal sellingPrice, @RequestParam String unit,
            @RequestParam(defaultValue = "false") boolean isTaxable,
            @RequestParam(defaultValue = "false") boolean trackInventory,
            @RequestParam(defaultValue = "false") boolean tracksExpiry,
            @RequestParam(required = false) Integer shelfLifeDays,
            @RequestParam int reorderLevel, @RequestParam int reorderQuantity,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();

        if (productCode != null && productsDAO.getProductByCode(bid, productCode.trim(), false) != null) {
            flash.addFlashAttribute("error", "A product with code " + productCode + " already exists.");
            return "redirect:/admin/products/new";
        }
        try {
            // New products start at stock 0. Opening stock comes from the restock form.
            catalogService.addProduct(new Products(bid, categoryId,
                    productCode == null ? null : productCode.trim(), productName, costPrice, sellingPrice,
                    unit, isTaxable, trackInventory, tracksExpiry, shelfLifeDays, reorderLevel,
                    reorderQuantity));
            flash.addFlashAttribute("success", "Product added. Use Restock to add its opening stock.");
            return "redirect:/products";
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        Products product = productsDAO.getProductById(id, bid, false);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("product", product);
        model.addAttribute("categories", categoriesDAO.getCategoriesByBusiness(bid, true));
        model.addAttribute("productSuppliers", productSuppliersDAO.getSuppliersForProduct(id, bid));
        model.addAttribute("suppliers", suppliersDAO.getSuppliersByBusiness(bid, true));
        return "admin/product-form";
    }

    @PostMapping("/{id}")
    public String save(@PathVariable long id, @RequestParam long categoryId,
            @RequestParam String productCode, @RequestParam String productName,
            @RequestParam BigDecimal costPrice, @RequestParam BigDecimal sellingPrice,
            @RequestParam String unit,
            @RequestParam(defaultValue = "false") boolean isTaxable,
            @RequestParam(defaultValue = "false") boolean trackInventory,
            @RequestParam(defaultValue = "false") boolean tracksExpiry,
            @RequestParam(required = false) Integer shelfLifeDays,
            @RequestParam int reorderLevel, @RequestParam int reorderQuantity,
            HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        Products existing = productsDAO.getProductById(id, bid, false);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (productName == null || productName.isBlank() || costPrice.signum() < 0
                || sellingPrice.signum() < 0) {
            flash.addFlashAttribute("error", "Enter a name and non-negative prices.");
            return "redirect:/admin/products/" + id + "/edit";
        }
        // The existing stock is passed through, so this form can never change stock.
        boolean ok = productsDAO.updateProduct(new Products(id, bid, categoryId, productCode.trim(),
                productName.trim(), costPrice, sellingPrice, unit, isTaxable, trackInventory, tracksExpiry,
                shelfLifeDays, existing.getCurrentStock(), reorderLevel, reorderQuantity,
                existing.isActive()));
        flash.addFlashAttribute(ok ? "success" : "error", ok ? "Product saved." : "Could not save the product.");
        return "redirect:/admin/products/" + id + "/edit";
    }

    // Products are deactivated, never deleted.
    @PostMapping("/{id}/active")
    public String setActive(@PathVariable long id, @RequestParam boolean active,
            HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        boolean ok = productsDAO.setProductActive(id, bid, active);
        flash.addFlashAttribute(ok ? "success" : "error",
                ok ? (active ? "Product activated." : "Product deactivated.") : "Product not found.");
        return "redirect:/products";
    }

    @PostMapping("/{id}/reorder")
    public String updateReorder(@PathVariable long id, @RequestParam int reorderLevel,
            @RequestParam int reorderQuantity, HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        if (reorderLevel < 0 || reorderQuantity < 0) {
            flash.addFlashAttribute("error", "Reorder numbers cannot be negative.");
        } else {
            boolean ok = productsDAO.updateReorderSettings(id, bid, reorderLevel, reorderQuantity);
            flash.addFlashAttribute(ok ? "success" : "error",
                    ok ? "Reorder settings saved." : "Product not found.");
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/{id}/suppliers")
    public String addSupplierLink(@PathVariable long id, @RequestParam long supplierId,
            @RequestParam(required = false) String supplierProductCode,
            @RequestParam(required = false) BigDecimal supplierCost,
            @RequestParam(required = false) Integer leadTimeDays,
            @RequestParam(defaultValue = "false") boolean preferred,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        try {
            catalogService.addProductSupplier(new ProductSuppliers(0, id, supplierId, supplierProductCode,
                    supplierCost, leadTimeDays, preferred), bid);
            flash.addFlashAttribute("success", "Supplier linked.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/{id}/suppliers/{supplierId}/preferred")
    public String setPreferred(@PathVariable long id, @PathVariable long supplierId,
            HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        boolean ok = productSuppliersDAO.setPreferredSupplier(id, supplierId, bid, true);
        flash.addFlashAttribute(ok ? "success" : "error", ok ? "Preferred supplier set." : "Link not found.");
        return "redirect:/admin/products/" + id + "/edit";
    }
}
