package com.inventory.negosyopro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.inventory.negosyopro.dao.*;
import com.inventory.negosyopro.service.*;

@Configuration
public class BeanConfig {

    // ---------- services ----------
    @Bean
    public AuthService authService() {
        return new AuthService();
    }

    @Bean
    public SetupService setupService() {
        return new SetupService();
    }

    @Bean
    public UserService userService() {
        return new UserService();
    }

    @Bean
    public InventoryService inventoryService() {
        return new InventoryService();
    }

    @Bean
    public PurchaseOrderService purchaseOrderService() {
        return new PurchaseOrderService();
    }

    @Bean
    public SalesService salesService() {
        return new SalesService();
    }

    @Bean
    public AiReportService aiReportService() {
        return new AiReportService();
    }

    @Bean
    public CatalogService catalogService() {
        return new CatalogService();
    }

    // ---------- DAOs the controllers read directly ----------
    @Bean
    public ProductsDAO productsDAO() {
        return new ProductsDAO();
    }

    @Bean
    public StockBatchesDAO stockBatchesDAO() {
        return new StockBatchesDAO();
    }

    @Bean
    public InventoryTransactionsDAO inventoryTransactionsDAO() {
        return new InventoryTransactionsDAO();
    }

    @Bean
    public ReportsDAO reportsDAO() {
        return new ReportsDAO();
    }

    @Bean
    public SalesDAO salesDAO() {
        return new SalesDAO();
    }

    @Bean
    public BusinessDAO businessDAO() {
        return new BusinessDAO();
    }

    @Bean
    public CategoriesDAO categoriesDAO() {
        return new CategoriesDAO();
    }

    @Bean
    public SuppliersDAO suppliersDAO() {
        return new SuppliersDAO();
    }

    @Bean
    public ProductSuppliersDAO productSuppliersDAO() {
        return new ProductSuppliersDAO();
    }

    @Bean
    public PaymentMethodsDAO paymentMethodsDAO() {
        return new PaymentMethodsDAO();
    }

    @Bean
    public DiscountsDAO discountsDAO() {
        return new DiscountsDAO();
    }

    @Bean
    public PurchaseOrdersDAO purchaseOrdersDAO() {
        return new PurchaseOrdersDAO();
    }

    @Bean
    public UsersDAO usersDAO() {
        return new UsersDAO();
    }

    @Bean
    public AiReportsDAO aiReportsDAO() {
        return new AiReportsDAO();
    }

    @Bean
    public TemplateDAO templateDAO() {
        return new TemplateDAO();
    }
}