package com.inventory.negosyopro;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import com.inventory.negosyopro.dao.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.*;
import com.inventory.negosyopro.service.PasswordUtil;

//terminal testing for the done functions
public class MainSample {

    // ---------- tiny test helpers ----------
    interface Test {
        void run() throws Exception;
    }

    static int passed = 0, failed = 0;

    static void check(String what, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("   [PASS] " + what);
        } else {
            failed++;
            System.out.println("   [FAIL] " + what);
        }
    }

    static void section(String title, Test t) {
        System.out.println("\n=== " + title + " ===");
        try {
            t.run();
        } catch (Exception e) {
            failed++;
            System.out.println("   [FAIL] section stopped: " + e);
            e.printStackTrace(System.out);
        }
    }

    // ---------- data shared between sections ----------
    static long templateId, businessId, ownerId, employeeId, categoryId, productId;
    static String ownerUsername = "owner" + System.currentTimeMillis() % 100000;

    public static void main(String[] args) {
        TemplateDAO templateDAO = new TemplateDAO();
        BusinessDAO businessDAO = new BusinessDAO();
        UsersDAO usersDAO = new UsersDAO();
        PaymentMethodsDAO paymentDAO = new PaymentMethodsDAO();
        DiscountsDAO discountDAO = new DiscountsDAO();
        CategoriesDAO categoryDAO = new CategoriesDAO();
        ProductsDAO productDAO = new ProductsDAO();

        System.out.println("NegosyoPro - backend test");

        // Preflight: stop right away if the database cannot be reached, so we do not
        // run 10 sections that all fail (and some DAOs hide errors and look like they
        // pass).
        try (Connection c = dbConnection.getConnection()) {
            if (c == null || c.isClosed())
                throw new IllegalStateException("connection is closed");
        } catch (Exception e) {
            System.out.println("\nCANNOT REACH THE DATABASE - test stopped.");
            System.out.println("  1. Open Laragon and click Start All (MySQL must be running).");
            System.out.println("  Reason: " + e.getMessage().split("\n")[0]);
            return;
        }
        System.out.println("Database connection OK.");

        // 1 ---------------------------------------------------------------
        section("1. Database connection", () -> {
            try (Connection c = dbConnection.getConnection()) {
                check("getConnection() returns an open connection", c != null && !c.isClosed());
            }
        });

        // 2 ---------------------------------------------------------------
        section("2. PasswordUtil (BCrypt)", () -> {
            String hash = PasswordUtil.hashPassword("secret123");
            check("hash is not the plain password", !hash.equals("secret123"));
            check("correct password accepted", PasswordUtil.checkPassword("secret123", hash));
            check("wrong password rejected", !PasswordUtil.checkPassword("wrong", hash));
            check("null password rejected", !PasswordUtil.checkPassword(null, hash));
        });

        // 3 ---------------------------------------------------------------
        section("3. TemplateDAO", () -> {
            List<Template> all = templateDAO.getAllTemplates();
            check("getAllTemplates() returns the 3 seeded templates", all.size() >= 3);
            check("getAllKeywords() returns keywords", !templateDAO.getAllKeywords().isEmpty());
            check("getTemplatesByKeywords([salon]) finds a template",
                    !templateDAO.getTemplatesByKeyword(List.of("salon")).isEmpty());
            templateId = all.get(0).getTemplateId();
            check("getTemplateById() finds template " + templateId,
                    templateDAO.getTemplateById(templateId) != null);
        });

        // 4 ---------------------------------------------------------------
        section("4. Sign-up transaction (business + owner + defaults, all or nothing)", () -> {
            Template t = templateDAO.getTemplateById(templateId);
            Business b = new Business(templateId, "Test Mini Mart", "123 Rizal St", "09170000000",
                    true, true, new BigDecimal("12.00"), "PHP", "₱", "Thank you!",
                    t.isUsesExpiry(), t.isUsesPurchaseOrders(), 30,
                    new BigDecimal("150.00"), new BigDecimal("20.00"));
            Users owner = new Users(0, 0, ownerUsername, PasswordUtil.hashPassword("secret123"),
                    "Juan", "Dela Cruz", Users.Role.ADMIN, true, true);

            try (Connection conn = dbConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    businessId = businessDAO.insertBusiness(conn, b);
                    owner.setBusinessId(businessId);
                    ownerId = usersDAO.insertUser(conn, owner);
                    paymentDAO.insertDefaults(conn, businessId);
                    discountDAO.insertDefaults(conn, businessId);
                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }
            check("business created (id " + businessId + ")", businessId > 0);
            check("owner created (id " + ownerId + ")", ownerId > 0);

            // Rollback demo: duplicate username must fail and leave NO new business behind.
            long before = businessDAO.getBusinessById(businessId + 1) == null ? 0 : 1;
            boolean failedAsExpected = false;
            try (Connection conn = dbConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    long badBiz = businessDAO.insertBusiness(conn, b);
                    Users dup = new Users(0, badBiz, ownerUsername, "x", "Dup", "User", Users.Role.ADMIN, true, true);
                    usersDAO.insertUser(conn, dup); // same username -> must throw
                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    failedAsExpected = true;
                }
            }
            check("duplicate username is rejected", failedAsExpected);
            check("rollback left no half-created business",
                    (businessDAO.getBusinessById(businessId + 1) == null ? 0 : 1) == before);
        });

        if (businessId == 0) {
            System.out.println("\nSign-up failed, so the remaining sections need a business and are skipped.");
            System.out.println("PASSED: " + passed + "   FAILED: " + failed);
            return;
        }

        // 5 ---------------------------------------------------------------
        section("5. BusinessDAO", () -> {
            Business b = businessDAO.getBusinessById(businessId);
            check("getBusinessById() finds it", b != null);
            b.setBusinessName("Test Mini Mart (renamed)");
            check("updateBusiness() returns true", businessDAO.updateBusiness(b));
            check("name was changed in the database",
                    businessDAO.getBusinessById(businessId).getBusinessName().equals("Test Mini Mart (renamed)"));
            check("updateTemplate() returns true",
                    businessDAO.updateTemplate(businessId, templateId, true, true));
            check("getBusinessById(0) returns null", businessDAO.getBusinessById(0) == null);
        });

        // 6 ---------------------------------------------------------------
        section("6. UsersDAO", () -> {
            check("usernameExists() true for the owner", usersDAO.usernameExists(ownerUsername));
            check("usernameExists() false for an unknown name", !usersDAO.usernameExists("nobody_" + ownerUsername));

            Users u = usersDAO.getUserByUsername(ownerUsername);
            check("getUserByUsername() finds the owner", u != null && u.getUserId() == ownerId);
            check("stored hash matches the password", PasswordUtil.checkPassword("secret123", u.getPasswordHash()));
            check("getUserById() finds the owner", usersDAO.getUserById(ownerId) != null);

            try (Connection conn = dbConnection.getConnection()) {
                Users emp = new Users(0, businessId, ownerUsername + "_emp", PasswordUtil.hashPassword("emp12345"),
                        "Maria", "Santos", Users.Role.EMPLOYEE, true, false);
                employeeId = usersDAO.insertUser(conn, emp);
            }
            check("employee added (id " + employeeId + ")", employeeId > 0);
            check("getUsersByBusiness() lists 2 users", usersDAO.getUsersByBusiness(businessId).size() == 2);

            Users e = usersDAO.getUserById(employeeId);
            Users edited = new Users(e.getUserId(), e.getBusinessId(), e.getUsername(), e.getPasswordHash(),
                    "Maria Clara", e.getLastName(), e.getRole(), e.isActive(), e.isOwner());
            check("updateUser() returns true", usersDAO.updateUser(edited));
            check("first name changed", usersDAO.getUserById(employeeId).getFirstName().equals("Maria Clara"));

            String newHash = PasswordUtil.hashPassword("newpass999");
            check("updatePasswordHash() returns true", usersDAO.updatePasswordHash(employeeId, newHash));
            check("new password works", PasswordUtil.checkPassword("newpass999",
                    usersDAO.getUserById(employeeId).getPasswordHash()));

            check("setUserActive(false) returns true", usersDAO.setUserActive(employeeId, false));
            check("employee is now inactive", !usersDAO.getUserById(employeeId).isActive());
            check("setUserActive(true) returns true", usersDAO.setUserActive(employeeId, true));
        });

        // 7 ---------------------------------------------------------------
        section("7. PaymentMethodDAO and DiscountDAO", () -> {
            List<PaymentMethods> pm = paymentDAO.getPaymentMethodsByBusiness(businessId, false);
            check("default payment methods were created (" + pm.size() + ")", pm.size() > 0);

            try (Connection conn = dbConnection.getConnection()) {
                long id = paymentDAO.insertPaymentMethod(conn, new PaymentMethods(0, businessId, "PayPal", true));
                check("insertPaymentMethod() returns an id", id > 0);
            }
            PaymentMethods first = paymentDAO.getPaymentMethodsByBusiness(businessId, false).get(0);
            check("setPaymentMethodActive(false) works",
                    paymentDAO.setPaymentMethodActive(first.getPaymentMethodId(), businessId, false));
            check("activeOnly=true hides the inactive one",
                    paymentDAO.getPaymentMethodsByBusiness(businessId, true).size() < paymentDAO
                            .getPaymentMethodsByBusiness(businessId, false).size());

            List<Discounts> ds = discountDAO.getDiscountsByBusiness(businessId, false);
            check("default discounts were created (" + ds.size() + ")", ds.size() > 0);
            check("setDiscountActive() works",
                    discountDAO.setDiscountActive(ds.get(0).getDiscountId(), businessId, false));
        });

        // 8 ---------------------------------------------------------------
        section("8. CategoryDAO", () -> {
            try (Connection conn = dbConnection.getConnection()) {
                categoryId = categoryDAO.insertCategory(conn, new Categories(0, businessId, "Beverages", true));
            }
            check("insertCategory() returns an id (" + categoryId + ")", categoryId > 0);
            check("getCategoriesByBusiness() lists it",
                    categoryDAO.getCategoriesByBusiness(businessId, true).size() == 1);
            Categories c = categoryDAO.getCategoriesByBusiness(businessId, true).get(0);
            check("setCategoryActive(false) works",
                    categoryDAO.setCategoryActive(c.getCategoryId(), businessId, false));
            check("inactive category hidden when activeOnly=true",
                    categoryDAO.getCategoriesByBusiness(businessId, true).isEmpty());
            categoryDAO.setCategoryActive(c.getCategoryId(), businessId, true);
        });

        // 9 ---------------------------------------------------------------
        section("9. ProductDAO", () -> {
            Products p = new Products(businessId, categoryId, "BEV-001", "Bottled Water 500ml",
                    new BigDecimal("8.00"), new BigDecimal("15.00"), "bottle",
                    true, true, true, 365, 24, 48);
            try (Connection conn = dbConnection.getConnection()) {
                productId = productDAO.insertProduct(conn, p);
            }
            check("insertProduct() returns an id (" + productId + ")", productId > 0);

            Products got = productDAO.getProductById(productId, businessId, false);
            check("getProductById() finds it, stock starts at 0", got != null && got.getCurrentStock() == 0);
            check("getProductByCode() finds it", productDAO.getProductByCode(businessId, "BEV-001", false) != null);
            check("getProductsByBusiness() lists 1", productDAO.getProductsByBusiness(businessId, false).size() == 1);
            check("getProductsByCategory() lists 1",
                    productDAO.getProductByCateg(businessId, categoryId, false).size() == 1);
            check("searchProducts(\"water\") finds it", productDAO.searchProducts(businessId, "water").size() == 1);
            check("searchProducts(\"zzz\") finds nothing", productDAO.searchProducts(businessId, "zzz").isEmpty());

            Products edited = new Products(got.getProductId(), businessId, categoryId, "BEV-001",
                    "Bottled Water 500ml (new label)", new BigDecimal("8.50"), new BigDecimal("16.00"), "bottle",
                    true, true, true, 365, got.getCurrentStock(), 24, 48, true);
            check("updateProduct() returns true", productDAO.updateProduct(edited));
            check("price changed", productDAO.getProductById(productId, businessId, false)
                    .getSellingPrice().compareTo(new BigDecimal("16.00")) == 0);

            // signature: (productId, businessId, reorderLevel, reorderQuantity)
            check("updateReorderSettings() works", productDAO.updateReorderSettings(productId, businessId, 30, 60));
            Products rp = productDAO.getProductById(productId, businessId, false);
            check("reorder level saved as 30", rp.getReorderLevel() == 30);
            check("reorder quantity saved as 60", rp.getReorderQuantity() == 60);

            // stock 0 <= reorder level 30 -> should be listed as low stock
            check("getLowStockProducts() lists it (stock 0 <= level 30)",
                    productDAO.getLowStockProducts(businessId).size() == 1);

            try (Connection conn = dbConnection.getConnection()) {
                check("updateCurrentStock(+100) works",
                        productDAO.updateCurrentStock(conn, productId, businessId, 100));
            }
            check("stock is now 100", productDAO.getProductById(productId, businessId, false).getCurrentStock() == 100);
            check("no longer low stock", productDAO.getLowStockProducts(businessId).isEmpty());

            // Only valid if your updateCurrentStock has the "AND current_stock + ? >= 0"
            // check.
            try (Connection conn = dbConnection.getConnection()) {
                check("cannot remove more stock than exists",
                        !productDAO.updateCurrentStock(conn, productId, businessId, -500));
            }
            check("stock is still 100",
                    productDAO.getProductById(productId, businessId, false).getCurrentStock() == 100);

            check("setProductActive(false) works", productDAO.setProductActive(productId, businessId, false));
            check("inactive product hidden by activeOnly=true",
                    productDAO.getProductsByBusiness(businessId, true).isEmpty());
        });

        // 10 --------------------------------------------------------------
        section("10. Business isolation (one business must never see another's rows)", () -> {
            check("business 0 has no products", productDAO.getProductsByBusiness(0, false).isEmpty());
            check("a product id from another business is not returned",
                    productDAO.getProductById(productId, businessId + 999, false) == null);
        });

        System.out.println("\n==============================");
        System.out.println("PASSED: " + passed + "   FAILED: " + failed);
        System.out.println("==============================");
    }
}