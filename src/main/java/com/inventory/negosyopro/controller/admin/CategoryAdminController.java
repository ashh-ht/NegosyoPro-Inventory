package com.inventory.negosyopro.controller.admin;

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
import com.inventory.negosyopro.dao.CategoriesDAO;
import com.inventory.negosyopro.model.Categories;
import com.inventory.negosyopro.service.CatalogService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/categories")
public class CategoryAdminController {

    private final CategoriesDAO categoriesDAO;
    private final CatalogService catalogService;

    public CategoryAdminController(CategoriesDAO categoriesDAO, CatalogService catalogService) {
        this.categoriesDAO = categoriesDAO;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        model.addAttribute("categories", categoriesDAO.getCategoriesByBusiness(bid, false)); // all, so inactive can be re-enabled
        return "admin/categories";
    }

    @PostMapping
    public String add(@RequestParam String categoryName, HttpSession session, RedirectAttributes flash)
            throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        try {
            catalogService.addCategory(new Categories(0, bid, categoryName, true));
            flash.addFlashAttribute("success", "Category added.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id, @RequestParam String categoryName,
            @RequestParam(defaultValue = "true") boolean active,
            HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        if (categoryName == null || categoryName.isBlank()) {
            flash.addFlashAttribute("error", "Category name cannot be blank.");
        } else if (categoriesDAO.updateCategory(new Categories(id, bid, categoryName.trim(), active))) {
            flash.addFlashAttribute("success", "Category updated.");
        } else {
            flash.addFlashAttribute("error", "Category not found.");
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/active")
    public String setActive(@PathVariable long id, @RequestParam boolean active,
            HttpSession session, RedirectAttributes flash) {
        long bid = CurrentUser.get(session).getBusinessId();
        if (categoriesDAO.setCategoryActive(id, bid, active)) {
            flash.addFlashAttribute("success", active ? "Category activated." : "Category deactivated.");
        } else {
            flash.addFlashAttribute("error", "Category not found.");
        }
        return "redirect:/admin/categories";
    }
}
