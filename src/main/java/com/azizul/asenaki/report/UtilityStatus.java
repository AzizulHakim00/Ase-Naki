package com.azizul.asenaki.report;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UtilityStatus {
    AVAILABLE("Available", "status-available"),
    UNAVAILABLE("Unavailable", "status-unavailable"),
    LOW_PRESSURE("Low pressure", "status-warning"),
    UNSTABLE("Unstable", "status-warning"),
    MAINTENANCE("Maintenance", "status-warning");

    private final String label;
    private final String cssClass;
}
