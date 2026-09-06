package com.azizul.asenaki.incident;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IncidentAggregationServiceTest {

    private final IncidentAggregationService service = new IncidentAggregationService();
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 6, 13, 0);

    @Test
    void oneAffectedSignalIsPossibleIssueWithLowConfidence() {
        var result = service.calculate(
                IncidentState.POSSIBLE_ISSUE, now.minusMinutes(2), now,
                List.of(signal(IncidentSignalType.SAME_PROBLEM, now.minusMinutes(2))));

        assertThat(result.state()).isEqualTo(IncidentState.POSSIBLE_ISSUE);
        assertThat(result.confidence()).isEqualTo(IncidentConfidence.LOW);
        assertThat(result.resolved()).isFalse();
    }

    @Test
    void twoOrThreeAgreeingAffectedSignalsAreLikelyOutage() {
        for (int count : List.of(2, 3)) {
            var signals = repeated(IncidentSignalType.STILL_OUT, count, now.minusMinutes(3));

            var result = service.calculate(
                    IncidentState.POSSIBLE_ISSUE, now.minusMinutes(3), now, signals);

            assertThat(result.state()).isEqualTo(IncidentState.LIKELY_OUTAGE);
            assertThat(result.confidence()).isEqualTo(IncidentConfidence.MEDIUM);
        }
    }

    @Test
    void fourAffectedSignalsWithTwoToOneDominanceAreConfirmed() {
        var signals = new ArrayList<IncidentSignal>();
        signals.addAll(repeated(IncidentSignalType.SAME_PROBLEM, 4, now.minusMinutes(5)));
        signals.add(signal(IncidentSignalType.WORKING_FOR_ME, now.minusMinutes(4)));

        var result = service.calculate(
                IncidentState.LIKELY_OUTAGE, now.minusMinutes(4), now, signals);

        assertThat(result.state()).isEqualTo(IncidentState.CONFIRMED_OUTAGE);
        assertThat(result.confidence()).isEqualTo(IncidentConfidence.HIGH);
    }

    @Test
    void balancedAffectedAndWorkingSignalsAreMixed() {
        var signals = new ArrayList<IncidentSignal>();
        signals.addAll(repeated(IncidentSignalType.SAME_PROBLEM, 2, now.minusMinutes(5)));
        signals.addAll(repeated(IncidentSignalType.WORKING_FOR_ME, 2, now.minusMinutes(4)));

        var result = service.calculate(
                IncidentState.LIKELY_OUTAGE, now.minusMinutes(4), now, signals);

        assertThat(result.state()).isEqualTo(IncidentState.MIXED_REPORTS);
        assertThat(result.confidence()).isEqualTo(IncidentConfidence.MEDIUM);
    }

    @Test
    void twoRecoverySignalsAfterOutageReportRestoration() {
        var signals = new ArrayList<IncidentSignal>();
        signals.addAll(repeated(IncidentSignalType.SAME_PROBLEM, 3, now.minusMinutes(20)));
        signals.add(signal(IncidentSignalType.RESTORED, now.minusMinutes(3)));
        signals.add(signal(IncidentSignalType.RESTORED, now.minusMinutes(2)));

        var result = service.calculate(
                IncidentState.LIKELY_OUTAGE, now.minusMinutes(2), now, signals);

        assertThat(result.state()).isEqualTo(IncidentState.RESTORATION_REPORTED);
        assertThat(result.confidence()).isEqualTo(IncidentConfidence.MEDIUM);
    }

    @Test
    void newerAffectedSignalBlocksRestoration() {
        var signals = new ArrayList<IncidentSignal>();
        signals.add(signal(IncidentSignalType.RESTORED, now.minusMinutes(5)));
        signals.add(signal(IncidentSignalType.RESTORED, now.minusMinutes(4)));
        signals.add(signal(IncidentSignalType.STILL_OUT, now.minusMinutes(1)));

        var result = service.calculate(
                IncidentState.LIKELY_OUTAGE, now.minusMinutes(1), now, signals);

        assertThat(result.state()).isEqualTo(IncidentState.POSSIBLE_ISSUE);
    }

    @Test
    void recoveryAndWorkingDominanceCanResolveAfterRestorationStage() {
        var signals = new ArrayList<IncidentSignal>();
        signals.add(signal(IncidentSignalType.SAME_PROBLEM, now.minusMinutes(20)));
        signals.add(signal(IncidentSignalType.RESTORED, now.minusMinutes(5)));
        signals.add(signal(IncidentSignalType.RESTORED, now.minusMinutes(4)));
        signals.add(signal(IncidentSignalType.WORKING_FOR_ME, now.minusMinutes(3)));

        var result = service.calculate(
                IncidentState.RESTORATION_REPORTED, now.minusMinutes(3), now, signals);

        assertThat(result.state()).isEqualTo(IncidentState.RESOLVED);
        assertThat(result.resolved()).isTrue();
    }

    @Test
    void sixtyMinutesWithoutFreshSignalsBecomesStaleNotResolved() {
        var result = service.calculate(
                IncidentState.CONFIRMED_OUTAGE, now.minusMinutes(61), now,
                List.of(signal(IncidentSignalType.SAME_PROBLEM, now.minusMinutes(61))));

        assertThat(result.state()).isEqualTo(IncidentState.STALE);
        assertThat(result.confidence()).isEqualTo(IncidentConfidence.LOW);
        assertThat(result.resolved()).isFalse();
    }

    @Test
    void signalsOlderThanFortyFiveMinutesDoNotVote() {
        var signals = new ArrayList<IncidentSignal>();
        signals.addAll(repeated(IncidentSignalType.SAME_PROBLEM, 4, now.minusMinutes(46)));
        signals.add(signal(IncidentSignalType.SAME_PROBLEM, now.minusMinutes(5)));

        var result = service.calculate(
                IncidentState.POSSIBLE_ISSUE, now.minusMinutes(5), now, signals);

        assertThat(result.state()).isEqualTo(IncidentState.POSSIBLE_ISSUE);
        assertThat(result.confidence()).isEqualTo(IncidentConfidence.LOW);
    }

    private List<IncidentSignal> repeated(
            IncidentSignalType type, int count, LocalDateTime updatedAt) {
        List<IncidentSignal> signals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            signals.add(signal(type, updatedAt.plusSeconds(i)));
        }
        return signals;
    }

    private IncidentSignal signal(IncidentSignalType type, LocalDateTime updatedAt) {
        IncidentSignal signal = new IncidentSignal();
        signal.setSignalType(type);
        signal.setCreatedAt(updatedAt);
        signal.setUpdatedAt(updatedAt);
        return signal;
    }
}
