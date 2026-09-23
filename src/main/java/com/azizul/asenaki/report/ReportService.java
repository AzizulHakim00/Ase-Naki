package com.azizul.asenaki.report;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.user.UserAccount;
import com.azizul.asenaki.user.UserService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UtilityReportRepository reportRepository;
    private final AreaRepository areaRepository;
    private final UserService userService;

    public List<UtilityReport> getAllReports() {
        return reportRepository.findAllByOrderByReportedAtDesc();
    }

    public List<UtilityReport> getReportsForUser(String email) {
        return reportRepository.findAllByReporterEmailIgnoreCaseOrderByReportedAtDesc(email);
    }

    public UtilityReport getReport(String id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }

    public UtilityReport saveReport(ReportForm form, String email) {
        UserAccount user = userService.findByEmail(email);
        Area area = findArea(form.getAreaId());

        UtilityReport report = new UtilityReport();
        applyForm(report, form, area);
        report.setReporterId(user.getId());
        report.setReporterName(user.getName());
        report.setReporterEmail(user.getEmail());
        report.setReportedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    public UtilityReport updateReport(String id, ReportForm form, String email, boolean admin) {
        UtilityReport report = getReport(id);
        assertCanManage(report, email, admin);
        applyForm(report, form, findArea(form.getAreaId()));
        report.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    public void deleteReport(String id, String email, boolean admin) {
        UtilityReport report = getReport(id);
        assertCanManage(report, email, admin);
        reportRepository.delete(report);
    }

    public boolean canManage(UtilityReport report, String email, boolean admin) {
        return admin || (email != null
                && report.getReporterEmail() != null
                && report.getReporterEmail().equalsIgnoreCase(email));
    }

    public ReportForm toForm(UtilityReport report) {
        ReportForm form = new ReportForm();
        form.setAreaId(report.getArea().getId());
        form.setUtilityType(report.getUtilityType());
        form.setStatus(report.getStatus());
        form.setDescription(report.getDescription());
        return form;
    }

    private void assertCanManage(UtilityReport report, String email, boolean admin) {
        if (!canManage(report, email, admin)) {
            throw new AccessDeniedException("You cannot modify this report");
        }
    }

    private Area findArea(String id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Area not found"));
    }

    private void applyForm(UtilityReport report, ReportForm form, Area area) {
        report.setArea(area);
        report.setUtilityType(form.getUtilityType());
        report.setStatus(form.getStatus());
        report.setDescription(form.getDescription().trim());
    }
}
