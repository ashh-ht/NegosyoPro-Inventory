package com.inventory.negosyopro.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import com.inventory.negosyopro.model.Discounts;
import com.inventory.negosyopro.db.dbConnection;

public class DiscountsDAO {
    // will be called in settim=ngs for adding discounts in db
    public long insertDiscount(Connection conn, Discounts discount) throws SQLException {
        String sql = "INSERT INTO discounts (business_id, discount_name, discount_percent, requires_id, is_active) VALUES (?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, discount.getBusinessId());
            pstmt.setString(2, discount.getDiscountName());
            pstmt.setBigDecimal(3, discount.getDiscountPercent());
            pstmt.setBoolean(4, discount.isRequiresId());
            pstmt.setBoolean(5, discount.isActive());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no discount_id was returned.");
            }
        }
    }  

    // called in setupService
    public void insertDefaults(Connection conn, long businessId) throws SQLException {
        String[] defaultDiscounts = { "Senior Citizen", "PWD" };
        for (String discountName : defaultDiscounts) {
            Discounts discount = new Discounts(0, businessId, discountName, new BigDecimal("20.00"), true, true);
            insertDiscount(conn, discount);
        }
    }

    // updating discount name, percent, and requires_id in db
    public boolean updateDiscount(Discounts discount) {
        String sql = "UPDATE discounts SET discount_name = ?, discount_percent = ?, requires_id = ? WHERE discount_id = ? AND business_id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, discount.getDiscountName());
            pstmt.setBigDecimal(2, discount.getDiscountPercent());
            pstmt.setBoolean(3, discount.isRequiresId());
            pstmt.setLong(4, discount.getDiscountId());
            pstmt.setLong(5, discount.getBusinessId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // setting discount active/inactive
    public boolean setDiscountActive(long discountId, long businessId, boolean isActive) {
        String sql = "UPDATE discounts SET is_active = ? WHERE discount_id = ? AND business_id =?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isActive);
            pstmt.setLong(2, discountId);
            pstmt.setLong(3, businessId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Discounts> getDiscountsByBusiness(long businessId, boolean onlyActive) {
        String sql = "SELECT * FROM discounts WHERE business_id = ?"
                + (onlyActive ? " AND is_active = TRUE" : "") + " ORDER BY discount_id";
        List<Discounts> discounts = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    discounts.add(new Discounts(
                            rs.getLong("discount_id"),
                            rs.getLong("business_id"),
                            rs.getString("discount_name"),
                            rs.getBigDecimal("discount_percent"),
                            rs.getBoolean("requires_id"),
                            rs.getBoolean("is_active")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return discounts;
    }
}
