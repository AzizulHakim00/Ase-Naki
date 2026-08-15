package com.azizul.asenaki.web;

import com.azizul.asenaki.admin.AuditLogRepository;
import com.azizul.asenaki.common.AppException;
import com.azizul.asenaki.report.ReportService;
import com.azizul.asenaki.report.ReportState;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final ReportService reportService;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute(
                "reports", reportService.findAllForModeration());
        return "admin/reports";
    }

    @PostMapping("/reports/{id}")
    public String moderate(
            @PathVariable Long id,
            @RequestParam ReportState state,
            @RequestParam(required = false) String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            reportService.moderate(
                    id, state, reason, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "success", "Report moderation saved and audited.");
        } catch (AppException exception) {
            redirectAttributes.addFlashAttribute(
                    "error", exception.getMessage());
        }
        return "redirect:/admin/reports";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(Model model) {
        model.addAttribute("logs", auditLogRepository.findAllWithActor());
        return "admin/audit-logs";
    }
}
