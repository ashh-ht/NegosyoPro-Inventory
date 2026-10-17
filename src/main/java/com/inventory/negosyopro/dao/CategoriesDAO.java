package com.inventory.negosyopro.dao;

import java.sql.*;
import java.util.*;
import com.inventory.negosyopro.model.Categories;
import com.inventory.negosyopro.db.dbConnection;

public class CategoriesDAO {
    // call in products page
    public long insertCategory(Connection conn, Categories category) throws SQLException {
        String sql = "INSERT INTO categories (business_id, category_name, is_active) VALUES (?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, category.getBusinessId());
            pstmt.setString(2, category.getCategoryName());
            pstmt.setBoolean(3, category.isActive());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no category_id was returned.");
            }
        }
    }

    // called in product page
    public boolean updateCategory(Categories category) {
        String sql = "UPDATE categories SET category_name = ? WHERE category_id = ? AND business_id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getCategoryName());
            pstmt.setLong(2, category.getCategoryId());
            pstmt.setLong(3, category.getBusinessId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }

    // called in product page
    public boolean setCategoryActive(long categoryId, long businessId, boolean isActive) {
        String sql = "UPDATE categories SET is_active = ? WHERE category_id = ? AND business_id = ?";

        try (Connection conn = dbConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
                pstmt.setBoolean(1, isActive);
                pstmt.setLong(2, categoryId);
                pstmt.setLong(3, businessId);
                return pstmt.executeUpdate() > 0;
            }catch(SQLException e){
                e.printStackTrace();
                return false;
            }
    }

    //used for getting all categories
    public List<Categories> getCategoriesByBusiness(long businessId, boolean onlyActive) {
        String sql = "SELECT * FROM categories WHERE business_id = ?"
                + (onlyActive ? " AND is_active = TRUE" : "") + " ORDER BY category_id";
        List<Categories> categories = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(new Categories(
                            rs.getLong("category_id"),
                            rs.getLong("business_id"),
                            rs.getString("category_name"),
                            rs.getBoolean("is_active")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }
}
