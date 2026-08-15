package com.azizul.asenaki.report;

public enum UtilityStatus {
    AVAILABLE("Available"),
    UNAVAILABLE("Unavailable"),
    LOW_PRESSURE("Low pressure"),
    UNSTABLE("Unstable"),
    SLOW("Slow"),
    PARTIAL_OUTAGE("Partial outage"),
    MAINTENANCE("Maintenance"),
    RESTORED("Restored");

    private final String label;

    UtilityStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return switch (this) {
            case AVAILABLE, RESTORED -> "status-good";
            case UNAVAILABLE, PARTIAL_OUTAGE -> "status-bad";
            default -> "status-warning";
        };
    }
}
