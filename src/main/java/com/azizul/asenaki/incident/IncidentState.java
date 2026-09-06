package com.azizul.asenaki.incident;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IncidentState {
    POSSIBLE_ISSUE("Possible issue", "status-warning"),
    LIKELY_OUTAGE("Likely outage", "status-warning"),
    CONFIRMED_OUTAGE("Confirmed outage", "status-unavailable"),
    MIXED_REPORTS("Mixed reports", "status-warning"),
    RESTORATION_REPORTED("Restoration reported", "status-warning"),
    RESOLVED("Resolved", "status-available"),
    STALE("Stale", "status-warning");

    private final String label;
    private final String cssClass;
}
