package com.inventory.negosyopro.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class dbConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/negosyopro_inventory";
    private static final String USER = "inventory";
    private static final String PASSWORD = "OopInventorySystem";

    public static Connection dbConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}