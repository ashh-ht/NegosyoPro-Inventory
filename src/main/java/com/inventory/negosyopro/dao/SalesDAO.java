package com.inventory.negosyopro.dao;

import java.sql.*;
import java.time.*;
import java.util.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Sales;
import com.inventory.negosyopro.model.SaleItems;

public class SalesDAO {

    public long insertSale(Connection conn, Sales sale) throws SQLException {
        String sql = "INSERT INTO sales (business_id, user_id, order_number, customer_name, customer_contact, "
                + "subtotal, discount_amount, tax_amount, total_amount, amount_paid, change_amount, "
                + "payment_method_id, discount_id, sale_status) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, sale.getBusinessId());
            pstmt.setLong(2, sale.getUserId());
            pstmt.setString(3, sale.getOrderNumber());
            pstmt.setString(4, sale.getCustomerName());
            pstmt.setString(5, sale.getCustomerContact());
            pstmt.setBigDecimal(6, sale.getSubtotal());
            pstmt.setBigDecimal(7, sale.getDiscountAmount());
            pstmt.setBigDecimal(8, sale.getTaxAmount());
            pstmt.setBigDecimal(9, sale.getTotalAmount());
            pstmt.setBigDecimal(10, sale.getAmountPaid());
            pstmt.setBigDecimal(11, sale.getChangeAmount());
            pstmt.setLong(12, sale.getPaymentMethodId());
            if (sale.getDiscountId() == null) {
                pstmt.setNull(13, Types.BIGINT);
            } else {
                pstmt.setLong(13, sale.getDiscountId());
            }
            pstmt.setString(14, sale.getStatus().name());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no sale_id was returned.");
            }
        }
    }

    public long insertSaleItem(Connection conn, SaleItems item) throws SQLException {
        String sql = "INSERT INTO sale_items (sale_id, product_id, product_name, quantity, unit_price, "
                + "cost_price, discount_amount, tax_amount, subtotal) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, item.getSalesId());
            pstmt.setLong(2, item.getProductId());
            pstmt.setString(3, item.getProductName());
            pstmt.setInt(4, item.getQuantity());
            pstmt.setBigDecimal(5, item.getUnitPrice());
            pstmt.setBigDecimal(6, item.getCostPrice());
            pstmt.setBigDecimal(7, item.getDiscountAmount());
            pstmt.setBigDecimal(8, item.getTaxAmount());
            pstmt.setBigDecimal(9, item.getSubtotal());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no sale_item_id was returned.");
            }
        }
    }

    // Marks a sale CANCELLED or REFUNDED. Throws so the service can roll back.
    public boolean updateSaleStatus(Connection conn, long saleId, long businessId, Sales.Status status,
            String cancelReason) throws SQLException {
        String sql = "UPDATE sales SET sale_status = ?, cancel_reason = ? "
                + "WHERE sale_id = ? AND business_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, cancelReason); // null is fine
            pstmt.setLong(3, saleId);
            pstmt.setLong(4, businessId);
            return pstmt.executeUpdate() > 0;
        }
    }

    // Sales from the start of 'from' to the end of 'to' (both days included),
    // newest first.
    public List<Sales> getSales(long businessId, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT * FROM sales WHERE business_id = ? "
                + "AND created_at >= ? AND created_at < ? "
                + "ORDER BY created_at DESC, sale_id DESC";
        List<Sales> sales = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            pstmt.setTimestamp(2, Timestamp.valueOf(from.atStartOfDay()));
            pstmt.setTimestamp(3, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapSale(rs));
                }
            }
        }
        return sales;
    }

    public Sales getSaleById(long saleId, long businessId) {
        String sql = "SELECT * FROM sales WHERE sale_id = ? AND business_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, saleId);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapSale(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<SaleItems> getItemsBySale(long saleId, long businessId) {
        String sql = "SELECT item.* FROM sale_items item "
                + "JOIN sales s ON s.sale_id = item.sale_id "
                + "WHERE item.sale_id = ? AND s.business_id = ?";
        List<SaleItems> items = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, saleId);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapSaleItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    // Next number for this business, e.g. ORD-00001. order_number is VARCHAR(20),
    // so this fits.
    public String nextOrderNumber(long businessId) throws SQLException {
        String sql = "SELECT order_number FROM sales WHERE business_id = ? "
                + "ORDER BY sale_id DESC LIMIT 1";
        int next = 1;

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String last = rs.getString("order_number"); // e.g. "ORD-00007"
                    next = Integer.parseInt(last.substring(4)) + 1; // skip "ORD-"
                }
            }
        }
        return String.format("ORD-%05d", next);
    }

    // Constructor argument order = column order of the sales table.
    // If your Sales constructor is ordered differently, reorder these arguments to
    // match it.
    private Sales mapSale(ResultSet rs) throws SQLException {
        long discountId = rs.getLong("discount_id");
        Long discountIdOrNull = rs.wasNull() ? null : discountId; // optional discount

        return new Sales(
                rs.getLong("sale_id"),
                rs.getLong("business_id"),
                rs.getLong("user_id"),
                rs.getString("order_number"),
                rs.getString("customer_name"),
                rs.getString("customer_contact"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("tax_amount"),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("change_amount"),
                rs.getLong("payment_method_id"),
                discountIdOrNull,
                Sales.Status.valueOf(rs.getString("sale_status")),
                rs.getString("cancel_reason"),
                rs.getObject("created_at", LocalDateTime.class));
    }

    // Constructor argument order = column order of the sale_items table.
    private SaleItems mapSaleItem(ResultSet rs) throws SQLException {
        return new SaleItems(
                rs.getLong("sale_item_id"),
                rs.getLong("sale_id"),
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("cost_price"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("tax_amount"),
                rs.getBigDecimal("subtotal"));
    }
}