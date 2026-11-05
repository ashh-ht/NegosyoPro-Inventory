package com.inventory.negosyopro.controller.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventory.negosyopro.config.CurrentUser;
import com.inventory.negosyopro.dao.AiReportsDAO;
import com.inventory.negosyopro.model.Users;
import com.inventory.negosyopro.service.AiReportService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/ai-reports")
public class AiReportAdminController {

    private final AiReportService aiReportService;
    private final AiReportsDAO aiReportsDAO;

    public AiReportAdminController(AiReportService aiReportService, AiReportsDAO aiReportsDAO) {
        this.aiReportService = aiReportService;
        this.aiReportsDAO = aiReportsDAO;
    }

    @GetMapping
    public String page(@RequestParam(defaultValue = "WEEKLY") String reportType,
            HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        model.addAttribute("reportType", reportType);
        model.addAttribute("report", aiReportsDAO.getLatestAiReport(bid, reportType)); // can be null
        model.addAttribute("today", LocalDate.now());
        return "admin/ai-reports";
    }

    // Can take a while with the local model; the page should show "Generating..." on the button.
    @PostMapping("/generate")
    public String generate(@RequestParam String reportType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        Users user = CurrentUser.get(session);
        try {
            aiReportService.generateReport(user.getBusinessId(), user.getUserId(), reportType, from, to);
            flash.addFlashAttribute("success", "AI summary generated.");
        } catch (IllegalArgumentException | IllegalStateException e) { // bad dates, or no completed sales
            flash.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            flash.addFlashAttribute("error", "The AI is not running. Start Ollama and try again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            flash.addFlashAttribute("error", "The AI request was interrupted. Please try again.");
        }
        return "redirect:/admin/ai-reports?reportType=" + reportType;
    }
}
