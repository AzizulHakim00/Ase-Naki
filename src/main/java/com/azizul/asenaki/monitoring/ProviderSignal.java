package com.azizul.asenaki.monitoring;

public record ProviderSignal(
        MonitoringState state,
        String summary,
        String affectedNetwork) {

    public static ProviderSignal unavailable(String summary) {
        return new ProviderSignal(MonitoringState.UNAVAILABLE, summary, null);
    }
}
