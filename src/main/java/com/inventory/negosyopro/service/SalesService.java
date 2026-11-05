package com.inventory.negosyopro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.inventory.negosyopro.dao.BusinessDAO;
import com.inventory.negosyopro.dao.DiscountsDAO;
import com.inventory.negosyopro.dao.InventoryTransactionsDAO;
import com.inventory.negosyopro.dao.PaymentMethodsDAO;
import com.inventory.negosyopro.dao.ProductsDAO;
import com.inventory.negosyopro.dao.SalesDAO;
import com.inventory.negosyopro.dao.StockBatchesDAO;
import com.inventory.negosyopro.dao.UsersDAO;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Business;
import com.inventory.negosyopro.model.Discounts;
import com.inventory.negosyopro.model.InventoryTransactions;
import com.inventory.negosyopro.model.InventoryTransactions.TransactionType;
import com.inventory.negosyopro.model.PaymentMethods;
import com.inventory.negosyopro.model.Products;
import com.inventory.negosyopro.model.SaleItems;
import com.inventory.negosyopro.model.Sales;
import com.inventory.negosyopro.model.StockBatches;
import com.inventory.negosyopro.model.Users;

public class SalesService {
    private final SalesDAO salesDAO = new SalesDAO();
    private final ProductsDAO productsDAO = new ProductsDAO();
    private final BusinessDAO businessDAO = new BusinessDAO();
    private final DiscountsDAO discountsDAO = new DiscountsDAO();
    private final PaymentMethodsDAO paymentMethodsDAO = new PaymentMethodsDAO();
    private final UsersDAO usersDAO = new UsersDAO();
    private final StockBatchesDAO stockBatchesDAO = new StockBatchesDAO();
    private final InventoryTransactionsDAO transactionsDAO = new InventoryTransactionsDAO();
    private final InventoryService inventoryService = new InventoryService();

    private static final BigDecimal HUNDRED = new BigDecimal("100");

   
    public BigDecimal calculateTax(BigDecimal price, boolean taxable, Business business) {
        if (price == null || business == null || !business.isTaxEnabled() || !taxable) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal rate = business.getTaxRate();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal tax = business.isTaxInclusive()
                ? price.multiply(rate).divide(HUNDRED.add(rate), 2, RoundingMode.HALF_UP)
                : price.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return tax;
    }

