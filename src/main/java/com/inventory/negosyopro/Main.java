package com.inventory.negosyopro;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import com.inventory.negosyopro.dao.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.*;
import com.inventory.negosyopro.service.*;

// Terminal menu for testing every feature. Logic lives in the services and DAOs; this class only
// asks questions and prints answers. Every call uses currentUser's business, so one business
// can never see another business's rows.
public class Main {
    private static final Scanner in = new Scanner(System.in);

    private static final TemplateDAO templateDAO = new TemplateDAO();
    private static final BusinessDAO businessDAO = new BusinessDAO();
    private static final UsersDAO usersDAO = new UsersDAO();
    private static final CategoriesDAO categoriesDAO = new CategoriesDAO();
    private static final ProductsDAO productsDAO = new ProductsDAO();
    private static final StockBatchesDAO batchesDAO = new StockBatchesDAO();
    private static final InventoryTransactionsDAO transactionsDAO = new InventoryTransactionsDAO();
    private static final SuppliersDAO suppliersDAO = new SuppliersDAO();
    private static final ProductSuppliersDAO productSuppliersDAO = new ProductSuppliersDAO();
    private static final PurchaseOrdersDAO purchaseOrdersDAO = new PurchaseOrdersDAO();
    private static final PaymentMethodsDAO paymentMethodsDAO = new PaymentMethodsDAO();
    private static final DiscountsDAO discountsDAO = new DiscountsDAO();
    private static final SalesDAO salesDAO = new SalesDAO();
    private static final ReportsDAO reportsDAO = new ReportsDAO();
    private static final AiReportsDAO aiReportsDAO = new AiReportsDAO();

    private static final AuthService authService = new AuthService();
    private static final SetupService setupService = new SetupService();
    private static final UserService userService = new UserService();
    private static final InventoryService inventoryService = new InventoryService();
    private static final PurchaseOrderService purchaseOrderService = new PurchaseOrderService();
    private static final SalesService salesService = new SalesService();
    private static final AiReportService aiReportService = new AiReportService();

    private static Users currentUser = null;

    public static void main(String[] args) {
        while (true) {
            try {
                if (currentUser == null) {
                    if (!startMenu()) {
                        System.out.println("Goodbye.");
                        return;
                    }
                } else {
                    mainMenu();
                }
            } catch (Exception e) { // a bad input or a failed rule never closes the program
                System.out.println("\n! " + e.getMessage());
            }
        }
    }

    // =====================================================================
    // Start: sign up / login
    // =====================================================================
    private static boolean startMenu() throws Exception {
        int c = menu("NegosyoPro", "Sign up (new business)", "Login");
        if (c == 0) {
            return false;
        }
        if (c == 1) {
            signUp();
        } else if (c == 2) {
            String username = ask("Username");
            String password = ask("Password");
            Users user = authService.login(username, password); // may throw if deactivated
            if (user == null) {
                System.out.println("Wrong username or password.");
            } else {
                currentUser = user;
                System.out.println("Welcome, " + user.getFirstName() + "!");
            }
        }
        return true;
    }

    private static void signUp() throws Exception {
        List<Template> templates = templateDAO.getAllTemplates();
        if (templates.isEmpty()) {
            System.out.println("No business templates found. Run the templates part of the sample data first.");
            return;
        }
        System.out.println("\nChoose what kind of business you run:");
        for (Template t : templates) {
            System.out.println("  [" + t.getTemplateId() + "] " + t.getTemplateName() + " - " + t.getDescription());
        }
        long templateId = askLong("Template id");

        String name = ask("Business name");
        String address = ask("Address");
        String contact = ask("Contact number");
        boolean taxEnabled = askYes("Charge VAT/tax?");
        boolean taxInclusive = taxEnabled && askYes("Do your prices already include the tax?");
        BigDecimal taxRate = taxEnabled ? askDecimal("Tax rate in percent (e.g. 12)") : BigDecimal.ZERO;
        String code = askOr("Currency code", "PHP");
        String symbol = askOr("Currency symbol", "₱");
        String footer = askOr("Receipt footer", "Thank you!");
        int alertDays = (int) askLongOr("Expiry alert days", 7);
        BigDecimal orderingCost = askDecimalOr("Ordering cost per order (0 to skip restock suggestions)", BigDecimal.ZERO);
        BigDecimal holdingCost = askDecimalOr("Holding cost percent per year (0 to skip)", BigDecimal.ZERO);

        // The two template flags are filled in by SetupService.
        Business business = new Business(templateId, name, address, contact, taxEnabled, taxInclusive, taxRate,
                code, symbol, footer, false, false, alertDays, orderingCost, holdingCost);

        System.out.println("\nOwner account:");
        String first = ask("First name");
        String last = ask("Last name");
        String username = ask("Username");
        String password = ask("Password");
        Users owner = new Users(0, 0, username, null, first, last, Users.Role.ADMIN, true, true);

        long businessId = setupService.createBusinessWithOwner(business, owner, password);
        System.out.println("Business created (id " + businessId + "). You can log in now.");
    }

