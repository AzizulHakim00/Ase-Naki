package com.azizul.asenaki.incident;

import java.time.LocalDateTime;

public record IncidentSummary(
        UtilityIncident incident,
        long affected,
        long working,
        long restored,
        long total,
        LocalDateTime lastUpdated) {
}
