package com.inventory.negosyopro.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import com.inventory.negosyopro.dao.InventoryTransactionsDAO;
import com.inventory.negosyopro.dao.ProductsDAO;
import com.inventory.negosyopro.dao.StockBatchesDAO;
import com.inventory.negosyopro.dao.UsersDAO;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.InventoryTransactions;
import com.inventory.negosyopro.model.InventoryTransactions.TransactionType;
import com.inventory.negosyopro.model.Products;
import com.inventory.negosyopro.model.StockBatches;
import com.inventory.negosyopro.model.Users;

// Every stock change goes through here, so current_stock, the batches and the
// inventory_transactions history always change together in one transaction.
public class InventoryService {
    private final UsersDAO usersDAO = new UsersDAO();
    private final ProductsDAO productsDAO = new ProductsDAO();
    private final StockBatchesDAO stockBatchesDAO = new StockBatchesDAO();
    private final InventoryTransactionsDAO transactionsDAO = new InventoryTransactionsDAO();

    // ------------------------------------------------------------------
    // receiveStock: the Stock overlay "Restock" button.
    // The business is taken from the user, so one business cannot touch another's products.
    // ------------------------------------------------------------------
    public void receiveStock(long productId, long userId, int quantity, BigDecimal unitCost,
            LocalDate expirationDate, Long supplierId) throws SQLException {
        long businessId = businessOf(userId);
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                receiveStock(conn, businessId, productId, userId, quantity, unitCost, expirationDate,
                        supplierId, null, null, "Restock");
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Same work on a connection that is already inside a transaction
    // (PurchaseOrderService.receivePurchaseOrder calls this for each line).
    void receiveStock(Connection conn, long businessId, long productId, long userId, int quantity,
            BigDecimal unitCost, LocalDate expirationDate, Long supplierId, Long purchaseOrderItemId,
            Long referenceId, String reason) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be more than zero.");
        }
        Products product = requireProduct(conn, productId, businessId);

        Long batchId = null;
        if (product.isTracksExpiry()) { // a batch is only kept for products that expire
            LocalDate expiry = expirationDate;
            if (expiry == null && product.getShelfLifeDays() != null) {
                expiry = LocalDate.now().plusDays(product.getShelfLifeDays());
            }
            BigDecimal cost = unitCost != null ? unitCost : product.getCostPrice();
            batchId = stockBatchesDAO.insertBatch(conn, new StockBatches(0, businessId, productId, supplierId,
                    purchaseOrderItemId, newBatchNumber("B", productId), quantity, quantity, cost, expiry,
                    LocalDateTime.now()));
        }

