package com.inventory.negosyopro.service;

import java.sql.Connection;
import java.sql.SQLException;
import com.inventory.negosyopro.dao.UsersDAO;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Users;

public class UserService {
    private final UsersDAO usersDAO = new UsersDAO();

    // An admin adds a sub-admin or an employee. Never an owner: only sign-up creates the owner.
    public long addUser(long businessId, Users newUser, String plainPassword) throws SQLException {
        if (usersDAO.usernameExists(newUser.getUsername())) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        String hash = PasswordUtil.hashPassword(plainPassword); // throws if empty

        Users user = new Users(0, businessId, newUser.getUsername(), hash,
                newUser.getFirstName(), newUser.getLastName(), newUser.getRole(), true, false);
        try (Connection conn = dbConnection.getConnection()) {
            return usersDAO.insertUser(conn, user);
        }
    }

    // businessId is the admin's own business: a user of another business is never changed.
    public boolean deactivateUser(long userId, long businessId) throws SQLException {
        Users target = usersDAO.getUserById(userId);
        if (target == null || target.getBusinessId() != businessId) {
            return false;
        }
        if (target.isOwner()) {
            throw new IllegalStateException("The owner account cannot be deactivated.");
        }
        return usersDAO.setUserActive(userId, false);
    }

    public boolean activateUser(long userId, long businessId) throws SQLException {
        Users target = usersDAO.getUserById(userId);
        if (target == null || target.getBusinessId() != businessId) {
            return false;
        }
        return usersDAO.setUserActive(userId, true);
    }
}
