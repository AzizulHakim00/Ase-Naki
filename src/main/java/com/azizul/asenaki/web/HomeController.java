package com.azizul.asenaki.web;

import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.ReportService;
import com.azizul.asenaki.report.ReportState;
import com.azizul.asenaki.report.UtilityReportRepository;
import com.azizul.asenaki.report.UtilityTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ReportService reportService;
    private final UtilityReportRepository reportRepository;
    private final AreaRepository areaRepository;
    private final UtilityTypeRepository utilityTypeRepository;

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Long utilityTypeId,
            Model model) {
        model.addAttribute(
                "reports", reportService.findActive(areaId, utilityTypeId));
        model.addAttribute("areas", areaRepository.findAllWithLocation());
        model.addAttribute("utilities", utilityTypeRepository.findAll());
        model.addAttribute("selectedAreaId", areaId);
        model.addAttribute("selectedUtilityTypeId", utilityTypeId);
        model.addAttribute(
                "activeCount", reportRepository.countByState(ReportState.ACTIVE));
        model.addAttribute("areaCount", areaRepository.count());
        return "home";
    }
}
