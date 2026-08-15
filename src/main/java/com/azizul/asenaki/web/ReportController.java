package com.azizul.asenaki.web;

import com.azizul.asenaki.common.AppException;
import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.ConfirmationChoice;
import com.azizul.asenaki.report.ReportForm;
import com.azizul.asenaki.report.ReportService;
import com.azizul.asenaki.report.UtilityStatus;
import com.azizul.asenaki.report.UtilityTypeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final AreaRepository areaRepository;
    private final UtilityTypeRepository utilityTypeRepository;

    @GetMapping("/new")
    public String newReport(Model model) {
        if (!model.containsAttribute("reportForm")) {
            model.addAttribute("reportForm", new ReportForm());
        }
        addFormOptions(model);
        return "reports/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute ReportForm reportForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormOptions(model);
            return "reports/form";
        }

        try {
            var report = reportService.create(
                    reportForm, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "success", "Thank you. Your report is now live.");
            return "redirect:/reports/view/" + report.getId();
        } catch (AppException exception) {
            bindingResult.reject("report", exception.getMessage());
            addFormOptions(model);
            return "reports/form";
        }
    }

    @GetMapping("/view/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("report", reportService.findDetailed(id));
        return "reports/details";
    }

    @PostMapping("/{id}/vote")
    public String vote(
            @PathVariable Long id,
            @RequestParam ConfirmationChoice choice,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            reportService.vote(id, choice, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "success", "Your community vote was saved.");
        } catch (AppException exception) {
            redirectAttributes.addFlashAttribute(
                    "error", exception.getMessage());
        }
        return "redirect:/reports/view/" + id;
    }

    private void addFormOptions(Model model) {
        model.addAttribute("areas", areaRepository.findAllWithLocation());
        model.addAttribute("utilities", utilityTypeRepository.findAll());
        model.addAttribute("statuses", UtilityStatus.values());
    }
}
