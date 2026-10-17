package com.inventory.negosyopro.dao;

import java.sql.*;
import java.util.*;
import com.inventory.negosyopro.model.PaymentMethods;
import com.inventory.negosyopro.db.dbConnection;

public class PaymentMethodsDAO {
    // call in settings for inserting payment methods in db
    public long insertPaymentMethod(Connection conn, PaymentMethods method) throws SQLException {
        String sql = "INSERT INTO payment_methods (business_id, method_name, is_active) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, method.getBusinessId());
            pstmt.setString(2, method.getMethodName());
            pstmt.setBoolean(3, method.isActive());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no payment_method_id was returned.");
            }
        }
    }

    // will be called in setupService (when creating a business)
    public void insertDefaults(Connection conn, long businessId) throws SQLException {
        String[] defaultMethods = { "Cash", "E-wallet", "Card" };
        for (String methodName : defaultMethods) {
            PaymentMethods method = new PaymentMethods(0, businessId, methodName, true);
            insertPaymentMethod(conn, method);
        }
    }

    // updating payment method name in db. will be called in settings page.
    public boolean updatePaymentMethod(PaymentMethods method) {
        String sql = "UPDATE payment_methods SET method_name = ? WHERE payment_method_id = ? AND business_id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, method.getMethodName());
            pstmt.setLong(2, method.getPaymentMethodId());
            pstmt.setLong(3, method.getBusinessId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // activating/deactivating payment method in db.
    public boolean setPaymentMethodActive(long paymentMethodId, long businessId, boolean isActive) {
        String sql = "UPDATE payment_methods SET is_active = ? WHERE payment_method_id = ? AND business_id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isActive);
            pstmt.setLong(2, paymentMethodId);
            pstmt.setLong(3, businessId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // used for listing methods. NOTE: pass TRUE in pos checkout and FALSE in
    // settings page.
    public List<PaymentMethods> getPaymentMethodsByBusiness(long businessId, boolean onlyActive) {
        String sql = "SELECT * FROM payment_methods WHERE business_id = ?"
                + (onlyActive ? " AND is_active = TRUE" : "") + " ORDER BY payment_method_id";
        List<PaymentMethods> paymentMethods = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    paymentMethods.add(new PaymentMethods(
                            rs.getLong("payment_method_id"),
                            rs.getLong("business_id"),
                            rs.getString("method_name"),
                            rs.getBoolean("is_active")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paymentMethods;
    }
}
