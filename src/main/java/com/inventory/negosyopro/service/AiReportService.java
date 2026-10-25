package com.inventory.negosyopro.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import com.inventory.negosyopro.dao.AiReportsDAO;
import com.inventory.negosyopro.dao.BusinessDAO;
import com.inventory.negosyopro.dao.ReportsDAO;
import com.inventory.negosyopro.model.AiReports;
import com.inventory.negosyopro.model.Business;
import com.inventory.negosyopro.model.ProductSalesSummaries;

public class AiReportService {
    private final ReportsDAO reportsDAO = new ReportsDAO();
    private final AiReportsDAO aiReportsDAO = new AiReportsDAO();
    private final BusinessDAO businessDAO = new BusinessDAO();
    private final OllamaClient ollama;

    public AiReportService() {
        this(new OllamaClient()); // Ollama, qwen3.5:4b
    }

    public AiReportService(OllamaClient ollama) {
        this.ollama = ollama;
    }

    // Every number is calculated here in Java with ReportsDAO. The AI only receives the finished
    // summary and writes the words. Nothing is saved if the AI cannot be reached.
    public AiReports generateReport(long businessId, long userId, String reportType, LocalDate from, LocalDate to)
            throws SQLException, IOException, InterruptedException {
        if (reportType == null || reportType.isBlank()) {
            throw new IllegalArgumentException("Choose a report type.");
        }
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("The start date must be on or before the end date.");
        }
        Business business = businessDAO.getBusinessById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Unknown business.");
        }
        String currency = business.getCurrencyCode() == null ? "PHP" : business.getCurrencyCode();

        // This period
        Map<LocalDate, BigDecimal> daily = reportsDAO.getSalesTotalsByPeriod(businessId, from, to);
        BigDecimal revenue = sum(daily);
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        BigDecimal averageDaily = revenue.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);

        // The period of the same length right before it
        LocalDate previousTo = from.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        BigDecimal previousRevenue = sum(reportsDAO.getSalesTotalsByPeriod(businessId, previousFrom, previousTo));

        if (revenue.signum() == 0 && previousRevenue.signum() == 0) {
            throw new IllegalStateException("There are no completed sales in this period or the one before it.");
        }

        StringBuilder data = new StringBuilder();
        data.append("Report type: ").append(reportType).append('\n');
        data.append("Business: ").append(business.getBusinessName()).append('\n');
        data.append("Period: ").append(from).append(" to ").append(to).append(" (").append(days).append(" days)\n");
        data.append("Revenue this period: ").append(currency).append(' ').append(revenue).append('\n');
        data.append("Revenue in the previous ").append(days).append(" days (").append(previousFrom).append(" to ")
                .append(previousTo).append("): ").append(currency).append(' ').append(previousRevenue).append('\n');
        if (previousRevenue.signum() > 0) {
            BigDecimal change = revenue.subtract(previousRevenue).multiply(BigDecimal.valueOf(100))
                    .divide(previousRevenue, 1, RoundingMode.HALF_UP);
            data.append("Change versus the previous period: ").append(change).append("%\n");
        } else {
            data.append("Change versus the previous period: not available (no sales in the previous period)\n");
        }
        data.append("Average revenue per day: ").append(currency).append(' ').append(averageDaily).append('\n');

        Map.Entry<LocalDate, BigDecimal> best = null;
        for (Map.Entry<LocalDate, BigDecimal> e : daily.entrySet()) {
            if (best == null || e.getValue().compareTo(best.getValue()) > 0) {
                best = e;
            }
        }
        if (best != null) {
            data.append("Best day: ").append(best.getKey()).append(" with ").append(currency).append(' ')
                    .append(best.getValue()).append('\n');
        }
        data.append("Profit this period (selling price minus cost price): ").append(currency).append(' ')
                .append(reportsDAO.getProfitByPeriod(businessId, from, to)).append('\n');
        data.append("Current inventory value at cost: ").append(currency).append(' ')
                .append(reportsDAO.getInventoryValue(businessId)).append('\n');

        List<ProductSalesSummaries> top = reportsDAO.getTopSellingProducts(businessId, from, to, 5);
        if (!top.isEmpty()) {
            data.append("Top-selling products (by quantity):\n");
            int rank = 1;
            for (ProductSalesSummaries p : top) {
                data.append("  ").append(rank++).append(". ").append(p.getProductName()).append(" - ")
                        .append(p.getQuantity()).append(" sold, revenue ").append(currency).append(' ')
                        .append(p.getRevenue()).append('\n');
            }
        }

        String prompt = "You are a business report assistant.\n\n"
                + "Analyze the following sales data:\n\n" + data + "\n"
                + "Write a concise report containing:\n"
                + "1. A summary of the results.\n"
                + "2. Key observations based only on the supplied data.\n"
                + "3. Recommendations, clearly labeled as suggestions.\n\n"
                + "Do not invent missing information. Do not calculate new numbers; use only the numbers above.\n"
                + "Use " + currency + " as the currency.\n";

        String text = ollama.ask(prompt);

        AiReports toSave = new AiReports(businessId, userId, reportType.trim(), from, to, text);
        long id = aiReportsDAO.insertAiReport(toSave);
        return new AiReports(id, businessId, userId, toSave.getReportType(), from, to, text, LocalDateTime.now());
    }

    private BigDecimal sum(Map<LocalDate, BigDecimal> totals) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : totals.values()) {
            sum = sum.add(v);
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }
}