    // =====================================================================
    // Main menu
    // =====================================================================
    private static void mainMenu() throws Exception {
        int c = menu("Menu - " + currentUser.getFirstName() + " (" + currentUser.getRole() + ")",
                "Dashboard", "Products", "Stock", "Sales / POS", "Purchasing (suppliers, orders, restock)",
                "Reports", "Settings", "My account");
        switch (c) {
            case 0: currentUser = null; System.out.println("Logged out."); break;
            case 1: dashboard(); break;
            case 2: productsMenu(); break;
            case 3: stockMenu(); break;
            case 4: salesMenu(); break;
            case 5: purchasingMenu(); break;
            case 6: reportsMenu(); break;
            case 7: settingsMenu(); break;
            case 8: myAccount(); break;
            default: break;
        }
    }

    private static long bid() {
        return currentUser.getBusinessId();
    }

    // =====================================================================
    // Dashboard
    // =====================================================================
    private static void dashboard() throws Exception {
        Business b = businessDAO.getBusinessById(bid());
        LocalDate today = LocalDate.now();
        System.out.println("\n===== Dashboard: " + b.getBusinessName() + " =====");

        Map<LocalDate, BigDecimal> last7 = reportsDAO.getSalesTotalsByPeriod(bid(), today.minusDays(6), today);
        BigDecimal week = BigDecimal.ZERO;
        for (BigDecimal v : last7.values()) {
            week = week.add(v);
        }
        System.out.println("Sales today:      " + b.getCurrencySymbol()
                + last7.getOrDefault(today, BigDecimal.ZERO));
        System.out.println("Sales last 7 days: " + b.getCurrencySymbol() + week);
        System.out.println("Inventory value:  " + b.getCurrencySymbol() + reportsDAO.getInventoryValue(bid()));

        List<Products> low = productsDAO.getLowStockProducts(bid());
        System.out.println("\nLow stock (" + low.size() + "):");
        for (Products p : low) {
            System.out.println("  " + p.getProductCode() + "  " + p.getProductName() + "  stock "
                    + p.getCurrentStock() + " / reorder at " + p.getReorderLevel());
        }
        if (b.isUsesExpiry()) {
            List<StockBatches> expiring = batchesDAO.getExpiringBatches(bid(), b.getExpiryAlertDays());
            System.out.println("\nExpiring within " + b.getExpiryAlertDays() + " days: " + expiring.size());
            for (StockBatches s : expiring) {
                System.out.println("  batch " + s.getBatchId() + " product " + s.getProductId() + " qty "
                        + s.getQuantityRemaining() + " expires " + s.getExpirationDate());
            }
        }
        AiReports latest = aiReportsDAO.getLatestAiReport(bid(), "WEEKLY");
        System.out.println("\nLatest AI summary: " + (latest == null ? "none yet (Reports > AI report)" : "\n" + latest.getContent()));
        pause();
    }

