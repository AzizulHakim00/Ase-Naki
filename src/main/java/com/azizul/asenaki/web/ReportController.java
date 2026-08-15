package com.azizul.asenaki.web;

import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.ReportForm;
import com.azizul.asenaki.report.ReportService;
import com.azizul.asenaki.report.UtilityStatus;
import com.azizul.asenaki.report.UtilityType;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final AreaRepository areaRepository;

    @GetMapping("/add")
    public String showForm(Model model) {
        model.addAttribute("form", new ReportForm());
        addOptions(model);
        return "reports/form";
    }

    @PostMapping("/add")
    public String submit(
            @Valid @ModelAttribute("form") ReportForm form,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addOptions(model);
            return "reports/form";
        }

        try {
            var report = reportService.saveReport(
                    form, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "success", "Your report was saved successfully.");
            return "redirect:/reports/" + report.getId();
        } catch (IllegalArgumentException exception) {
            result.rejectValue("evidence", "invalid",
                    exception.getMessage());
            addOptions(model);
            return "reports/form";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("report", reportService.getReport(id));
        return "reports/details";
    }

    private void addOptions(Model model) {
        model.addAttribute("areas", areaRepository.findAllByOrderByNameAsc());
        model.addAttribute("utilities", UtilityType.values());
        model.addAttribute("statuses", UtilityStatus.values());
    }
}
