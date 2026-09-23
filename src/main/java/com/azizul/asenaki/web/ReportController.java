package com.azizul.asenaki.web;

import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.ReportForm;
import com.azizul.asenaki.report.ReportService;
import com.azizul.asenaki.report.UtilityReport;
import com.azizul.asenaki.report.UtilityStatus;
import com.azizul.asenaki.report.UtilityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final AreaRepository areaRepository;

    @GetMapping("/add")
    public String showCreateForm(Model model) {
        model.addAttribute("form", new ReportForm());
        model.addAttribute("editMode", false);
        addOptions(model);
        return "reports/form";
    }

    @PostMapping("/add")
    public String create(
            @Valid @ModelAttribute("form") ReportForm form,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("editMode", false);
            addOptions(model);
            return "reports/form";
        }

        UtilityReport report = reportService.saveReport(form, authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Report created successfully.");
        return "redirect:/reports/" + report.getId();
    }

    @GetMapping("/{id}")
    public String details(
            @PathVariable String id,
            Authentication authentication,
            Model model) {
        UtilityReport report = reportService.getReport(id);
        boolean admin = isAdmin(authentication);
        String email = authentication == null ? null : authentication.getName();

        model.addAttribute("report", report);
        model.addAttribute("canManage", reportService.canManage(report, email, admin));
        return "reports/details";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable String id,
            Authentication authentication,
            Model model) {
        UtilityReport report = reportService.getReport(id);
        if (!reportService.canManage(
                report, authentication.getName(), isAdmin(authentication))) {
            throw new AccessDeniedException("You cannot modify this report");
        }

        model.addAttribute("form", reportService.toForm(report));
        model.addAttribute("editMode", true);
        model.addAttribute("reportId", id);
        addOptions(model);
        return "reports/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @Valid @ModelAttribute("form") ReportForm form,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("editMode", true);
            model.addAttribute("reportId", id);
            addOptions(model);
            return "reports/form";
        }

        reportService.updateReport(
                id, form, authentication.getName(), isAdmin(authentication));
        redirectAttributes.addFlashAttribute("success", "Report updated successfully.");
        return "redirect:/reports/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable String id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        reportService.deleteReport(
                id, authentication.getName(), isAdmin(authentication));
        redirectAttributes.addFlashAttribute("success", "Report deleted successfully.");
        return "redirect:/";
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private void addOptions(Model model) {
        model.addAttribute("areas", areaRepository.findAllByOrderByNameAsc());
        model.addAttribute("utilities", UtilityType.values());
        model.addAttribute("statuses", UtilityStatus.values());
    }
}
