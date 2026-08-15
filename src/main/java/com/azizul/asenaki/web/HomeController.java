package com.azizul.asenaki.web;

import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.ReportService;
import com.azizul.asenaki.report.UtilityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ReportService reportService;
    private final AreaRepository areaRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("reports", reportService.getAllReports());
        model.addAttribute("utilities", UtilityType.values());
        model.addAttribute("areaCount", areaRepository.count());
        return "home";
    }
}
