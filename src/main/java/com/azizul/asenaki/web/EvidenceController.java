package com.azizul.asenaki.web;

import com.azizul.asenaki.report.ReportEvidence;
import com.azizul.asenaki.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EvidenceController {

    private final ReportService reportService;

    @GetMapping("/evidence/{id}")
    public ResponseEntity<byte[]> showImage(@PathVariable Long id) {
        ReportEvidence evidence = reportService.getEvidence(id);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(evidence.getContentType()))
                .body(evidence.getFileData());
    }
}