        int previous = product.getCurrentStock();
        if (!productsDAO.updateCurrentStock(conn, productId, businessId, quantity)) {
            throw new SQLException("Could not update the stock of product " + productId + ".");
        }
        logTransaction(conn, productId, userId, batchId, TransactionType.RESTOCK, quantity, previous,
                previous + quantity, referenceId, reason);
    }

    // ------------------------------------------------------------------
    // adjustStock: manual correction. type is "ADJUSTMENT" or "DAMAGE".
    // A negative quantityChange removes stock (earliest-expiring batch first).
    // ------------------------------------------------------------------
    public void adjustStock(long productId, long userId, int quantityChange, String type, String reason)
            throws SQLException {
        TransactionType transactionType = parseAdjustType(type);
        if (quantityChange == 0) {
            throw new IllegalArgumentException("The change cannot be zero.");
        }
        long businessId = businessOf(userId);

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Products product = requireProduct(conn, productId, businessId);

                if (quantityChange < 0) {
                    deductStock(conn, product, businessId, userId, -quantityChange, transactionType, null,
                            reason, false);
                } else {
                    Long batchId = null;
                    if (product.isTracksExpiry()) {
                        // keeps the batches adding up to current_stock; no expiry date is known
                        batchId = stockBatchesDAO.insertBatch(conn, new StockBatches(0, businessId, productId,
                                null, null, newBatchNumber("ADJ", productId), quantityChange, quantityChange,
                                product.getCostPrice(), null, LocalDateTime.now()));
                    }
                    int previous = product.getCurrentStock();
                    if (!productsDAO.updateCurrentStock(conn, productId, businessId, quantityChange)) {
                        throw new SQLException("Could not update the stock of product " + productId + ".");
                    }
                    logTransaction(conn, productId, userId, batchId, transactionType, quantityChange, previous,
                            previous + quantityChange, null, reason);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------
    // writeOffExpired: removes what is left of an expired batch.
    // ------------------------------------------------------------------
    public void writeOffExpired(long batchId, long userId) throws SQLException {
        long businessId = businessOf(userId);

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                StockBatches batch = stockBatchesDAO.getBatchById(conn, batchId, businessId);
                if (batch == null) {
                    throw new IllegalArgumentException("That batch does not exist.");
                }
                if (batch.getExpirationDate() == null || !batch.getExpirationDate().isBefore(LocalDate.now())) {
                    throw new IllegalStateException("That batch has not expired yet.");
                }
                int remaining = batch.getQuantityRemaining();
                if (remaining > 0) {
                    Products product = requireProduct(conn, batch.getProductId(), businessId);
                    int previous = product.getCurrentStock();

                    if (!productsDAO.updateCurrentStock(conn, product.getProductId(), businessId, -remaining)) {
                        throw new IllegalStateException("Stock is lower than the expired batch quantity.");
                    }
                    stockBatchesDAO.updateQuantityRemaining(conn, batchId, businessId, 0);
                    logTransaction(conn, product.getProductId(), userId, batchId, TransactionType.EXPIRED,
                            -remaining, previous, previous - remaining, null, "Expired batch written off");
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------
    // deductStock: shared by sales and negative adjustments. Must run inside a transaction,
    // with 'product' read by getProductForUpdate on the same connection.
    // Takes from the batch that expires first (FEFO) and writes one history row per batch.
    // skipExpired = true for sales: expired batches are never sold.
    // Throws IllegalStateException (and changes nothing) when there is not enough stock.
    // ------------------------------------------------------------------
    void deductStock(Connection conn, Products product, long businessId, long userId, int quantity,
            TransactionType type, Long referenceId, String reason, boolean skipExpired) throws SQLException {
        if (product.getCurrentStock() < quantity) {
            throw new IllegalStateException("Not enough stock for " + product.getProductName() + " (have "
                    + product.getCurrentStock() + ", need " + quantity + ").");
        }
        int running = product.getCurrentStock();

        if (!product.isTracksExpiry()) {
            logTransaction(conn, product.getProductId(), userId, null, type, -quantity, running,
                    running - quantity, referenceId, reason);
        } else {
            // Plan first, so nothing is changed if the batches cannot cover the quantity.
            List<StockBatches> batches = new ArrayList<>();
            List<Integer> takes = new ArrayList<>();
            int left = quantity;
            LocalDate today = LocalDate.now();
            for (StockBatches b : stockBatchesDAO.getBatchesFEFO(conn, product.getProductId())) {
                if (left == 0) {
                    break;
                }
                if (skipExpired && b.getExpirationDate() != null && b.getExpirationDate().isBefore(today)) {
                    continue;
                }
                int take = Math.min(left, b.getQuantityRemaining());
                batches.add(b);
                takes.add(take);
                left -= take;
            }
            if (left > 0 && skipExpired) {
                throw new IllegalStateException("Not enough unexpired stock for " + product.getProductName() + ".");
            }

            for (int i = 0; i < batches.size(); i++) {
                StockBatches b = batches.get(i);
                int take = takes.get(i);
                stockBatchesDAO.updateQuantityRemaining(conn, b.getBatchId(), businessId,
                        b.getQuantityRemaining() - take);
                logTransaction(conn, product.getProductId(), userId, b.getBatchId(), type, -take, running,
                        running - take, referenceId, reason);
                running -= take;
            }
            if (left > 0) { // stock that was never put in a batch (manual adjustment only)
                logTransaction(conn, product.getProductId(), userId, null, type, -left, running,
                        running - left, referenceId, reason);
            }
        }

        if (!productsDAO.updateCurrentStock(conn, product.getProductId(), businessId, -quantity)) {
            throw new IllegalStateException("Not enough stock for " + product.getProductName() + ".");
        }
    }

    // ---------------------------- helpers ----------------------------
    private long businessOf(long userId) throws SQLException {
        Users user = usersDAO.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user.");
        }
        return user.getBusinessId();
    }

    private Products requireProduct(Connection conn, long productId, long businessId) throws SQLException {
        Products product = productsDAO.getProductForUpdate(conn, productId, businessId);
        if (product == null) {
            throw new IllegalArgumentException("That product does not exist in this business.");
        }
        return product;
    }

    private TransactionType parseAdjustType(String type) {
        TransactionType t;
        try {
            t = TransactionType.valueOf(type == null ? "" : type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type must be ADJUSTMENT or DAMAGE.");
        }
        if (t != TransactionType.ADJUSTMENT && t != TransactionType.DAMAGE) {
            throw new IllegalArgumentException("Type must be ADJUSTMENT or DAMAGE.");
        }
        return t;
    }

    private String newBatchNumber(String prefix, long productId) {
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-"
                + productId + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private void logTransaction(Connection conn, long productId, long userId, Long batchId, TransactionType type,
            int quantity, int previousStock, int newStock, Long referenceId, String reason) throws SQLException {
        transactionsDAO.insertTransaction(conn, new InventoryTransactions(0, productId, userId, batchId, type,
                null, quantity, previousStock, newStock, referenceId, reason));
    }
}
