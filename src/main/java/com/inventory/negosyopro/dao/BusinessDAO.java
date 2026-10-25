package com.inventory.negosyopro.dao;

import java.sql.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Business;

public class BusinessDAO {
    // called by SetupService.createBusinessWithOwner (in setting up a new business)
    public long insertBusiness(Connection conn, Business business) throws SQLException {
        String sql = "INSERT INTO businesses (template_id, business_name, address, contact_number, "
                + "tax_enabled, tax_inclusive, tax_rate, currency_code, currency_symbol, receipt_footer, "
                + "uses_expiry, uses_purchase_orders, expiry_alert_days, ordering_cost, holding_cost_percent) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, business.getTemplateId());
            pstmt.setString(2, business.getBusinessName());
            pstmt.setString(3, business.getBusinessAddress());
            pstmt.setString(4, business.getBusinessContact());
            pstmt.setBoolean(5, business.isTaxEnabled());
            pstmt.setBoolean(6, business.isTaxInclusive());
            pstmt.setBigDecimal(7, business.getTaxRate());
            pstmt.setString(8, business.getCurrencyCode());
            pstmt.setString(9, business.getCurrencySymbol());
            pstmt.setString(10, business.getReceiptFooter());
            pstmt.setBoolean(11, business.isUsesExpiry());
            pstmt.setBoolean(12, business.isUsesPurchaseOrders());
            pstmt.setInt(13, business.getExpiryAlertDays());
            pstmt.setBigDecimal(14, business.getOrderingCost());
            pstmt.setBigDecimal(15, business.getHoldingCost());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no business_id was returned.");
            }
        }
    }

    // get business details
    public Business getBusinessById(long businessId) {
        String sql = "SELECT * FROM businesses WHERE business_id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Business(
                            rs.getLong("business_id"),
                            rs.getLong("template_id"),
                            rs.getString("business_name"),
                            rs.getString("address"),
                            rs.getString("contact_number"),
                            rs.getBoolean("tax_enabled"),
                            rs.getBoolean("tax_inclusive"),
                            rs.getBigDecimal("tax_rate"),
                            rs.getString("currency_code"),
                            rs.getString("currency_symbol"),
                            rs.getString("receipt_footer"),
                            rs.getBoolean("uses_expiry"),
                            rs.getBoolean("uses_purchase_orders"),
                            rs.getInt("expiry_alert_days"),
                            rs.getBigDecimal("ordering_cost"),
                            rs.getBigDecimal("holding_cost_percent"),
                            rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // call in settings. used for updating business details
    public boolean updateBusiness(Business business) {
        String sql = "UPDATE businesses SET template_id = ?, business_name = ?, address = ?, contact_number = ?, "
                + "tax_enabled = ?, tax_inclusive = ?, tax_rate = ?, currency_code = ?, currency_symbol = ?, "
                + "receipt_footer = ?, uses_expiry = ?, uses_purchase_orders = ?, expiry_alert_days = ?, "
                + "ordering_cost = ?, holding_cost_percent = ? WHERE business_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, business.getTemplateId());
            pstmt.setString(2, business.getBusinessName());
            pstmt.setString(3, business.getBusinessAddress());
            pstmt.setString(4, business.getBusinessContact());
            pstmt.setBoolean(5, business.isTaxEnabled());
            pstmt.setBoolean(6, business.isTaxInclusive());
            pstmt.setBigDecimal(7, business.getTaxRate());
            pstmt.setString(8, business.getCurrencyCode());
            pstmt.setString(9, business.getCurrencySymbol());
            pstmt.setString(10, business.getReceiptFooter());
            pstmt.setBoolean(11, business.isUsesExpiry());
            pstmt.setBoolean(12, business.isUsesPurchaseOrders());
            pstmt.setInt(13, business.getExpiryAlertDays());
            pstmt.setBigDecimal(14, business.getOrderingCost());
            pstmt.setBigDecimal(15, business.getHoldingCost());
            pstmt.setLong(16, business.getBusinessId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateTemplate(long businessId, long templateId, boolean usesExpiry, boolean usesPurchaseOrders) {
        String sql = "UPDATE businesses SET template_id = ?, uses_expiry = ?, uses_purchase_orders = ? WHERE business_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, templateId);
            pstmt.setBoolean(2, usesExpiry);
            pstmt.setBoolean(3, usesPurchaseOrders);
            pstmt.setLong(4, businessId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}