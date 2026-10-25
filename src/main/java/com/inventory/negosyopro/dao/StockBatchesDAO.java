package com.inventory.negosyopro.dao;

import java.sql.*;
import java.util.*;
import java.time.*;
import com.inventory.negosyopro.model.StockBatches;
import com.inventory.negosyopro.db.dbConnection;

public class StockBatchesDAO {
    public long insertBatch(Connection conn, StockBatches batch) throws SQLException {
        String sql = "INSERT INTO stock_batches (business_id, product_id, supplier_id, purchase_order_item_id, batch_number, quantity_received, quantity_remaining, unit_cost, expiration_date, received_at)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, batch.getBusinessId());
            pstmt.setLong(2, batch.getProductId());
            pstmt.setObject(3, batch.getSupplierId()); // Long, nullable
            pstmt.setObject(4, batch.getPurchaseOrderItemId());
            pstmt.setString(5, batch.getBatchNumber());
            pstmt.setInt(6, batch.getQuantityReceived());
            pstmt.setInt(7, batch.getQuantityRemaining());
            pstmt.setBigDecimal(8, batch.getUnitCost());
            pstmt.setObject(9, batch.getExpirationDate());
            pstmt.setObject(10, batch.getReceivedAt());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no batch_id was returned.");
            }
        }
    }

    private StockBatches mapBatches(ResultSet rs) throws SQLException {
        LocalDate expiryDate = rs.getObject("expiration_date", LocalDate.class);
        LocalDateTime receivedAt = rs.getObject("received_at", LocalDateTime.class);
        return new StockBatches(
                rs.getLong("batch_id"),
                rs.getLong("business_id"),
                rs.getLong("product_id"),
                rs.getObject("supplier_id", Long.class),
                rs.getObject("purchase_order_item_id", Long.class),
                rs.getString("batch_number"),
                rs.getInt("quantity_received"),
                rs.getInt("quantity_remaining"),
                rs.getBigDecimal("unit_cost"),
                expiryDate,
                receivedAt);

    }

    public List<StockBatches> getBatchesFEFO(Connection conn, long productId) throws SQLException {
        String sql = "SELECT * FROM stock_batches WHERE product_id = ? AND quantity_remaining > 0 "
                + "ORDER BY expiration_date IS NULL, expiration_date ASC, batch_id ASC FOR UPDATE";
        List<StockBatches> batch = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    batch.add(mapBatches(rs));
                }
            }
        }
        return batch;
    }

    //for getting and displaying expired batches
    public List<StockBatches> getExpiredBatches(long businessId) {
        String sql = "SELECT * FROM stock_batches WHERE expiration_date < CURDATE() AND quantity_remaining > 0 "
                + "AND business_id = ? ORDER BY expiration_date ASC";
        List<StockBatches> batches = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    batches.add(mapBatches(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return batches;
    }

    // One batch, read on the transaction's connection and locked (used by cancelSale and writeOffExpired).
    public StockBatches getBatchById(Connection conn, long batchId, long businessId) throws SQLException {
        String sql = "SELECT * FROM stock_batches WHERE batch_id = ? AND business_id = ? FOR UPDATE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, batchId);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapBatches(rs);
                }
            }
        }
        return null;
    }

    public List<StockBatches> getExpiringBatches(long businessId, int alertDays) {
        List<StockBatches> list = new ArrayList<>();

        String sql = "SELECT * FROM stock_batches "
                + "WHERE business_id = ? "
                + "AND quantity_remaining > 0 "
                + "AND expiration_date IS NOT NULL "
                + "AND expiration_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) "
                + "ORDER BY expiration_date ASC";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, businessId);
            ps.setInt(2, alertDays);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBatches(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateQuantityRemaining(Connection conn, long batchId, long businessId, int newQuantity)
            throws SQLException {

        if (newQuantity < 0) {
            throw new IllegalArgumentException("quantity_remaining cannot be negative.");
        }

        String sql = "UPDATE stock_batches SET quantity_remaining = ? "
                + "WHERE batch_id = ? AND business_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newQuantity);
            pstmt.setLong(2, batchId);
            pstmt.setLong(3, businessId);
            return pstmt.executeUpdate() > 0;
        }
    }
    

}
