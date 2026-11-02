package com.inventory.negosyopro.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

//shared pages by employee and owner/admin
import com.inventory.negosyopro.dao.CategoriesDAO;
import com.inventory.negosyopro.dao.DiscountsDAO;
import com.inventory.negosyopro.dao.PaymentMethodsDAO;
import com.inventory.negosyopro.dao.ProductSuppliersDAO;
import com.inventory.negosyopro.dao.ProductsDAO;
import com.inventory.negosyopro.dao.SuppliersDAO;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Categories;
import com.inventory.negosyopro.model.Discounts;
import com.inventory.negosyopro.model.PaymentMethods;
import com.inventory.negosyopro.model.ProductSuppliers;
import com.inventory.negosyopro.model.Products;
import com.inventory.negosyopro.model.Suppliers;

//   Wraps the DAO insert methods that need a Connection, so controllers never touch JDBC. Auto-commit is on, so each insert saves by itself.

public class CatalogService {

    private final ProductsDAO productsDAO = new ProductsDAO();
    private final CategoriesDAO categoriesDAO = new CategoriesDAO();
    private final SuppliersDAO suppliersDAO = new SuppliersDAO();
    private final ProductSuppliersDAO productSuppliersDAO = new ProductSuppliersDAO();
    private final PaymentMethodsDAO paymentMethodsDAO = new PaymentMethodsDAO();
    private final DiscountsDAO discountsDAO = new DiscountsDAO();

    public long addProduct(Products p) throws SQLException {
        if (p == null) {
            throw new IllegalArgumentException("Product is required.");
        }
        requireText(p.getProductName(), "Product name");
        requireNotNegative(p.getCostPrice(), "Cost price");
        requireNotNegative(p.getSellingPrice(), "Selling price");
        try (Connection conn = dbConnection.getConnection()) {
            return productsDAO.insertProduct(conn, p);
        }
    }

    public long addCategory(Categories c) throws SQLException {
        if (c == null) {
            throw new IllegalArgumentException("Category is required.");
        }
        requireText(c.getCategoryName(), "Category name");
        try (Connection conn = dbConnection.getConnection()) {
            return categoriesDAO.insertCategory(conn, c);
        }
    }

    public long addSupplier(Suppliers s) throws SQLException {
        if (s == null) {
            throw new IllegalArgumentException("Supplier is required.");
        }
        requireText(s.getSupplierName(), "Supplier name");
        try (Connection conn = dbConnection.getConnection()) {
            return suppliersDAO.insertSupplier(conn, s);
        }
    }

    public long addProductSupplier(ProductSuppliers ps, long businessId) throws SQLException {
        if (ps == null) {
            throw new IllegalArgumentException("Product supplier link is required.");
        }
        // The link table has no business_id, so confirm both rows belong
        // to this business before linking them.
        if (productsDAO.getProductById(ps.getProductId(), businessId, false) == null) {
            throw new IllegalArgumentException("Product not found.");
        }
        if (suppliersDAO.getSupplierById(ps.getSupplierId(), businessId, false) == null) {
            throw new IllegalArgumentException("Supplier not found.");
        }
        try (Connection conn = dbConnection.getConnection()) {
            return productSuppliersDAO.insertProductSupplier(conn, ps);
        }
    }

    public long addPaymentMethod(PaymentMethods m) throws SQLException {
        if (m == null) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        requireText(m.getMethodName(), "Payment method name");
        try (Connection conn = dbConnection.getConnection()) {
            return paymentMethodsDAO.insertPaymentMethod(conn, m);
        }
    }

    public long addDiscount(Discounts d) throws SQLException {
        if (d == null) {
            throw new IllegalArgumentException("Discount is required.");
        }
        requireText(d.getDiscountName(), "Discount name");
        try (Connection conn = dbConnection.getConnection()) {
            return discountsDAO.insertDiscount(conn, d);
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank.");
        }
    }

    private static void requireNotNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(label + " cannot be negative.");
        }
    }
}