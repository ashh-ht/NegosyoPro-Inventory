package com.inventory.negosyopro.dao;

import java.sql.*;
import java.util.*;
import java.time.*;
import com.inventory.negosyopro.model.InventoryTransactions;
import com.inventory.negosyopro.db.dbConnection;

public class InventoryTransactionsDAO {
    public long insertTransaction(Connection conn, InventoryTransactions t) throws SQLException {
        String sql = "INSERT INTO inventory_transactions (product_id, user_id, batch_id, transaction_type, quantity, previous_stock, new_stock, reference_id, reason)"
                + " VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, t.getProductId());
            pstmt.setLong(2, t.getUserId());
            pstmt.setObject(3, t.getBatchId());
            pstmt.setString(4, t.getTransactionType().name());
            pstmt.setInt(5, t.getQuantity());
            pstmt.setInt(6, t.getPreviousStock());
            pstmt.setInt(7, t.getNewStock());
            pstmt.setObject(8, t.getReferenceId());
            pstmt.setString(9, t.getReason());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no transaction_id was returned.");
            }
        }
    }

    private InventoryTransactions mapTransactions(ResultSet rs) throws SQLException {
        return new InventoryTransactions(
                rs.getLong("inventory_transaction_id"),
                rs.getLong("product_id"),
                rs.getLong("user_id"),
                rs.getObject("batch_id", Long.class),
                InventoryTransactions.TransactionType.valueOf(rs.getString("transaction_type")),
                rs.getObject("transaction_date", LocalDateTime.class),
                rs.getInt("quantity"),
                rs.getInt("previous_stock"),
                rs.getInt("new_stock"),
                rs.getObject("reference_id", Long.class),
                rs.getString("reason"));
    }

    public List<InventoryTransactions> getTransactionsByProduct(long productId, long businessId) {
        String sql = "SELECT t.* FROM inventory_transactions t "
                + "JOIN products p ON p.product_id = t.product_id "
                + "WHERE t.product_id = ? AND p.business_id = ? "
                + "ORDER BY t.inventory_transaction_id DESC";
        List<InventoryTransactions> t = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, productId);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    t.add(mapTransactions(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return t;
    }

    public List<InventoryTransactions> getTransactionsByReference(Connection conn,
            InventoryTransactions.TransactionType type, long referenceId, long businessId) throws SQLException {
        String sql = "SELECT t.* FROM inventory_transactions t "
                + "JOIN products p ON p.product_id = t.product_id "
                + "WHERE t.transaction_type = ? AND p.business_id = ? AND t.reference_id = ? "
                + "ORDER BY t.inventory_transaction_id DESC";
        List<InventoryTransactions> t = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type.name());
            pstmt.setLong(2, businessId);
            pstmt.setLong(3, referenceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    t.add(mapTransactions(rs));
                }
            }
        }

        return t;
    }
}
