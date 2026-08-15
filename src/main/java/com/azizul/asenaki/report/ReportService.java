package com.azizul.asenaki.report;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.user.UserAccount;
import com.azizul.asenaki.user.UserService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final UtilityReportRepository reportRepository;
    private final ReportEvidenceRepository evidenceRepository;
    private final AreaRepository areaRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<UtilityReport> getAllReports() {
        return reportRepository.findAllByOrderByReportedAtDesc();
    }

    @Transactional(readOnly = true)
    public UtilityReport getReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }

    @Transactional
    public UtilityReport saveReport(ReportForm form, String email) {
        UserAccount user = userService.findByEmail(email);
        Area area = areaRepository.findById(form.getAreaId())
                .orElseThrow(() -> new IllegalArgumentException("Area not found"));

        UtilityReport report = new UtilityReport();
        report.setArea(area);
        report.setReporter(user);
        report.setUtilityType(form.getUtilityType());
        report.setStatus(form.getStatus());
        report.setDescription(form.getDescription().trim());
        report.setReportedAt(LocalDateTime.now());

        MultipartFile image = form.getEvidence();
        if (image != null && !image.isEmpty()) {
            validateImage(image);
            report.addEvidence(createEvidence(image));
        }

        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public ReportEvidence getEvidence(Long id) {
        return evidenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
    }

    private void validateImage(MultipartFile image) {
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image must be 5 MB or smaller");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
            throw new IllegalArgumentException(
                    "Upload a JPG, PNG, or WebP image");
        }
    }

    private ReportEvidence createEvidence(MultipartFile image) {
        try {
            ReportEvidence evidence = new ReportEvidence();
            evidence.setFileName(image.getOriginalFilename());
            evidence.setContentType(image.getContentType());
            evidence.setFileData(image.getBytes());
            return evidence;
        } catch (IOException exception) {
            throw new IllegalArgumentException("The image could not be read");
        }
    }
}
