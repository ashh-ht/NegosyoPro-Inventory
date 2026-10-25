package com.inventory.negosyopro.service;

import java.sql.SQLException;
import com.inventory.negosyopro.dao.UsersDAO;
import com.inventory.negosyopro.model.Users;

public class AuthService {
    private final UsersDAO usersDAO = new UsersDAO();

    // Wrong username and wrong password give the same result (null).
    // A correct password on a deactivated account throws IllegalStateException.
    public Users login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return null;
        }
        Users user = usersDAO.getUserByUsername(username.trim());
        if (user == null || !PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            return null;
        }
        if (!user.isActive()) {
            throw new IllegalStateException("This account is deactivated. Ask the owner to activate it.");
        }
        return user;
    }

    // false if the old password is wrong (or the user is missing).
    public boolean changePassword(long userId, String oldPassword, String newPassword) throws SQLException {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("The new password cannot be empty.");
        }
        Users user = usersDAO.getUserById(userId);
        if (user == null || !PasswordUtil.checkPassword(oldPassword, user.getPasswordHash())) {
            return false;
        }
        return usersDAO.updatePasswordHash(userId, PasswordUtil.hashPassword(newPassword));
    }
}
