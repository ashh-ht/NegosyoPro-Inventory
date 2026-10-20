package com.inventory.negosyopro.dao;

import java.sql.*;
import java.util.*;
import java.time.*;
import com.inventory.negosyopro.model.Suppliers;
import com.inventory.negosyopro.db.dbConnection;

public class SuppliersDAO {
    public long insertSupplier(Connection conn, Suppliers supplier) throws SQLException {
        String sql = "INSERT INTO suppliers (business_id, supplier_name, contact_person, contact_number, email, address, is_active)"
                + " VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, supplier.getBusinessId());
            pstmt.setString(2, supplier.getSupplierName());
            pstmt.setString(3, supplier.getContactPerson());
            pstmt.setString(4, supplier.getContactNumber());
            pstmt.setString(5, supplier.getEmail());
            pstmt.setString(6, supplier.getAddress());
            pstmt.setBoolean(7, supplier.isActive());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no supplier_id was returned.");
            }
        }
    }

    public boolean updateSupplier(Suppliers supplier) {
        String sql = "UPDATE suppliers SET supplier_name = ?, contact_person = ?, contact_number = ?, email = ?, address = ? WHERE supplier_id = ? AND business_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, supplier.getSupplierName());
            pstmt.setString(2, supplier.getContactPerson());
            pstmt.setString(3, supplier.getContactNumber());
            pstmt.setString(4, supplier.getEmail());
            pstmt.setString(5, supplier.getAddress());
            pstmt.setLong(6, supplier.getSupplierId());
            pstmt.setLong(7, supplier.getBusinessId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setSupplierActive(long supplierId, long businessId, boolean isActive) {
        String sql = "UPDATE suppliers SET is_active = ? WHERE supplier_id = ? AND business_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isActive);
            pstmt.setLong(2, supplierId);
            pstmt.setLong(3, businessId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // listing supppliers
    public List<Suppliers> getSuppliersByBusiness(long businessId, boolean activeOnly) {
        String sql = "SELECT * FROM suppliers WHERE business_id = ?"
                + (activeOnly ? " AND is_active = 1 " : "")
                + " ORDER BY supplier_name";
        List<Suppliers> suppliers = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    suppliers.add(mapSupplier(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suppliers;
    }

    // for selecting only one supplier
    public Suppliers getSupplierById(long supplierId, long businessId, boolean activeOnly) {
        String sql = "SELECT * FROM suppliers WHERE supplier_id = ? AND business_id = ?"
                + (activeOnly ? " AND is_active = 1" : "");

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, supplierId);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapSupplier(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // not found, or belongs to another business
    }

    private Suppliers mapSupplier(ResultSet rs) throws SQLException {
        return new Suppliers(
                rs.getLong("supplier_id"),
                rs.getLong("business_id"),
                rs.getString("supplier_name"),
                rs.getString("contact_person"),
                rs.getString("contact_number"),
                rs.getString("email"),
                rs.getString("address"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
