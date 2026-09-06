package com.azizul.asenaki.incident;

import com.azizul.asenaki.user.UserAccount;
import com.azizul.asenaki.user.UserService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidentQueryService {

    private final UtilityIncidentRepository incidentRepository;
    private final IncidentSignalRepository signalRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Optional<IncidentSummary> summary(Long incidentId) {
        return incidentRepository.findById(incidentId).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public List<IncidentSummary> activeSummaries(String emailOrNull) {
        Long preferredAreaId = preferredAreaId(emailOrNull);
        Comparator<UtilityIncident> comparator = Comparator
                .comparing((UtilityIncident incident) -> !matchesPreferred(
                        incident, preferredAreaId))
                .thenComparing(
                        UtilityIncident::getLastSignalAt,
                        Comparator.nullsLast(Comparator.reverseOrder()));

        return incidentRepository.findAllByDismissedFalseOrderByLastSignalAtDesc().stream()
                .filter(this::isActive)
                .sorted(comparator)
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public long contributionCount(String email) {
        UserAccount user = userService.findByEmail(email);
        return signalRepository.countByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public List<UtilityIncident> recentAreaHistory(Long areaId, int limit) {
        int safeLimit = Math.max(0, Math.min(limit, 20));
        return incidentRepository.findTop20ByAreaIdOrderByLastSignalAtDesc(areaId).stream()
                .limit(safeLimit)
                .toList();
    }

    private IncidentSummary toSummary(UtilityIncident incident) {
        LocalDateTime freshCutoff = LocalDateTime.now()
                .minus(IncidentAggregationService.FRESH_SIGNAL_WINDOW);
        List<IncidentSignal> fresh = signalRepository.findAllByIncidentId(incident.getId())
                .stream()
                .filter(signal -> signal.getUpdatedAt() != null)
                .filter(signal -> !signal.getUpdatedAt().isBefore(freshCutoff))
                .toList();

        long affected = fresh.stream()
                .filter(signal -> signal.getSignalType().isAffected())
                .count();
        long working = fresh.stream()
                .filter(signal -> signal.getSignalType().isWorking())
                .count();
        long restored = fresh.stream()
                .filter(signal -> signal.getSignalType().isRecovery())
                .count();
        LocalDateTime lastUpdated = fresh.stream()
                .map(IncidentSignal::getUpdatedAt)
                .max(Comparator.naturalOrder())
                .orElse(incident.getLastSignalAt());

        return new IncidentSummary(
                incident,
                affected,
                working,
                restored,
                fresh.size(),
                lastUpdated);
    }

    private Long preferredAreaId(String emailOrNull) {
        if (emailOrNull == null || emailOrNull.isBlank()) {
            return null;
        }
        UserAccount user = userService.findByEmail(emailOrNull);
        if (user.getProfile() == null || user.getProfile().getPreferredArea() == null) {
            return null;
        }
        return user.getProfile().getPreferredArea().getId();
    }

    private boolean matchesPreferred(UtilityIncident incident, Long preferredAreaId) {
        return preferredAreaId != null
                && incident.getArea().getId().equals(preferredAreaId);
    }

    private boolean isActive(UtilityIncident incident) {
        return incident.getResolvedAt() == null
                && incident.getState() != IncidentState.RESOLVED
                && incident.getState() != IncidentState.STALE;
    }
}
