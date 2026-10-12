package com.inventory.negosyopro.dao;

import java.sql.*;
import java.util.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Products;

public class ProductsDAO {
    // Called from: Product management (add product).
    public long insertProduct(Connection conn, Products p) throws SQLException {
        String sql = "INSERT INTO products (business_id, category_id, product_code, product_name, "
                + "cost_price, selling_price, unit, is_taxable, track_inventory, tracks_expiry, "
                + "shelf_life_days, current_stock, reorder_level, reorder_quantity) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, p.getBusinessId());
            pstmt.setLong(2, p.getCategoryId());
            pstmt.setString(3, p.getProductCode());
            pstmt.setString(4, p.getProductName());
            pstmt.setBigDecimal(5, p.getCostPrice());
            pstmt.setBigDecimal(6, p.getSellingPrice());
            pstmt.setString(7, p.getUnit());
            pstmt.setBoolean(8, p.isTaxable());
            pstmt.setBoolean(9, p.isTrackInventory());
            pstmt.setBoolean(10, p.isTracksExpiry());
            if (p.getShelfLifeDays() == null)
                pstmt.setNull(11, Types.SMALLINT);
            else
                pstmt.setInt(11, p.getShelfLifeDays());
            pstmt.setInt(12, p.getCurrentStock());
            pstmt.setInt(13, p.getReorderLevel());
            pstmt.setInt(14, p.getReorderQuantity());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no product_id was returned.");
            }
        }
    }

    public boolean updateProduct(Products p) {
        String sql = "UPDATE products SET category_id = ?, product_code = ?, product_name = ?, cost_price = ?, selling_price = ?, unit = ?, is_taxable = ?, track_inventory = ?, tracks_expiry = ?, shelf_life_days = ?, reorder_level = ?, reorder_quantity = ? WHERE product_id = ? AND business_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, p.getCategoryId());
            pstmt.setString(2, p.getProductCode());
            pstmt.setString(3, p.getProductName());
            pstmt.setBigDecimal(4, p.getCostPrice());
            pstmt.setBigDecimal(5, p.getSellingPrice());
            pstmt.setString(6, p.getUnit());
            pstmt.setBoolean(7, p.isTaxable());
            pstmt.setBoolean(8, p.isTrackInventory());
            pstmt.setBoolean(9, p.isTracksExpiry());
            pstmt.setInt(10, p.getShelfLifeDays());
            pstmt.setInt(11, p.getReorderLevel());
            pstmt.setInt(12, p.getReorderQuantity());
            pstmt.setLong(13, p.getProductId());
            pstmt.setLong(14, p.getBusinessId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setProductActive(long productId, long businessId, boolean isActive) {
        String sql = "UPDATE products SET is_active = ? WHERE product_id = ? AND business_id = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isActive);
            pstmt.setLong(2, productId);
            pstmt.setLong(3, businessId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Products mapProduct(ResultSet rs) throws SQLException {
        Integer shelfLife = rs.getInt("shelf_life_days");
        if (rs.wasNull())
            shelfLife = null;

        return new Products(
                rs.getLong("product_id"),
                rs.getLong("business_id"),
                rs.getLong("category_id"),
                rs.getString("product_code"),
                rs.getString("product_name"),
                rs.getBigDecimal("cost_price"),
                rs.getBigDecimal("selling_price"),
                rs.getString("unit"),
                rs.getBoolean("is_taxable"),
                rs.getBoolean("track_inventory"),
                rs.getBoolean("tracks_expiry"),
                shelfLife,
                rs.getInt("current_stock"),
                rs.getInt("reorder_level"),
                rs.getInt("reorder_quantity"),
                rs.getBoolean("is_active"));
    }

    // will be called in new sale and receiving stock. pass false if kasama inactive
    // products
    public Products getProductById(long productId, long businessId, boolean activeOnly) {
        String sql = "SELECT * FROM products WHERE product_id = ? AND business_id = ?"
                + (activeOnly ? " AND is_active = 1" : "");
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, productId);
            pstmt.setLong(2, businessId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next())
                    return mapProduct(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // called when a product code is typed. pass true (inactive products should be
    // unsellable)
    public Products getProductByCode(long businessId, String productCode, boolean activeOnly) {
        String sql = "SELECT * FROM products WHERE product_code = ? AND business_id = ?"
                + (activeOnly ? " AND is_active = 1" : "");

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productCode);
            pstmt.setLong(2, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next())
                    return mapProduct(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // called when displaying all of the products. Settings and Product management
    // pass false, and sale and stock pages pass true. (both getProductsByBusiness
    // and ByCateg)
    public List<Products> getProductsByBusiness(long businessId, boolean activeOnly) {
        String sql = "SELECT * FROM products WHERE business_id = ?"
                + (activeOnly ? " AND is_active = 1" : "")
                + " ORDER BY product_code";
        List<Products> products = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public List<Products> getProductByCateg(long businessId, long categoryId, boolean activeOnly) {
        String sql = "SELECT * FROM products WHERE business_id = ? AND category_id = ?"
                + (activeOnly ? " AND is_active = 1" : "")
                + " ORDER BY product_code";
        List<Products> products = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            pstmt.setLong(2, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public List<Products> searchProduct(long businessId, String text) {
        if (text == null)
            text = "";
        String sql = "SELECT * FROM products WHERE business_id = ? AND is_active = 1 AND (product_name LIKE ? OR product_code LIKE ?) ORDER BY product_name LIMIT 50";
        List<Products> products = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String pattern = "%" + text.trim() + "%";
            pstmt.setLong(1, businessId);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public List<Products> getLowStockProducts(long businessId) {
        String sql = "SELECT * FROM products WHERE business_id = ? AND track_inventory = 1 AND is_active = 1"
                + " AND current_stock <= reorder_level "
                + "ORDER BY (current_stock - reorder_level), product_name";
        List<Products> products = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    // called inside transactions: receiving(add), sales(minus), adjustment(+/-)
    public boolean updateCurrentStock(Connection conn, long productId, long businessId, int addedStock)
            throws SQLException {
        String sql = "UPDATE products SET current_stock + ? WHERE product_id = ? AND business_id = ? AND current_stock + ? >= 0";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, addedStock);
            pstmt.setLong(2, productId);
            pstmt.setLong(3, businessId);
            pstmt.setInt(4, addedStock);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateReorderSettings(long businessId, long productId, int reorderQuantity, int reorderLevel) {
        String sql = "UPDATE products SET reorder_level = ?, reorder_quantity = ?"
                + " WHERE business_id = ? AND product_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reorderLevel);
            pstmt.setInt(2, reorderQuantity);
            pstmt.setLong(3, businessId);
            pstmt.setLong(4, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
