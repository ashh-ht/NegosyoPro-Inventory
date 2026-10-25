package com.inventory.negosyopro.service;

import java.sql.Connection;
import java.sql.SQLException;
import com.inventory.negosyopro.dao.BusinessDAO;
import com.inventory.negosyopro.dao.DiscountsDAO;
import com.inventory.negosyopro.dao.PaymentMethodsDAO;
import com.inventory.negosyopro.dao.TemplateDAO;
import com.inventory.negosyopro.dao.UsersDAO;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Business;
import com.inventory.negosyopro.model.Template;
import com.inventory.negosyopro.model.Users;

public class SetupService {
    private final TemplateDAO templateDAO = new TemplateDAO();
    private final BusinessDAO businessDAO = new BusinessDAO();
    private final UsersDAO usersDAO = new UsersDAO();
    private final PaymentMethodsDAO paymentMethodsDAO = new PaymentMethodsDAO();
    private final DiscountsDAO discountsDAO = new DiscountsDAO();

    // Sign-up: one transaction creates the business, the owner, and the default payment methods and discounts.
    // Returns the new business_id. Everything is rolled back if any step fails.
    public long createBusinessWithOwner(Business business, Users owner, String plainPassword) throws SQLException {
        Template template = templateDAO.getTemplateById(business.getTemplateId());
        if (template == null) {
            throw new IllegalArgumentException("That business template does not exist or is not active.");
        }
        if (usersDAO.usernameExists(owner.getUsername())) {
            throw new IllegalArgumentException("That username is already taken.");
        }

        // The template decides the two flags.
        business.setUsesExpiry(template.isUsesExpiry());
        business.setUsesPurchaseOrders(template.isUsesPurchaseOrders());

        String hash = PasswordUtil.hashPassword(plainPassword); // throws if empty

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long businessId = businessDAO.insertBusiness(conn, business);

                // The owner is always an ADMIN with is_owner = 1, whatever the form said.
                Users newOwner = new Users(0, businessId, owner.getUsername(), hash,
                        owner.getFirstName(), owner.getLastName(), Users.Role.ADMIN, true, true);
                usersDAO.insertUser(conn, newOwner);

                paymentMethodsDAO.insertDefaults(conn, businessId);
                discountsDAO.insertDefaults(conn, businessId);

                conn.commit();
                return businessId;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
