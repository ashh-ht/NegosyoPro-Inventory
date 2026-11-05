package com.inventory.negosyopro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.inventory.negosyopro.config.CurrentUser;
import com.inventory.negosyopro.dao.CategoriesDAO;
import com.inventory.negosyopro.dao.ProductsDAO;
import com.inventory.negosyopro.model.Users;

import jakarta.servlet.http.HttpSession;

// Read-only product list for everyone. Add/Edit/Restock live in /admin/products.
@Controller
public class ProductController {

    private final ProductsDAO productsDAO;
    private final CategoriesDAO categoriesDAO;

    public ProductController(ProductsDAO productsDAO, CategoriesDAO categoriesDAO) {
        this.productsDAO = productsDAO;
        this.categoriesDAO = categoriesDAO;
    }

    @GetMapping("/products")
    public String list(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            HttpSession session, Model model) {
        Users user = CurrentUser.get(session);
        long bid = user.getBusinessId();

        if (q != null && !q.isBlank()) {
            model.addAttribute("products", productsDAO.searchProducts(bid, q.trim()));
        } else if (categoryId != null) {
            model.addAttribute("products", productsDAO.getProductByCateg(bid, categoryId, true));
        } else {
            model.addAttribute("products", productsDAO.getProductsByBusiness(bid, true));
        }
        model.addAttribute("categories", categoriesDAO.getCategoriesByBusiness(bid, true));
        model.addAttribute("q", q);                     // keeps the search box filled in
        model.addAttribute("selectedCategoryId", categoryId);
        return "products";
    }
}
