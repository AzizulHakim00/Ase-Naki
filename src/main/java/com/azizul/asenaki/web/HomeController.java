package com.azizul.asenaki.web;

import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.monitoring.MonitoringQueryService;
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
    private final MonitoringQueryService monitoringQueryService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("reports", reportService.getAllReports());
        model.addAttribute("utilities", UtilityType.values());
        model.addAttribute("areaCount", areaRepository.count());
        model.addAttribute("powerStatus", monitoringQueryService.latestPower());
        model.addAttribute("internetStatus", monitoringQueryService.latestInternet());
        model.addAttribute("powerHistory", monitoringQueryService.recentPower());
        return "home";
    }
}