    // =====================================================================
    // Products
    // =====================================================================
    private static void productsMenu() throws Exception {
        int c = menu("Products", "List all", "Search", "Add product", "Low stock", "Categories");
        if (c == 1) {
            printProducts(productsDAO.getProductsByBusiness(bid(), false));
        } else if (c == 2) {
            printProducts(productsDAO.searchProducts(bid(), ask("Search text")));
        } else if (c == 3) {
            addProduct();
        } else if (c == 4) {
            printProducts(productsDAO.getLowStockProducts(bid()));
        } else if (c == 5) {
            for (Categories cat : categoriesDAO.getCategoriesByBusiness(bid(), false)) {
                System.out.println("  [" + cat.getCategoryId() + "] " + cat.getCategoryName()
                        + (cat.isActive() ? "" : " (off)"));
            }
            if (askYes("Add a category?")) {
                try (Connection conn = dbConnection.getConnection()) {
                    long id = categoriesDAO.insertCategory(conn, new Categories(0, bid(), ask("Name"), true));
                    System.out.println("Category added (id " + id + ").");
                }
            }
        }
        if (c != 0) {
            pause();
        }
    }

    private static void addProduct() throws Exception {
        List<Categories> cats = categoriesDAO.getCategoriesByBusiness(bid(), true);
        if (cats.isEmpty()) {
            System.out.println("Add a category first (Products > Categories).");
            return;
        }
        for (Categories cat : cats) {
            System.out.println("  [" + cat.getCategoryId() + "] " + cat.getCategoryName());
        }
        long categoryId = askLong("Category id");
        String code = ask("Product code");
        if (productsDAO.getProductByCode(bid(), code, false) != null) {
            System.out.println("That product code already exists.");
            return;
        }
        String name = ask("Product name");
        BigDecimal cost = askDecimal("Cost price");
        BigDecimal price = askDecimal("Selling price");
        String unit = askOr("Unit", "pc");
        boolean taxable = askYes("Taxable?");
        boolean track = askYes("Track inventory?");
        Business b = businessDAO.getBusinessById(bid());
        boolean expiry = b.isUsesExpiry() && askYes("Does it expire?");
        Integer shelf = expiry ? (int) askLong("Shelf life in days") : null;
        int reorderLevel = (int) askLongOr("Reorder level", 0);
        int reorderQty = (int) askLongOr("Reorder quantity", 0);

        long id;
        try (Connection conn = dbConnection.getConnection()) {
            id = productsDAO.insertProduct(conn, new Products(bid(), categoryId, code, name, cost, price, unit,
                    taxable, track, expiry, shelf, reorderLevel, reorderQty));
        }
        System.out.println("Product added (id " + id + ").");
        int opening = (int) askLongOr("Opening stock (0 for none)", 0);
        if (opening > 0) { // goes through InventoryService so the batch and history rows are written too
            inventoryService.receiveStock(id, currentUser.getUserId(), opening, cost, null, null);
            System.out.println("Opening stock recorded.");
        }
    }

    private static void printProducts(List<Products> list) {
        System.out.println();
        if (list.isEmpty()) {
            System.out.println("(none)");
        }
        for (Products p : list) {
            System.out.printf("  #%d  %-10s %-28s cost %8s  price %8s  stock %4d%s%n", p.getProductId(),
                    p.getProductCode(), p.getProductName(), p.getCostPrice(), p.getSellingPrice(),
                    p.getCurrentStock(), p.isActive() ? "" : "  (inactive)");
        }
    }

