package com.azizul.asenaki.report;

import com.azizul.asenaki.common.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final ReportEvidenceRepository evidenceRepository;

    public ReportEvidence buildEvidence(
            MultipartFile file, UtilityReport report) {
        try {
            ReportEvidence evidence = new ReportEvidence();
            evidence.setReport(report);
            evidence.setOriginalFileName(safeFileName(file.getOriginalFilename()));
            evidence.setContentType(file.getContentType());
            evidence.setFileSize(file.getSize());
            evidence.setFileData(file.getBytes());
            return evidence;
        } catch (IOException exception) {
            throw new AppException("The evidence file could not be read");
        }
    }

    @Transactional(readOnly = true)
    public ReportEvidence findById(Long id) {
        return evidenceRepository.findById(id)
                .orElseThrow(() -> new AppException("Evidence file not found"));
    }

    private String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "evidence";
        }
        return originalName.replace("\\", "_").replace("/", "_");
    }
}