    // What the POS cart shows before the customer pays. Changes nothing.
    public BigDecimal previewTotal(long businessId, Long discountId, List<SaleItems> items) {
        Business business = requireBusiness(businessId);
        BigDecimal percent = discountPercent(businessId, discountId);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : mergeQuantities(items).entrySet()) {
            Products product = productsDAO.getProductById(e.getKey(), businessId, true);
            if (product == null) {
                throw new IllegalArgumentException("Product " + e.getKey() + " does not exist.");
            }
            Line line = computeLine(product, e.getValue(), percent, business);
            subtotal = subtotal.add(line.subtotal);
            tax = tax.add(line.tax);
        }
        return totalOf(subtotal, tax, business);
    }

    public long completeSale(Sales sale, List<SaleItems> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("The cart is empty.");
        }
        long businessId = sale.getBusinessId();
        Business business = requireBusiness(businessId);

        Users cashier = usersDAO.getUserById(sale.getUserId());
        if (cashier == null || cashier.getBusinessId() != businessId || !cashier.isActive()) {
            throw new IllegalArgumentException("This user cannot make sales for this business.");
        }
        boolean paymentOk = false;
        for (PaymentMethods m : paymentMethodsDAO.getPaymentMethodsByBusiness(businessId, true)) {
            if (m.getPaymentMethodId() == sale.getPaymentMethodId()) {
                paymentOk = true;
            }
        }
        if (!paymentOk) {
            throw new IllegalArgumentException("That payment method does not exist or is turned off.");
        }
        BigDecimal percent = discountPercent(businessId, sale.getDiscountId());
        if (sale.getAmountPaid() == null) {
            throw new IllegalArgumentException("Enter the amount paid.");
        }

        Map<Long, Integer> quantities = mergeQuantities(items);

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Pass 1a: lock every product in a deterministic order (sorted by ID),
                // so two simultaneous checkouts that share products can't deadlock.
                // The same order matches InventoryService.deductStock, which also
                // takes the product row before any batch row.
                Map<Long, Products> lockedProducts = new HashMap<>();
                List<Long> sortedProductIds = new ArrayList<>(quantities.keySet());
                Collections.sort(sortedProductIds);
                for (Long pid : sortedProductIds) {
                    Products p = productsDAO.getProductForUpdate(conn, pid, businessId);
                    if (p == null || !p.isActive()) {
                        throw new IllegalArgumentException("Product " + pid
                                + " does not exist or is inactive.");
                    }
                    lockedProducts.put(pid, p);
                }

                // Pass 1b: price every line in cart order, so the sale_items keep
                // their original display order on the receipt.
                List<Line> lines = new ArrayList<>();
                BigDecimal grossSubtotal = BigDecimal.ZERO; // pre-discount; stored in sales.subtotal
                BigDecimal netSubtotal = BigDecimal.ZERO; // post-discount; used to compute total
                BigDecimal discountTotal = BigDecimal.ZERO;
                BigDecimal taxTotal = BigDecimal.ZERO;
                for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
                    Products product = lockedProducts.get(e.getKey());
                    Line line = computeLine(product, e.getValue(), percent, business);
                    lines.add(line);
                    grossSubtotal = grossSubtotal.add(
                            line.unitPrice.multiply(BigDecimal.valueOf(line.quantity)));
                    netSubtotal = netSubtotal.add(line.subtotal);
                    discountTotal = discountTotal.add(line.discount);
                    taxTotal = taxTotal.add(line.tax);
                }
                BigDecimal total = totalOf(netSubtotal, taxTotal, business);
                if (sale.getAmountPaid().compareTo(total) < 0) {
                    throw new IllegalArgumentException("Amount paid (" + sale.getAmountPaid()
                            + ") is less than the total (" + total + ").");
                }
                BigDecimal change = sale.getAmountPaid().subtract(total);

                // Pass 2: save the sale, its lines, and take the stock.
                String orderNumber = salesDAO.nextOrderNumber(conn, businessId);
                Sales header = new Sales(0, businessId, sale.getUserId(), orderNumber,
                        sale.getCustomerName(), sale.getCustomerContact(), grossSubtotal, discountTotal,
                        taxTotal, total, sale.getAmountPaid(), change, sale.getPaymentMethodId(),
                        sale.getDiscountId(), Sales.Status.COMPLETED, null, null);
                long saleId = salesDAO.insertSale(conn, header);

                for (Line line : lines) {
                    Products p = line.product;
                    salesDAO.insertSaleItem(conn, new SaleItems(0, saleId, p.getProductId(),
                            p.getProductName(), line.quantity, line.unitPrice, p.getCostPrice(),
                            line.discount, line.tax, line.subtotal));
                    if (p.isTrackInventory()) {
                        inventoryService.deductStock(conn, p, businessId, sale.getUserId(), line.quantity,
                                TransactionType.SALES, saleId, "Sale " + orderNumber, true);
                    }
                }
                conn.commit();
                return saleId;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public boolean cancelSale(long saleId, long userId, String reason) throws SQLException {
        Users user = usersDAO.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user.");
        }
        long businessId = user.getBusinessId();
        String why = (reason == null || reason.isBlank()) ? "Cancelled" : reason.trim();

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Locked read prevents a second simultaneous cancel from also
                // putting the stock back.
                Sales sale = salesDAO.getSaleByIdForUpdate(conn, saleId, businessId);
                if (sale == null || sale.getStatus() != Sales.Status.COMPLETED) {
                    conn.rollback();
                    return false;
                }

                List<InventoryTransactions> rows = transactionsDAO.getTransactionsByReference(conn,
                        TransactionType.SALES, saleId, businessId);

                rows.sort(Comparator
                        .comparingLong(InventoryTransactions::getProductId)
                        .thenComparingLong(t -> t.getBatchId() == null ? -1L : t.getBatchId()));

             
                Map<Long, Integer> runningStock = new HashMap<>();

                for (InventoryTransactions row : rows) {
                    int quantity = Math.abs(row.getQuantity());

                    // 1. Lock the product (only the first time we see it).
                    Integer running = runningStock.get(row.getProductId());
                    if (running == null) {
                        Products product = productsDAO.getProductForUpdate(conn, row.getProductId(), businessId);
                        if (product == null) {
                            throw new SQLException("Product " + row.getProductId()
                                    + " of this sale is missing.");
                        }
                        running = product.getCurrentStock();
                        runningStock.put(row.getProductId(), running);
                    }

                    // 2. Lock (and update) the batch, if this row took from a batch.
                    if (row.getBatchId() != null) {
                        StockBatches batch = stockBatchesDAO.getBatchById(conn, row.getBatchId(), businessId);
                        if (batch == null) {
                            throw new SQLException("Batch " + row.getBatchId()
                                    + " of this sale is missing.");
                        }
                        stockBatchesDAO.updateQuantityRemaining(conn, batch.getBatchId(), businessId,
                                batch.getQuantityRemaining() + quantity);
                    }

                    // 3. Put the quantity back on the product.
                    if (!productsDAO.updateCurrentStock(conn, row.getProductId(), businessId, quantity)) {
                        throw new SQLException("Could not put the stock back for product "
                                + row.getProductId());
                    }

                    // 4. Log the reversal.
                    transactionsDAO.insertTransaction(conn, new InventoryTransactions(0, row.getProductId(),
                            userId, row.getBatchId(), TransactionType.VOID, null, quantity, running,
                            running + quantity, saleId, "Void " + sale.getOrderNumber() + ": " + why));

                    // 5. Advance the running total for this product.
                    runningStock.put(row.getProductId(), running + quantity);
                }

                boolean updated = salesDAO.updateSaleStatus(conn, saleId, businessId,
                        Sales.Status.CANCELLED, why);
                if (!updated) {
                    throw new SQLException("Could not mark the sale as cancelled.");
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ---------------------------- helpers ----------------------------
    private static class Line {
        Products product;
        int quantity;
        BigDecimal unitPrice;
        BigDecimal discount;
        BigDecimal tax;
        BigDecimal subtotal;
    }

    private Line computeLine(Products product, int quantity, BigDecimal discountPercent, Business business) {
        Line line = new Line();
        line.product = product;
        line.quantity = quantity;
        line.unitPrice = product.getSellingPrice();
        BigDecimal gross = line.unitPrice.multiply(BigDecimal.valueOf(quantity));
        line.discount = gross.multiply(discountPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        line.subtotal = gross.subtract(line.discount).setScale(2, RoundingMode.HALF_UP);
        line.tax = calculateTax(line.subtotal, product.isTaxable(), business);
        return line;
    }

    private BigDecimal totalOf(BigDecimal subtotal, BigDecimal tax, Business business) {
        boolean taxOnTop = business.isTaxEnabled() && !business.isTaxInclusive();
        return (taxOnTop ? subtotal.add(tax) : subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    // The same product on two cart lines becomes one line.
    private Map<Long, Integer> mergeQuantities(List<SaleItems> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (SaleItems item : items) {
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Every quantity must be more than zero.");
            }
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private BigDecimal discountPercent(long businessId, Long discountId) {
        if (discountId == null) {
            return BigDecimal.ZERO;
        }
        for (Discounts d : discountsDAO.getDiscountsByBusiness(businessId, true)) {
            if (d.getDiscountId() == discountId) {
                return d.getDiscountPercent();
            }
        }
        throw new IllegalArgumentException("That discount does not exist or is turned off.");
    }

    private Business requireBusiness(long businessId) {
        Business business = businessDAO.getBusinessById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Unknown business.");
        }
        return business;
    }
}