    // =====================================================================
    // Stock
    // =====================================================================
    private static void stockMenu() throws Exception {
        int c = menu("Stock", "Restock a product", "Adjust / damage", "Stock history of a product",
                "Expiring batches", "Expired batches (write off)");
        Business b = businessDAO.getBusinessById(bid());
        if (c == 1) {
            long productId = askLong("Product id");
            int qty = (int) askLong("Quantity received");
            BigDecimal cost = askDecimalOr("Unit cost (blank = product cost)", null);
            LocalDate expiry = askDateOrNull("Expiry date yyyy-mm-dd (blank = none/default)");
            Long supplier = askLongOrNull("Supplier id (blank = none)");
            inventoryService.receiveStock(productId, currentUser.getUserId(), qty, cost, expiry, supplier);
            System.out.println("Stock added.");
        } else if (c == 2) {
            long productId = askLong("Product id");
            int change = (int) askLong("Quantity change (negative removes stock)");
            String type = askOr("Type (ADJUSTMENT or DAMAGE)", "ADJUSTMENT");
            inventoryService.adjustStock(productId, currentUser.getUserId(), change, type, ask("Reason"));
            System.out.println("Stock updated.");
        } else if (c == 3) {
            long productId = askLong("Product id");
            for (InventoryTransactions t : transactionsDAO.getTransactionsByProduct(productId, bid())) {
                System.out.printf("  %s  %-10s %+5d  %d -> %d  batch %s  %s%n", t.getTransactionDate(),
                        t.getTransactionType(), t.getQuantity(), t.getPreviousStock(), t.getNewStock(),
                        t.getBatchId(), t.getReason() == null ? "" : t.getReason());
            }
        } else if (c == 4) {
            for (StockBatches s : batchesDAO.getExpiringBatches(bid(), b.getExpiryAlertDays())) {
                System.out.println("  batch " + s.getBatchId() + "  product " + s.getProductId() + "  qty "
                        + s.getQuantityRemaining() + "  expires " + s.getExpirationDate());
            }
        } else if (c == 5) {
            List<StockBatches> expired = batchesDAO.getExpiredBatches(bid());
            for (StockBatches s : expired) {
                System.out.println("  batch " + s.getBatchId() + "  product " + s.getProductId() + "  qty "
                        + s.getQuantityRemaining() + "  expired " + s.getExpirationDate());
            }
            if (!expired.isEmpty() && askYes("Write off a batch?")) {
                inventoryService.writeOffExpired(askLong("Batch id"), currentUser.getUserId());
                System.out.println("Batch written off.");
            }
        }
        if (c != 0) {
            pause();
        }
    }

    // =====================================================================
    // Sales / POS
    // =====================================================================
    private static void salesMenu() throws Exception {
        int c = menu("Sales / POS", "New sale", "Sales history (last 30 days)", "View receipt", "Cancel a sale");
        if (c == 1) {
            newSale();
        } else if (c == 2) {
            for (Sales s : salesDAO.getSales(bid(), LocalDate.now().minusDays(30), LocalDate.now())) {
                System.out.printf("  #%d  %s  %s  total %s  %s%n", s.getSalesId(), s.getOrderNumber(),
                        s.getCreatedAt(), s.getTotalAmount(), s.getStatus());
            }
        } else if (c == 3) {
            printReceipt(askLong("Sale id"));
        } else if (c == 4) {
            boolean ok = salesService.cancelSale(askLong("Sale id"), currentUser.getUserId(), ask("Reason"));
            System.out.println(ok ? "Sale cancelled and stock put back." : "That sale cannot be cancelled.");
        }
        if (c != 0) {
            pause();
        }
    }

