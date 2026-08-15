package com.azizul.asenaki.web;

import com.azizul.asenaki.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ReportService reportService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("reports", reportService.getAllReports());
        return "home";
    }
}
