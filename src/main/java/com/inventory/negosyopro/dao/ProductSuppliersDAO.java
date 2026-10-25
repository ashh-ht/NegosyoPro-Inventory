package com.inventory.negosyopro.dao;

import java.sql.*;
import java.util.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.ProductSuppliers;

public class ProductSuppliersDAO {
    public long insertProductSupplier(Connection conn, ProductSuppliers ps) throws SQLException {
        String sql = "INSERT INTO product_suppliers (product_id, supplier_id, "
                + "supplier_product_code, supplier_cost, lead_time_days) "
                + "VALUES (?,?,?,?,?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, ps.getProductId());
            pstmt.setLong(2, ps.getSupplierId());
            pstmt.setString(3, ps.getSupplierProductCode());
            pstmt.setBigDecimal(4, ps.getSupplierCost());
            pstmt.setObject(5, ps.getLeadTimeDays());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no product_supplier_id was returned.");
            }
        }
    }

    // for changing supplier cost and lead time
    public boolean updateProductSupplier(ProductSuppliers ps, long businessId) {
        String sql = "UPDATE product_suppliers ps "
                + "JOIN products p ON p.product_id = ps.product_id "
                + "SET ps.supplier_product_code = ?, ps.supplier_cost = ?, ps.lead_time_days = ? "
                + "WHERE ps.product_supplier_id = ? AND p.business_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, ps.getSupplierProductCode());
            pstmt.setBigDecimal(2, ps.getSupplierCost());
            pstmt.setObject(3, ps.getLeadTimeDays());
            pstmt.setLong(4, ps.getProductSupplierId());
            pstmt.setLong(5, businessId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private ProductSuppliers mapProductSuppliers(ResultSet rs) throws SQLException {
        return new ProductSuppliers(
                rs.getLong("product_supplier_id"),
                rs.getLong("product_id"),
                rs.getLong("supplier_id"),
                rs.getString("supplier_product_code"),
                rs.getBigDecimal("supplier_cost"),
                rs.getObject("lead_time_days", Integer.class),
                rs.getBoolean("is_preferred"));
    }

    // only one preferred supplier for one product. setting one as preferred will clear other supplier in that certain product
    public boolean setPreferredSupplier(long productId, long supplierId, long businessId, boolean isPreferred) {
        String clearSql = "UPDATE product_suppliers ps "
                + "JOIN products p ON p.product_id = ps.product_id "
                + "SET ps.is_preferred = 0 "
                + "WHERE ps.product_id = ? AND p.business_id = ?";

        String setSql = "UPDATE product_suppliers ps "
                + "JOIN products p ON p.product_id = ps.product_id "
                + "SET ps.is_preferred = ? "
                + "WHERE ps.product_id = ? AND ps.supplier_id = ? AND p.business_id = ?";

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (isPreferred) {
                    try (PreparedStatement clear = conn.prepareStatement(clearSql)) {
                        clear.setLong(1, productId);
                        clear.setLong(2, businessId);
                        clear.executeUpdate();
                    }
                }

                int updated;
                try (PreparedStatement set = conn.prepareStatement(setSql)) {
                    set.setBoolean(1, isPreferred);
                    set.setLong(2, productId);
                    set.setLong(3, supplierId);
                    set.setLong(4, businessId);
                    updated = set.executeUpdate();
                }

                if (updated == 0) { // no such link for this business
                    conn.rollback();
                    return false;
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /// getting the supppliers of one product
    public List<ProductSuppliers> getSuppliersForProduct(long productId, long businessId) {
        String sql = "SELECT ps.* FROM product_suppliers ps "
                + "JOIN products p ON p.product_id = ps.product_id "
                + "WHERE ps.product_id = ? AND p.business_id = ?";
        List<ProductSuppliers> pSuppliers = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, productId);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pSuppliers.add(mapProductSuppliers(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pSuppliers;
    }

    // getting the products of one supplier
    public List<ProductSuppliers> getProductsForSupplier(long supplierId, long businessId) {
    String sql = "SELECT ps.* FROM product_suppliers ps "
               + "JOIN suppliers s ON s.supplier_id = ps.supplier_id "
               + "WHERE ps.supplier_id = ? AND s.business_id = ?";
        List<ProductSuppliers> pSuppliers = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, supplierId);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pSuppliers.add(mapProductSuppliers(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pSuppliers;
    }

    public ProductSuppliers getPreferredSupplier(long productId, long businessId) {
        String sql = "SELECT ps.* FROM product_suppliers ps "
                + "JOIN products p ON p.product_id = ps.product_id "
                + "WHERE ps.product_id = ? AND ps.is_preferred = 1 AND p.business_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, productId);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapProductSuppliers(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return null;
    }

}