    private static void newSale() throws Exception {
        List<SaleItems> cart = new ArrayList<>();
        System.out.println("Add products. Blank code to finish.");
        while (true) {
            String code = ask("Product code (blank = done)");
            if (code.isEmpty()) {
                break;
            }
            Products p = productsDAO.getProductByCode(bid(), code, true);
            if (p == null) {
                System.out.println("  No active product with that code.");
                continue;
            }
            int qty = (int) askLongOr("Quantity for " + p.getProductName() + " @ " + p.getSellingPrice(), 1);
            cart.add(new SaleItems(0, 0, p.getProductId(), p.getProductName(), qty, p.getSellingPrice(),
                    p.getCostPrice(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        if (cart.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        Long discountId = null;
        List<Discounts> discounts = discountsDAO.getDiscountsByBusiness(bid(), true);
        if (!discounts.isEmpty()) {
            for (Discounts d : discounts) {
                System.out.println("  discount [" + d.getDiscountId() + "] " + d.getDiscountName() + " "
                        + d.getDiscountPercent() + "%" + (d.isRequiresId() ? " (ID required)" : ""));
            }
            discountId = askLongOrNull("Discount id (blank = none)");
        }
        BigDecimal total = salesService.previewTotal(bid(), discountId, cart);
        System.out.println("Total to pay: " + total);

        List<PaymentMethods> methods = paymentMethodsDAO.getPaymentMethodsByBusiness(bid(), true);
        for (PaymentMethods m : methods) {
            System.out.println("  payment [" + m.getPaymentMethodId() + "] " + m.getMethodName());
        }
        long methodId = askLong("Payment method id");
        BigDecimal paid = askDecimal("Amount paid");
        String customer = ask("Customer name (blank = none)");
        String contact = ask("Customer contact (blank = none)");

        Sales header = new Sales(0, bid(), currentUser.getUserId(), null, customer.isEmpty() ? null : customer,
                contact.isEmpty() ? null : contact, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, paid, BigDecimal.ZERO, methodId, discountId, Sales.Status.COMPLETED, null, null);
        long saleId = salesService.completeSale(header, cart);
        printReceipt(saleId);
    }

    private static void printReceipt(long saleId) {
        Sales s = salesDAO.getSaleById(saleId, bid());
        if (s == null) {
            System.out.println("No such sale.");
            return;
        }
        Business b = businessDAO.getBusinessById(bid());
        String cur = b.getCurrencySymbol();
        System.out.println("\n----------------------------------------");
        System.out.println("  " + b.getBusinessName());
        System.out.println("  " + s.getOrderNumber() + "   " + s.getCreatedAt());
        System.out.println("----------------------------------------");
        for (SaleItems i : salesDAO.getItemsBySale(saleId, bid())) {
            System.out.printf("  %-22s %3d x %8s = %9s%n", i.getProductName(), i.getQuantity(), i.getUnitPrice(),
                    i.getSubtotal());
        }
        System.out.println("----------------------------------------");
        System.out.println("  Subtotal:  " + cur + s.getSubtotal());
        System.out.println("  Discount:  " + cur + s.getDiscountAmount() + "  (already taken off the lines)");
        System.out.println("  Tax:       " + cur + s.getTaxAmount());
        System.out.println("  TOTAL:     " + cur + s.getTotalAmount());
        System.out.println("  Paid:      " + cur + s.getAmountPaid());
        System.out.println("  Change:    " + cur + s.getChangeAmount());
        System.out.println("  Status:    " + s.getStatus());
        System.out.println("----------------------------------------");
        System.out.println("  " + (b.getReceiptFooter() == null ? "" : b.getReceiptFooter()));
    }

    // =====================================================================
    // Purchasing
    // =====================================================================
    private static void purchasingMenu() throws Exception {
        int c = menu("Purchasing", "Suppliers", "Add supplier", "Link product to supplier", "Purchase orders",
                "New purchase order", "Mark order as ordered", "Receive an order", "Cancel an order",
                "Restock suggestions");
        if (c == 1) {
            for (Suppliers s : suppliersDAO.getSuppliersByBusiness(bid(), false)) {
                System.out.println("  [" + s.getSupplierId() + "] " + s.getSupplierName()
                        + (s.isActive() ? "" : " (inactive)"));
            }
        } else if (c == 2) {
            try (Connection conn = dbConnection.getConnection()) {
                long id = suppliersDAO.insertSupplier(conn, new Suppliers(0, bid(), ask("Supplier name"),
                        ask("Contact person"), ask("Contact number"), ask("Email"), ask("Address"), true, null));
                System.out.println("Supplier added (id " + id + ").");
            }
        } else if (c == 3) {
            long productId = askLong("Product id");
            long supplierId = askLong("Supplier id");
            if (productsDAO.getProductById(productId, bid(), false) == null
                    || suppliersDAO.getSupplierById(supplierId, bid(), false) == null) {
                System.out.println("Unknown product or supplier.");
            } else {
                String supplierCode = ask("Supplier's product code");
                BigDecimal supplierCost = askDecimal("Supplier cost");
                Long lead = askLongOrNull("Lead time in days (blank = unknown)");
                try (Connection conn = dbConnection.getConnection()) {
                    productSuppliersDAO.insertProductSupplier(conn, new ProductSuppliers(0, productId, supplierId,
                            supplierCode, supplierCost, lead == null ? null : Integer.valueOf(lead.intValue()),
                            false));
                }
                if (askYes("Make this the preferred supplier?")) {
                    productSuppliersDAO.setPreferredSupplier(productId, supplierId, bid(), true);
                }
                System.out.println("Linked.");
            }
        } else if (c == 4) {
            for (PurchaseOrders po : purchaseOrdersDAO.getPurchaseOrders(bid(), null)) {
                System.out.println("  #" + po.getPurchaseOrderId() + "  " + po.getPoNumber() + "  supplier "
                        + po.getSupplierId() + "  " + po.getStatus());
                for (PurchaseOrderItems i : purchaseOrdersDAO.getItemsByPurchaseOrder(po.getPurchaseOrderId(), bid())) {
                    System.out.println("      item " + i.getPurchaseOrderItemId() + ": product " + i.getProductId()
                            + "  ordered " + i.getQuantityOrdered() + "  received " + i.getQuantityReceived()
                            + "  @ " + i.getUnitCost());
                }
            }
        } else if (c == 5) {
            newPurchaseOrder();
        } else if (c == 6) {
            System.out.println(purchaseOrderService.markOrdered(askLong("Order id"), bid()) ? "Marked as ordered."
                    : "Only a DRAFT order can be marked as ordered.");
        } else if (c == 7) {
            receiveOrder();
        } else if (c == 8) {
            System.out.println(purchaseOrderService.cancelPurchaseOrder(askLong("Order id"), bid()) ? "Cancelled."
                    : "That order cannot be cancelled.");
        } else if (c == 9) {
            List<RestockSuggestions> list = purchaseOrderService.getRestockSuggestions(bid());
            if (list.isEmpty()) {
                System.out.println("No suggestions (nothing is low, there is no sales history yet, or ordering/holding cost is 0).");
            }
            for (RestockSuggestions r : list) {
                System.out.println("  " + r.getProduct().getProductName() + ": stock " + r.getProduct().getCurrentStock()
                        + ", reorder point " + r.getReorderPoint() + ", order " + r.getEconomicOrderQuantity()
                        + " (EOQ), preferred supplier " + r.getPreferredSupplierId());
            }
        }
        if (c != 0) {
            pause();
        }
    }

    private static void newPurchaseOrder() throws Exception {
        long supplierId = askLong("Supplier id");
        List<PurchaseOrderItems> items = new ArrayList<>();
        while (true) {
            String code = ask("Product code (blank = done)");
            if (code.isEmpty()) {
                break;
            }
            Products p = productsDAO.getProductByCode(bid(), code, true);
            if (p == null) {
                System.out.println("  No active product with that code.");
                continue;
            }
            int qty = (int) askLong("Quantity to order");
            BigDecimal cost = askDecimalOr("Unit cost (blank = " + p.getCostPrice() + ")", null);
            items.add(new PurchaseOrderItems(0, 0, p.getProductId(), qty, 0, cost));
        }
        PurchaseOrders header = new PurchaseOrders(0, bid(), supplierId, currentUser.getUserId(), null,
                PurchaseOrders.Status.DRAFT, null, askDateOrNull("Expected arrival yyyy-mm-dd (blank = none)"),
                null, ask("Notes"), null);
        long id = purchaseOrderService.createPurchaseOrder(header, items);
        System.out.println("Draft purchase order created (id " + id + ").");
    }

    private static void receiveOrder() throws Exception {
        long orderId = askLong("Order id");
        List<ReceivedItems> received = new ArrayList<>();
        for (PurchaseOrderItems i : purchaseOrdersDAO.getItemsByPurchaseOrder(orderId, bid())) {
            Products p = productsDAO.getProductById(i.getProductId(), bid(), false);
            int qty = (int) askLongOr("Received for " + p.getProductName() + " (ordered " + i.getQuantityOrdered() + ")",
                    i.getQuantityOrdered());
            LocalDate expiry = null;
            if (qty > 0 && p.isTracksExpiry()) {
                expiry = askDateOrNull("Expiry date yyyy-mm-dd (blank = today + shelf life)");
            }
            received.add(new ReceivedItems(i.getPurchaseOrderItemId(), qty, expiry));
        }
        purchaseOrderService.receivePurchaseOrder(orderId, currentUser.getUserId(), received);
        System.out.println("Order received and stock updated.");
    }

    // =====================================================================
    // Reports
    // =====================================================================
    private static void reportsMenu() throws Exception {
        int c = menu("Reports", "Sales per day", "Top selling products", "Profit", "Inventory value",
                "AI report (generate)", "AI report (last saved)");
        if (c == 0) {
            return;
        }
        Business b = businessDAO.getBusinessById(bid());
        String cur = b.getCurrencySymbol();
        LocalDate from = null;
        LocalDate to = null;
        if (c >= 1 && c <= 3 || c == 5) {
            to = askDateOr("To date yyyy-mm-dd (blank = today)", LocalDate.now());
            from = askDateOr("From date yyyy-mm-dd (blank = 7 days before)", to.minusDays(6));
        }

        if (c == 1) {
            for (Map.Entry<LocalDate, BigDecimal> e : reportsDAO.getSalesTotalsByPeriod(bid(), from, to).entrySet()) {
                System.out.println("  " + e.getKey() + "  " + cur + e.getValue());
            }
        } else if (c == 2) {
            for (ProductSalesSummaries p : reportsDAO.getTopSellingProducts(bid(), from, to, 10)) {
                System.out.println("  " + p.getProductName() + "  qty " + p.getQuantity() + "  revenue " + cur + p.getRevenue());
            }
        } else if (c == 3) {
            System.out.println("  Profit: " + cur + reportsDAO.getProfitByPeriod(bid(), from, to));
        } else if (c == 4) {
            System.out.println("  Inventory value at cost: " + cur + reportsDAO.getInventoryValue(bid()));
        } else if (c == 5) {
            System.out.println("Asking the AI (this can take a minute)...");
            AiReports r = aiReportService.generateReport(bid(), currentUser.getUserId(),
                    askOr("Report type", "WEEKLY"), from, to);
            System.out.println("\n===== AI REPORT (saved as #" + r.getAiReportId() + ") =====\n" + r.getContent());
        } else if (c == 6) {
            AiReports r = aiReportsDAO.getLatestAiReport(bid(), askOr("Report type", "WEEKLY"));
            System.out.println(r == null ? "None saved yet." : "\n" + r.getPeriodStart() + " to " + r.getPeriodEnd()
                    + "  (saved " + r.getCreatedAt() + ")\n\n" + r.getContent());
        }
        pause();
    }

    // =====================================================================
    // Settings and account
    // =====================================================================
    private static void settingsMenu() throws Exception {
        int c = menu("Settings", "Business info", "Payment methods", "Add payment method", "Discounts",
                "Add discount", "Users", "Add user", "Deactivate / activate user");
        if (c == 1) {
            Business b = businessDAO.getBusinessById(bid());
            System.out.println("  " + b.getBusinessName() + " | " + b.getBusinessAddress() + " | " + b.getBusinessContact());
            System.out.println("  tax " + (b.isTaxEnabled() ? b.getTaxRate() + "%" + (b.isTaxInclusive() ? " inclusive" : " exclusive") : "off")
                    + " | uses expiry " + b.isUsesExpiry() + " | uses purchase orders " + b.isUsesPurchaseOrders());
        } else if (c == 2) {
            for (PaymentMethods m : paymentMethodsDAO.getPaymentMethodsByBusiness(bid(), false)) {
                System.out.println("  [" + m.getPaymentMethodId() + "] " + m.getMethodName() + (m.isActive() ? "" : " (off)"));
            }
        } else if (c == 3) {
            try (Connection conn = dbConnection.getConnection()) {
                paymentMethodsDAO.insertPaymentMethod(conn, new PaymentMethods(0, bid(), ask("Method name"), true));
            }
            System.out.println("Added.");
        } else if (c == 4) {
            for (Discounts d : discountsDAO.getDiscountsByBusiness(bid(), false)) {
                System.out.println("  [" + d.getDiscountId() + "] " + d.getDiscountName() + " " + d.getDiscountPercent()
                        + "%" + (d.isActive() ? "" : " (off)"));
            }
        } else if (c == 5) {
            try (Connection conn = dbConnection.getConnection()) {
                discountsDAO.insertDiscount(conn, new Discounts(0, bid(), ask("Discount name"),
                        askDecimal("Percent (e.g. 10)"), askYes("Requires an ID?"), true));
            }
            System.out.println("Added.");
        } else if (c == 6) {
            for (Users u : usersDAO.getUsersByBusiness(bid())) {
                System.out.println("  [" + u.getUserId() + "] " + u.getUsername() + " - " + u.getFirstName() + " "
                        + u.getLastName() + " " + u.getRole() + (u.isOwner() ? " (owner)" : "") + (u.isActive() ? "" : " (inactive)"));
            }
        } else if (c == 7) {
            if (currentUser.getRole() != Users.Role.ADMIN) {
                System.out.println("Only an admin can add users.");
            } else {
                Users.Role role = askYes("Make this user an ADMIN? (no = EMPLOYEE)") ? Users.Role.ADMIN : Users.Role.EMPLOYEE;
                Users u = new Users(0, bid(), ask("Username"), null, ask("First name"), ask("Last name"), role, true, false);
                long id = userService.addUser(bid(), u, ask("Password"));
                System.out.println("User added (id " + id + ").");
            }
        } else if (c == 8) {
            long id = askLong("User id");
            boolean ok = askYes("Deactivate? (no = activate)") ? userService.deactivateUser(id, bid())
                    : userService.activateUser(id, bid());
            System.out.println(ok ? "Done." : "No such user in your business.");
        }
        if (c != 0) {
            pause();
        }
    }

    private static void myAccount() throws Exception {
        System.out.println("\n" + currentUser.getFirstName() + " " + currentUser.getLastName() + " (" + currentUser.getUsername() + ")");
        if (askYes("Change password?")) {
            boolean ok = authService.changePassword(currentUser.getUserId(), ask("Old password"), ask("New password"));
            System.out.println(ok ? "Password changed." : "The old password is wrong.");
        }
    }

    // =====================================================================
    // Small input helpers
    // =====================================================================
    private static int menu(String title, String... options) {
        System.out.println("\n= = = = = " + title + " = = = = =");
        for (int i = 0; i < options.length; i++) {
            System.out.println("[" + (i + 1) + "] " + options[i]);
        }
        System.out.println("[0] " + (title.equals("NegosyoPro") ? "Exit" : (title.startsWith("Menu") ? "Logout" : "Back")));
        while (true) {
            String s = ask("Choose");
            try {
                int n = Integer.parseInt(s);
                if (n >= 0 && n <= options.length) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Enter a number from the list.");
        }
    }

    private static String ask(String label) {
        System.out.print(label + ": ");
        return in.nextLine().trim();
    }

    private static String askOr(String label, String fallback) {
        String s = ask(label + " [" + fallback + "]");
        return s.isEmpty() ? fallback : s;
    }

    private static boolean askYes(String label) {
        String s = ask(label + " (y/n)");
        return s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes");
    }

    private static long askLong(String label) {
        while (true) {
            try {
                return Long.parseLong(ask(label));
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number.");
            }
        }
    }

    private static long askLongOr(String label, long fallback) {
        while (true) {
            String s = ask(label + " [" + fallback + "]");
            if (s.isEmpty()) {
                return fallback;
            }
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number.");
            }
        }
    }

    private static Long askLongOrNull(String label) {
        while (true) {
            String s = ask(label);
            if (s.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number, or leave it blank.");
            }
        }
    }

    private static BigDecimal askDecimal(String label) {
        while (true) {
            try {
                return new BigDecimal(ask(label));
            } catch (NumberFormatException e) {
                System.out.println("Enter a number like 12.50.");
            }
        }
    }

    private static BigDecimal askDecimalOr(String label, BigDecimal fallback) {
        while (true) {
            String s = ask(label);
            if (s.isEmpty()) {
                return fallback;
            }
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                System.out.println("Enter a number like 12.50, or leave it blank.");
            }
        }
    }

    private static LocalDate askDateOrNull(String label) {
        return askDateOr(label, null);
    }

    private static LocalDate askDateOr(String label, LocalDate fallback) {
        while (true) {
            String s = ask(label);
            if (s.isEmpty()) {
                return fallback;
            }
            try {
                return LocalDate.parse(s);
            } catch (Exception e) {
                System.out.println("Use the format yyyy-mm-dd, for example 2026-12-31.");
            }
        }
    }

    private static void pause() {
        ask("\nPress Enter to go back");
    }
}
