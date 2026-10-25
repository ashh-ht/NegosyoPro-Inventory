package com.inventory.negosyopro.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AiReports {
    private final long aiReportId;
    private final long businessId;
    private final long userId;
    private final String reportType;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final String content;
    private final LocalDateTime createdAt;

    // Full constructor: used when reading a row from the database.
    public AiReports(long aiReportId, long businessId, long userId, String reportType,
            LocalDate periodStart, LocalDate periodEnd, String content, LocalDateTime createdAt) {
        this.aiReportId = aiReportId;
        this.businessId = businessId;
        this.userId = userId;
        this.reportType = reportType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.content = content;
        this.createdAt = createdAt;
    }

    // Used by AiReportService before saving (the database sets the id and
    // created_at).
    public AiReports(long businessId, long userId, String reportType,
            LocalDate periodStart, LocalDate periodEnd, String content) {
        this(0, businessId, userId, reportType, periodStart, periodEnd, content, null);
    }

    public long getAiReportId() {
        return aiReportId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public long getUserId() {
        return userId;
    }

    public String getReportType() {
        return reportType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}