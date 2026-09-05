package com.azizul.asenaki.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MonitoringAggregationServiceTest {

    private final MonitoringAggregationService service =
            new MonitoringAggregationService();

    @Test
    void bothNormalSourcesProduceNormalStatus() {
        var result = service.aggregate(
                signal(MonitoringState.NORMAL),
                signal(MonitoringState.NORMAL));

        assertThat(result).isEqualTo(MonitoringState.NORMAL);
    }

    @Test
    void oneDisruptionSignalProducesPossibleDisruption() {
        var result = service.aggregate(
                signal(MonitoringState.POSSIBLE_DISRUPTION),
                signal(MonitoringState.NORMAL));

        assertThat(result).isEqualTo(MonitoringState.POSSIBLE_DISRUPTION);
    }

    @Test
    void twoDisruptionSignalsProduceLikelyDisruption() {
        var result = service.aggregate(
                signal(MonitoringState.POSSIBLE_DISRUPTION),
                signal(MonitoringState.POSSIBLE_DISRUPTION));

        assertThat(result).isEqualTo(MonitoringState.LIKELY_DISRUPTION);
    }

    @Test
    void oneUnavailableSourceDoesNotEraseAnExplicitNormalSignal() {
        var result = service.aggregate(
                signal(MonitoringState.UNAVAILABLE),
                signal(MonitoringState.NORMAL));

        assertThat(result).isEqualTo(MonitoringState.NORMAL);
    }

    @Test
    void twoUnavailableSourcesProduceUnavailableStatus() {
        var result = service.aggregate(
                signal(MonitoringState.UNAVAILABLE),
                signal(MonitoringState.UNAVAILABLE));

        assertThat(result).isEqualTo(MonitoringState.UNAVAILABLE);
    }

    private ProviderSignal signal(MonitoringState state) {
        return new ProviderSignal(state, state.name(), null);
    }
}
