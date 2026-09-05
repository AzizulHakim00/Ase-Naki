package com.azizul.asenaki.monitoring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MonitoringState {
    NORMAL("Normal", "status-available"),
    POSSIBLE_DISRUPTION("Possible disruption", "status-warning"),
    LIKELY_DISRUPTION("Likely disruption", "status-unavailable"),
    UNAVAILABLE("Unavailable", "status-warning");

    private final String label;
    private final String cssClass;
}
