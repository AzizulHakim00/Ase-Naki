package com.azizul.asenaki.incident;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IncidentAggregationService {

    static final Duration FRESH_SIGNAL_WINDOW = Duration.ofMinutes(45);
    static final Duration STALE_INCIDENT_AFTER = Duration.ofMinutes(60);

    public IncidentAggregationResult calculate(
            IncidentState previousState,
            LocalDateTime lastSignalAt,
            LocalDateTime now,
            List<IncidentSignal> signals) {
        LocalDateTime freshCutoff = now.minus(FRESH_SIGNAL_WINDOW);
        List<IncidentSignal> fresh = signals.stream()
                .filter(signal -> signal.getUpdatedAt() != null)
                .filter(signal -> !signal.getUpdatedAt().isBefore(freshCutoff))
                .toList();

        if (fresh.isEmpty()
                && lastSignalAt != null
                && !lastSignalAt.isAfter(now.minus(STALE_INCIDENT_AFTER))) {
            return result(IncidentState.STALE, IncidentConfidence.LOW, false);
        }

        long affected = fresh.stream()
                .filter(signal -> signal.getSignalType().isAffected())
                .count();
        long working = fresh.stream()
                .filter(signal -> signal.getSignalType().isWorking())
                .count();
        long restored = fresh.stream()
                .filter(signal -> signal.getSignalType().isRecovery())
                .count();

        Optional<LocalDateTime> newestAffected = newestTime(
                fresh.stream()
                        .filter(signal -> signal.getSignalType().isAffected())
                        .toList());
        Optional<LocalDateTime> newestRecovery = newestTime(
                fresh.stream()
                        .filter(signal -> signal.getSignalType().isRecovery())
                        .toList());
        Optional<LocalDateTime> newestHealthy = newestTime(
                fresh.stream()
                        .filter(signal -> signal.getSignalType().isRecovery()
                                || signal.getSignalType().isWorking())
                        .toList());

        if (previouslyHadOutage(previousState)
                && restored >= 2
                && newerThanAffected(newestRecovery, newestAffected)) {
            IncidentConfidence confidence = restored >= 4
                    ? IncidentConfidence.HIGH : IncidentConfidence.MEDIUM;
            return result(
                    IncidentState.RESTORATION_REPORTED, confidence, false);
        }

        long healthy = restored + working;
        if (healthy >= 3
                && healthy > affected
                && newerThanOrEqualToAffected(newestHealthy, newestAffected)) {
            return result(IncidentState.RESOLVED, IncidentConfidence.HIGH, true);
        }

        if (affected >= 2 && working >= 2
                && affected < 2 * working
                && working < 2 * affected) {
            return result(
                    IncidentState.MIXED_REPORTS, IncidentConfidence.MEDIUM, false);
        }

        if (affected >= 4 && affected >= 2 * Math.max(working, 1)) {
            return result(
                    IncidentState.CONFIRMED_OUTAGE, IncidentConfidence.HIGH, false);
        }

        if ((affected == 2 || affected == 3) && affected > working) {
            return result(
                    IncidentState.LIKELY_OUTAGE, IncidentConfidence.MEDIUM, false);
        }

        if (affected >= 1) {
            return result(
                    IncidentState.POSSIBLE_ISSUE, IncidentConfidence.LOW, false);
        }

        if (previousState == IncidentState.RESOLVED) {
            return result(IncidentState.RESOLVED, IncidentConfidence.HIGH, true);
        }
        if (previousState == IncidentState.STALE) {
            return result(IncidentState.STALE, IncidentConfidence.LOW, false);
        }
        return result(IncidentState.POSSIBLE_ISSUE, IncidentConfidence.LOW, false);
    }

    private Optional<LocalDateTime> newestTime(List<IncidentSignal> signals) {
        return signals.stream()
                .map(IncidentSignal::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder());
    }

    private boolean previouslyHadOutage(IncidentState state) {
        return state == IncidentState.LIKELY_OUTAGE
                || state == IncidentState.CONFIRMED_OUTAGE
                || state == IncidentState.MIXED_REPORTS;
    }

    private boolean newerThanAffected(
            Optional<LocalDateTime> candidate,
            Optional<LocalDateTime> affected) {
        return candidate.isPresent()
                && (affected.isEmpty()
                || candidate.orElseThrow().isAfter(affected.orElseThrow()));
    }

    private boolean newerThanOrEqualToAffected(
            Optional<LocalDateTime> candidate,
            Optional<LocalDateTime> affected) {
        return candidate.isPresent()
                && (affected.isEmpty()
                || !affected.orElseThrow().isAfter(candidate.orElseThrow()));
    }

    private IncidentAggregationResult result(
            IncidentState state,
            IncidentConfidence confidence,
            boolean resolved) {
        return new IncidentAggregationResult(state, confidence, resolved);
    }
}
