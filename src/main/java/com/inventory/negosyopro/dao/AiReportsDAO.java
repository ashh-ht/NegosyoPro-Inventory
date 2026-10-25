package com.inventory.negosyopro.dao;

import java.sql.*;
import java.time.*;
import com.inventory.negosyopro.db.dbConnection;
import com.inventory.negosyopro.model.AiReports;

public class AiReportsDAO {

    // Saves the text the AI returned.
    public long insertAiReport(AiReports report) throws SQLException {
        String sql = "INSERT INTO ai_reports (business_id, user_id, report_type, period_start, period_end, content) "
                + "VALUES (?,?,?,?,?,?)";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, report.getBusinessId());
            pstmt.setLong(2, report.getUserId());
            pstmt.setString(3, report.getReportType());
            pstmt.setObject(4, report.getPeriodStart());
            pstmt.setObject(5, report.getPeriodEnd());
            pstmt.setString(6, report.getContent());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert succeeded but no ai_report_id was returned.");
            }
        }
    }

    // Last saved report of this type, so the Reports page does not call the AI
    // again.
    public AiReports getLatestAiReport(long businessId, String reportType) {
        String sql = "SELECT * FROM ai_reports WHERE business_id = ? AND report_type = ? "
                + "ORDER BY created_at DESC, ai_report_id DESC LIMIT 1";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, businessId);
            pstmt.setString(2, reportType);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapAiReport(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private AiReports mapAiReport(ResultSet rs) throws SQLException {
        return new AiReports(
                rs.getLong("ai_report_id"),
                rs.getLong("business_id"),
                rs.getLong("user_id"),
                rs.getString("report_type"),
                rs.getObject("period_start", LocalDate.class),
                rs.getObject("period_end", LocalDate.class),
                rs.getString("content"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}