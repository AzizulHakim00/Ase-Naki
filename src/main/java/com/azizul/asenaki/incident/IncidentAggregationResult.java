package com.azizul.asenaki.incident;

public record IncidentAggregationResult(
        IncidentState state,
        IncidentConfidence confidence,
        boolean resolved) {
}
