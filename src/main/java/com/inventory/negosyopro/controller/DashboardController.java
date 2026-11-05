package com.inventory.negosyopro.controller;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.inventory.negosyopro.config.CurrentUser;
import com.inventory.negosyopro.dao.AiReportsDAO;
import com.inventory.negosyopro.dao.BusinessDAO;
import com.inventory.negosyopro.dao.ProductsDAO;
import com.inventory.negosyopro.dao.ReportsDAO;
import com.inventory.negosyopro.dao.SalesDAO;
import com.inventory.negosyopro.dao.StockBatchesDAO;
import com.inventory.negosyopro.model.Business;
import com.inventory.negosyopro.model.Products;
import com.inventory.negosyopro.model.Sales;
import com.inventory.negosyopro.model.Users;

import jakarta.servlet.http.HttpSession;

@Controller
public class DashboardController {

    private final ReportsDAO reportsDAO;
    private final SalesDAO salesDAO;
    private final ProductsDAO productsDAO;
    private final StockBatchesDAO stockBatchesDAO;
    private final BusinessDAO businessDAO;
    private final AiReportsDAO aiReportsDAO;

    public DashboardController(ReportsDAO reportsDAO, SalesDAO salesDAO, ProductsDAO productsDAO,
            StockBatchesDAO stockBatchesDAO, BusinessDAO businessDAO, AiReportsDAO aiReportsDAO) {
        this.reportsDAO = reportsDAO;
        this.salesDAO = salesDAO;
        this.productsDAO = productsDAO;
        this.stockBatchesDAO = stockBatchesDAO;
        this.businessDAO = businessDAO;
        this.aiReportsDAO = aiReportsDAO;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) throws SQLException {
        Users user = CurrentUser.get(session);
        long bid = user.getBusinessId(); // always from the session, never from the URL
        boolean isAdmin = CurrentUser.isAdmin(user);
        LocalDate today = LocalDate.now();

        Business business = businessDAO.getBusinessById(bid);
        if (business == null) { // business row missing: log the user out instead of crashing
            session.invalidate();
            return "redirect:/login";
        }

        // Sales today (money). The map only has days that had sales.
        BigDecimal salesToday = reportsDAO.getSalesTotalsByPeriod(bid, today, today)
                .getOrDefault(today, BigDecimal.ZERO);

        // Transactions today: getSales also returns CANCELLED and REFUNDED, so count COMPLETED only.
        int transactionsToday = 0;
        for (Sales s : salesDAO.getSales(bid, today, today)) {
            if (s.getStatus() == Sales.Status.COMPLETED) {
                transactionsToday++;
            }
        }

        // Low stock and out of stock
        List<Products> lowStock = productsDAO.getLowStockProducts(bid);
        List<Products> outOfStock = new ArrayList<>();
        for (Products p : productsDAO.getProductsByBusiness(bid, true)) {
            if (p.isTrackInventory() && p.getCurrentStock() == 0) {
                outOfStock.add(p);
            }
        }

        // Product names by id: the expiring list only carries batch rows, which hold the product id.
        Map<Long, String> productNames = new HashMap<>();
        for (Products p : productsDAO.getProductsByBusiness(bid, false)) {
            productNames.put(p.getProductId(), p.getProductName());
        }
        model.addAttribute("productNames", productNames);

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("salesToday", salesToday);
        model.addAttribute("transactionsToday", transactionsToday);
        model.addAttribute("lowStock", lowStock);
        model.addAttribute("outOfStock", outOfStock);

        // Expiring soon: only when this business tracks expiry
        if (business.isUsesExpiry()) {
            model.addAttribute("expiring",
                    stockBatchesDAO.getExpiringBatches(bid, business.getExpiryAlertDays()));
        }

        fillChart(model, bid, today);

        // Top 5 products in the last 30 days
        model.addAttribute("topProducts",
                reportsDAO.getTopSellingProducts(bid, today.minusDays(29), today, 5));

        // AI summary card: admin only. May be null if none was generated yet.
        if (isAdmin) {
            model.addAttribute("latestAi", aiReportsDAO.getLatestAiReport(bid, "WEEKLY"));
        }

        return "dashboard";
    }

    // Builds chartLabels and chartValues for the last 7 days (today included).
    // Days with no sales are missing from the map, so each day uses getOrDefault(..., ZERO).
    private void fillChart(Model model, long bid, LocalDate today) throws SQLException {
        Map<LocalDate, BigDecimal> totals = reportsDAO.getSalesTotalsByPeriod(bid, today.minusDays(6), today);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");

        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            labels.add(day.format(fmt));
            values.add(totals.getOrDefault(day, BigDecimal.ZERO));
        }
        model.addAttribute("chartLabels", labels);
        model.addAttribute("chartValues", values);
    }
}
