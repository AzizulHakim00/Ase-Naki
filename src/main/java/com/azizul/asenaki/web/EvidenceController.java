package com.azizul.asenaki.web;

import com.azizul.asenaki.report.EvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@RequestMapping("/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> view(@PathVariable Long id) {
        var evidence = evidenceService.findById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                evidence.getContentType()));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(evidence.getOriginalFileName(), StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(evidence.getFileData());
    }
}
