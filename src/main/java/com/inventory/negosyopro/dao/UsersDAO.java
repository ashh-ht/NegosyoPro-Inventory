package com.inventory.negosyopro.dao;

import java.sql.*;
import java.util.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Users;

public class UsersDAO {

    private static final String INSERT_SQL = "INSERT INTO users (business_id, username, password_hash, first_name, last_name, "
            + "role, is_owner, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    //will be called in setup page.
    public long insertUser(Connection conn, Users user) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, user.getBusinessId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getFirstName());
            pstmt.setString(5, user.getLastName());
            pstmt.setString(6, user.getRole().name());
            pstmt.setBoolean(7, user.isOwner());
            pstmt.setBoolean(8, user.isActive());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Creating user failed, no user_id was returned.");
            }
        }
    }

    //username is unique. checks if username alr exists in the db.
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    // will be used in login. checks if username exists and returns the user object.
    public Users getUserByUsername(String username) {
        String sql = "SELECT user_id, business_id, username, password_hash, first_name, last_name, "
                + "role, is_owner, is_active FROM users WHERE username = ?";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Users(
                            rs.getLong("user_id"),
                            rs.getLong("business_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            Users.Role.valueOf(rs.getString("role")),
                            rs.getBoolean("is_active"),
                            rs.getBoolean("is_owner"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //will be used in listing all of the users in the business.
    public List<Users> getUsersByBusiness(long businessId) {
        List<Users> users = new ArrayList<>();
        String sql = "SELECT user_id, business_id, username, password_hash, first_name, last_name, "
                + "role, is_owner, is_active FROM users WHERE business_id = ?"
                + "ORDER BY is_owner DESC, first_name";
        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Users user = new Users(
                            rs.getLong("user_id"),
                            rs.getLong("business_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            Users.Role.valueOf(rs.getString("role")),
                            rs.getBoolean("is_active"),
                            rs.getBoolean("is_owner"));
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    

}