package com.inventory.negosyopro.dao;

import java.sql.*;
import java.time.*;
import java.util.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.PurchaseOrders;
import com.inventory.negosyopro.model.PurchaseOrderItems;

public class PurchaseOrdersDAO {
    public long insertPurchaseOrder(Connection conn, PurchaseOrders po) throws SQLException {
        String sql = "INSERT INTO purchase_orders (business_id, supplier_id, user_id, po_number, status, ordered_at, "
                + "expected_arrival, received_at, notes) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, po.getBusinessId());
            pstmt.setLong(2, po.getSupplierId());
            pstmt.setLong(3, po.getUserId());
            pstmt.setString(4, po.getPoNumber());
            pstmt.setString(5, po.getStatus().name());
            pstmt.setObject(6, po.getOrderedAt());
            pstmt.setObject(7, po.getExpectedArrival());
            pstmt.setObject(8, po.getReceivedAt());
            pstmt.setString(9, po.getNotes());
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no purchase_order_id was returned.");
            }
        }
    }

    public long insertPurchaseOrderItem(Connection conn, PurchaseOrderItems poItems) throws SQLException {
        String sql = "INSERT INTO purchase_order_items (purchase_order_id, product_id, quantity_ordered, quantity_received, unit_cost) "
                + "VALUES (?,?,?,?,?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, poItems.getPurchaseOrderId());
            pstmt.setLong(2, poItems.getProductId());
            pstmt.setInt(3, poItems.getQuantityOrdered());
            pstmt.setInt(4, poItems.getQuantityReceived());
            pstmt.setBigDecimal(5, poItems.getUnitCost());
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no purchase_order_items_id was returned.");
            }
        }
    }

    public boolean updateStatus(Connection conn, long purchaseOrderId, long businessId, PurchaseOrders.Status status)
            throws SQLException {
        String dateColumn = "";
        if (status == PurchaseOrders.Status.ORDERED) {
            dateColumn = ", ordered_at = CURDATE()";
        } else if (status == PurchaseOrders.Status.RECEIVED) {
            dateColumn = ", received_at = NOW()";
        }

        String sql = "UPDATE purchase_orders SET status = ?" + dateColumn
                + " WHERE purchase_order_id = ? AND business_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setLong(2, purchaseOrderId);
            pstmt.setLong(3, businessId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private PurchaseOrders mapPurchaseOrders(ResultSet rs) throws SQLException {
        return new PurchaseOrders(
                rs.getLong("purchase_order_id"),
                rs.getLong("business_id"),
                rs.getLong("supplier_id"),
                rs.getLong("user_id"),
                rs.getString("po_number"),
                PurchaseOrders.Status.valueOf(rs.getString("status")),
                rs.getObject("ordered_at", LocalDate.class),
                rs.getObject("expected_arrival", LocalDate.class),
                rs.getObject("received_at", LocalDateTime.class),
                rs.getString("notes"),
                rs.getObject("created_at", LocalDateTime.class));
    }

    public boolean updateItemQuantityReceived(Connection conn, long poItemId, long businessId, int quantity)
            throws SQLException {
        String sql = "UPDATE purchase_order_items item "
                + "JOIN purchase_orders po ON po.purchase_order_id = item.purchase_order_id "
                + "SET item.quantity_received = ? "
                + "WHERE item.purchase_order_item_id = ? AND po.business_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setLong(2, poItemId);
            pstmt.setLong(3, businessId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public PurchaseOrders getPurchaseOrderById(long purchaseOrderId, long businessId) {
        String sql = "SELECT * FROM purchase_orders WHERE purchase_order_id = ? AND business_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, purchaseOrderId);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapPurchaseOrders(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<PurchaseOrders> getPurchaseOrders(long businessId, PurchaseOrders.Status status) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM purchase_orders WHERE business_id = ?");
        if (status != null) {
            sql.append(" AND status = ?");
        }
        sql.append(" ORDER BY purchase_order_id DESC"); // newest first

        List<PurchaseOrders> orders = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setLong(1, businessId);
            if (status != null) {
                ps.setString(2, status.name());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapPurchaseOrders(rs));
                }
            }
        }
        return orders;
    }

    private PurchaseOrderItems mapPOItems(ResultSet rs) throws SQLException {
        return new PurchaseOrderItems(
                rs.getLong("purchase_order_item_id"),
                rs.getLong("purchase_order_id"),
                rs.getLong("product_id"),
                rs.getInt("quantity_ordered"),
                rs.getInt("quantity_received"),
                rs.getBigDecimal("unit_cost"));
    }

    public List<PurchaseOrderItems> getItemsByPurchaseOrder(long purchaseOrderId, long businessId) {
        String sql = "SELECT item.* FROM purchase_order_items item "
                + "JOIN purchase_orders po ON po.purchase_order_id = item.purchase_order_id "
                + "WHERE item.purchase_order_id = ? AND po.business_id = ?";
        List<PurchaseOrderItems> order = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, purchaseOrderId);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    order.add(mapPOItems(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return order;
    }

    public String nextPoNumber(long businessId) throws SQLException {
        String sql = "SELECT po_number FROM purchase_orders " +
                "WHERE business_id = ? " +
                "ORDER BY purchase_order_id DESC LIMIT 1";

        int next = 1; // first order for this business

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, businessId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String last = rs.getString("po_number"); // e.g. "PO-0007"
                    next = Integer.parseInt(last.substring(3)) + 1; // skip "PO-"
                }
            }
        }
        return String.format("PO-%04d", next); // 8 -> "PO-0008"
    }

}