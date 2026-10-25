package com.inventory.negosyopro.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.ProductSalesSummaries;

// Read-only report queries. Only COMPLETED sales count; cancelled and refunded sales are ignored.
public class ReportsDAO {

    // Total sales per day, oldest day first.
    public Map<LocalDate, BigDecimal> getSalesTotalsByPeriod(long businessId, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "SELECT DATE(created_at) AS sale_day, SUM(total_amount) AS total "
                + "FROM sales WHERE business_id = ? AND sale_status = 'COMPLETED' "
                + "AND created_at >= ? AND created_at < ? "
                + "GROUP BY DATE(created_at) ORDER BY sale_day";
        Map<LocalDate, BigDecimal> totals = new LinkedHashMap<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            pstmt.setTimestamp(2, Timestamp.valueOf(from.atStartOfDay()));
            pstmt.setTimestamp(3, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getDate("sale_day").toLocalDate(), rs.getBigDecimal("total"));
                }
            }
        }
        return totals;
    }

    // Best sellers by quantity.
    public List<ProductSalesSummaries> getTopSellingProducts(long businessId, LocalDate from, LocalDate to,
            int limit) throws SQLException {
        String sql = "SELECT item.product_name, SUM(item.quantity) AS qty, SUM(item.subtotal) AS revenue "
                + "FROM sale_items item JOIN sales s ON s.sale_id = item.sale_id "
                + "WHERE s.business_id = ? AND s.sale_status = 'COMPLETED' "
                + "AND s.created_at >= ? AND s.created_at < ? "
                + "GROUP BY item.product_id, item.product_name "
                + "ORDER BY qty DESC LIMIT ?";
        List<ProductSalesSummaries> list = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            pstmt.setTimestamp(2, Timestamp.valueOf(from.atStartOfDay()));
            pstmt.setTimestamp(3, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            pstmt.setInt(4, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new ProductSalesSummaries(
                            rs.getString("product_name"),
                            rs.getInt("qty"),
                            rs.getBigDecimal("revenue")));
                }
            }
        }
        return list;
    }

    // Profit = (unit_price - cost_price) x quantity, minus each line's own
    // discount.
    public BigDecimal getProfitByPeriod(long businessId, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT COALESCE(SUM((item.unit_price - item.cost_price) * item.quantity "
                + "- item.discount_amount), 0) AS profit "
                + "FROM sale_items item JOIN sales s ON s.sale_id = item.sale_id "
                + "WHERE s.business_id = ? AND s.sale_status = 'COMPLETED' "
                + "AND s.created_at >= ? AND s.created_at < ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            pstmt.setTimestamp(2, Timestamp.valueOf(from.atStartOfDay()));
            pstmt.setTimestamp(3, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("profit");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    // Stock on hand valued at cost.
    public BigDecimal getInventoryValue(long businessId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(current_stock * cost_price), 0) AS value "
                + "FROM products WHERE business_id = ? AND is_active = 1";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("value");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    // Units sold in the last N days (completed sales only).
    public int getUnitsSold(long productId, int days) throws SQLException {
        String sql = "SELECT COALESCE(SUM(item.quantity), 0) AS units "
                + "FROM sale_items item JOIN sales s ON s.sale_id = item.sale_id "
                + "WHERE item.product_id = ? AND s.sale_status = 'COMPLETED' AND s.created_at >= ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, productId);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(days)));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("units");
                }
            }
        }
        return 0;
    }
}