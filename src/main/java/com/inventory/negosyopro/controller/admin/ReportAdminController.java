package com.inventory.negosyopro.controller.admin;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.inventory.negosyopro.config.CurrentUser;
import com.inventory.negosyopro.dao.ReportsDAO;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/reports")
public class ReportAdminController {

    private final ReportsDAO reportsDAO;

    public ReportAdminController(ReportsDAO reportsDAO) {
        this.reportsDAO = reportsDAO;
    }

    @GetMapping
    public String reports(@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session, Model model) throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        if (to == null) {
            to = LocalDate.now();
        }
        if (from == null) {
            from = to.minusDays(29); // default: last 30 days
        }
        if (to.isBefore(from)) {
            model.addAttribute("error", "The end date is before the start date.");
            from = to.minusDays(29);
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            model.addAttribute("error", "Please pick a period of one year or less.");
            from = to.minusDays(29);
        }

        // Days without sales are missing from the map, so build every day of the period for the chart.
        Map<LocalDate, BigDecimal> totals = reportsDAO.getSalesTotalsByPeriod(bid, from, to);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartValues = new ArrayList<>();
        BigDecimal revenue = BigDecimal.ZERO;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            BigDecimal value = totals.getOrDefault(day, BigDecimal.ZERO);
            chartLabels.add(day.format(fmt));
            chartValues.add(value);
            revenue = revenue.add(value);
        }

        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartValues", chartValues);
        model.addAttribute("revenue", revenue);
        model.addAttribute("topProducts", reportsDAO.getTopSellingProducts(bid, from, to, 10));
        model.addAttribute("profit", reportsDAO.getProfitByPeriod(bid, from, to));
        model.addAttribute("inventoryValue", reportsDAO.getInventoryValue(bid));
        return "admin/reports";
    }
}
