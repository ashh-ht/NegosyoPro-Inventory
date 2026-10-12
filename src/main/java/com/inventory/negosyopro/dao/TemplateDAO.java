package com.inventory.negosyopro.dao;

import java.util.*;
import java.sql.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.Template;

public class TemplateDAO {
    // Implementation for fetching all templates
    public List<Template> getAllTemplates() {
        List<Template> templates = new ArrayList<>();
        String sql = "SELECT * FROM business_templates WHERE is_active = 1";

        try (Connection conn = dbConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Template temp = new Template(
                        rs.getLong("template_id"),
                        rs.getString("template_name"),
                        Template.TemplateType.valueOf(rs.getString("template_type")),
                        rs.getString("description"),
                        rs.getBoolean("uses_expiry"),
                        rs.getBoolean("uses_purchase_orders"),
                        rs.getBoolean("is_active"));
                templates.add(temp);
            }
            return templates;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<String> getAllKeywords() {
        List<String> keywords = new ArrayList<>();
        String sql = "SELECT keyword FROM template_keywords";

        try (Connection conn = dbConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                keywords.add(rs.getString("keyword"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return keywords;
    }   

    // Called from: the setup wizard, after the user types what kind of business they run.
    public List<Template> getTemplatesByKeyword(List<String> keywords) {
        List<Template> templates = new ArrayList<>();
        if (keywords == null || keywords.isEmpty()) {
            return templates; // nothing typed: nothing to search
        }

        // one "?" per keyword: "?,?,?"
        String placeholders = String.join(",", Collections.nCopies(keywords.size(), "?"));
        String sql = "SELECT DISTINCT bt.* FROM business_templates bt "
                + "JOIN template_keywords tk ON bt.template_id = tk.template_id "
                + "WHERE tk.keyword IN (" + placeholders + ") AND bt.is_active = 1 "
                + "ORDER BY bt.template_name";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < keywords.size(); i++) {
                pstmt.setString(i + 1, keywords.get(i).trim());
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    templates.add(new Template(
                            rs.getLong("template_id"),
                            rs.getString("template_name"),
                            Template.TemplateType.valueOf(rs.getString("template_type")),
                            rs.getString("description"),
                            rs.getBoolean("uses_expiry"),
                            rs.getBoolean("uses_purchase_orders"),
                            rs.getBoolean("is_active")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return templates;
    }
    
    // called from: SetupService.createBusinessWithOwner (copies the template's two flags into the business).
    public Template getTemplateById(long templateId) {
        String sql = "SELECT * FROM business_templates WHERE template_id = ? AND is_active = 1";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, templateId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Template(
                            rs.getLong("template_id"),
                            rs.getString("template_name"),
                            Template.TemplateType.valueOf(rs.getString("template_type")),
                            rs.getString("description"),
                            rs.getBoolean("uses_expiry"),
                            rs.getBoolean("uses_purchase_orders"),
                            rs.getBoolean("is_active"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; 
    }
}
