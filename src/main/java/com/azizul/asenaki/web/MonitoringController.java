package com.azizul.asenaki.web;

import com.azizul.asenaki.monitoring.MonitoringRefreshService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitoringController {

    private final MonitoringRefreshService refreshService;
    private final String refreshSecret;

    public MonitoringController(
            MonitoringRefreshService refreshService,
            @Value("${app.monitoring.refresh-secret:${MONITORING_REFRESH_SECRET:}}") String refreshSecret) {
        this.refreshService = refreshService;
        this.refreshSecret = refreshSecret == null ? "" : refreshSecret;
    }

    @PostMapping("/internal/monitoring/refresh")
    public ResponseEntity<Void> refresh(
            @RequestHeader(value = "X-Monitoring-Secret", required = false)
            String suppliedSecret) {
        if (!validSecret(suppliedSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        refreshService.refreshAll();
        return ResponseEntity.noContent().build();
    }

    private boolean validSecret(String suppliedSecret) {
        if (refreshSecret.isBlank() || suppliedSecret == null) {
            return false;
        }
        return MessageDigest.isEqual(
                refreshSecret.getBytes(StandardCharsets.UTF_8),
                suppliedSecret.getBytes(StandardCharsets.UTF_8));
    }
}
