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

    public List<Template> getTemplatesByKeyword(String keyword) {
        List<Template> templates = new ArrayList<>();
        String sql = "SELECT bt.* FROM business_templates bt "
                + "JOIN template_keywords tk ON bt.template_id = tk.template_id "
                + "WHERE tk.keyword = ? AND bt.is_active = 1";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, keyword);
            ResultSet rs = pstmt.executeQuery();
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return templates;
    }  
    
    public List<Template> getTemplateById(long templateId) {
        List<Template> templates = new ArrayList<>();
        String sql = "SELECT * FROM business_templates WHERE template_id = ? AND is_active = 1";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, templateId);
            ResultSet rs = pstmt.executeQuery();
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return templates;
    }
}
