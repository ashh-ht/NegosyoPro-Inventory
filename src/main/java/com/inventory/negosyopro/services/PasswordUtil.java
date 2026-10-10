package com.inventory.negosyopro.services;

import org.mindrot.jbcrypt.BCrypt;

//utility/helper class. do not put @Service.
public class PasswordUtil {
    private static final int WORKLOAD = 12; // controls how slow the hashing is, and that slowness is what protects
                                            // users’ passwords.

    public static String hashPassword(String passwordPlaintext) {
        if (passwordPlaintext == null || passwordPlaintext.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(passwordPlaintext, BCrypt.gensalt(WORKLOAD));
    }
    
    public static boolean checkPassword(String passwordPlaintext, String storedHash) {
        if (storedHash == null || storedHash.isBlank() || passwordPlaintext == null || passwordPlaintext.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(passwordPlaintext, storedHash);
        } catch (IllegalArgumentException e) {
            return false; 
        }
    }
}
