package com.azizul.asenaki.incident;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.report.UtilityType;
import com.azizul.asenaki.user.UserAccount;
import com.azizul.asenaki.user.UserService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidentService {

    static final Duration SIGNAL_CHANGE_COOLDOWN = Duration.ofMinutes(2);

    private final UtilityIncidentRepository incidentRepository;
    private final IncidentSignalRepository signalRepository;
    private final IncidentAggregationService aggregationService;
    private final UserService userService;

    @Transactional
    public UtilityIncident submitObservation(
            Area area,
            UtilityType utilityType,
            String provider,
            UserAccount user,
            IncidentSignalType signalType,
            LocalDateTime now) {
        validateIncidentUtility(utilityType);
        String normalizedProvider = UtilityProvider.normalize(utilityType, provider);
        Optional<UtilityIncident> current = findCompatibleActiveIncident(
                area, utilityType, normalizedProvider);

        if (current.isEmpty()) {
            if (!signalType.isAffected()) {
                throw new IllegalArgumentException(
                        "There is no active incident to update with a working status");
            }
            UtilityIncident created = new UtilityIncident();
            created.setArea(area);
            created.setUtilityType(utilityType);
            created.setProvider(normalizedProvider);
            created.setState(IncidentState.POSSIBLE_ISSUE);
            created.setConfidence(IncidentConfidence.LOW);
            created.setFirstSeenAt(now);
            created.setLastSignalAt(now);
            current = Optional.of(incidentRepository.save(created));
        }

        return applySignal(current.orElseThrow(), user, signalType, now);
    }

    @Transactional
    public UtilityIncident submitSignal(
            Long incidentId,
            String email,
            IncidentSignalType signalType) {
        return submitSignal(incidentId, email, signalType, LocalDateTime.now());
    }

    @Transactional
    UtilityIncident submitSignal(
            Long incidentId,
            String email,
            IncidentSignalType signalType,
            LocalDateTime now) {
        UtilityIncident incident = incidentRepository.findByIdAndDismissedFalse(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        ensureWritable(incident);
        UserAccount user = userService.findByEmail(email);
        return applySignal(incident, user, signalType, now);
    }

    @Transactional(readOnly = true)
    public Optional<UtilityIncident> findCompatibleActiveIncident(
            Area area,
            UtilityType utilityType,
            String provider) {
        String normalizedProvider = UtilityProvider.normalize(utilityType, provider);
        List<UtilityIncident> matches = normalizedProvider == null
                ? incidentRepository
                .findByAreaIdAndUtilityTypeAndProviderIsNullAndDismissedFalseOrderByLastSignalAtDesc(
                        area.getId(), utilityType)
                : incidentRepository
                .findByAreaIdAndUtilityTypeAndProviderIgnoreCaseAndDismissedFalseOrderByLastSignalAtDesc(
                        area.getId(), utilityType, normalizedProvider);

        return matches.stream()
                .filter(this::isOpen)
                .findFirst();
    }

    @Transactional
    public void dismiss(Long incidentId) {
        UtilityIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        incident.setDismissed(true);
        incidentRepository.save(incident);
    }

    @Transactional
    public void resolve(Long incidentId) {
        resolve(incidentId, LocalDateTime.now());
    }

    @Transactional
    void resolve(Long incidentId, LocalDateTime now) {
        UtilityIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        incident.setState(IncidentState.RESOLVED);
        incident.setConfidence(IncidentConfidence.HIGH);
        incident.setResolvedAt(now);
        incidentRepository.save(incident);
    }

    private UtilityIncident applySignal(
            UtilityIncident incident,
            UserAccount user,
            IncidentSignalType signalType,
            LocalDateTime now) {
        ensureWritable(incident);
        Optional<IncidentSignal> existing = signalRepository.findByIncidentIdAndUserId(
                incident.getId(), user.getId());

        if (existing.isPresent()) {
            IncidentSignal signal = existing.orElseThrow();
            if (signal.getSignalType() == signalType) {
                return incident;
            }
            if (signalType != IncidentSignalType.RESTORED
                    && signal.getUpdatedAt() != null
                    && Duration.between(signal.getUpdatedAt(), now)
                    .compareTo(SIGNAL_CHANGE_COOLDOWN) < 0) {
                throw new IllegalArgumentException(
                        "Please wait 2 minutes before changing your status again");
            }
            signal.setSignalType(signalType);
            signal.setUpdatedAt(now);
            signalRepository.save(signal);
        } else {
            IncidentSignal signal = new IncidentSignal();
            signal.setIncident(incident);
            signal.setUser(user);
            signal.setSignalType(signalType);
            signal.setCreatedAt(now);
            signal.setUpdatedAt(now);
            signalRepository.save(signal);
        }

        IncidentState previousState = incident.getState();
        incident.setLastSignalAt(now);
        IncidentAggregationResult result = aggregationService.calculate(
                previousState,
                incident.getLastSignalAt(),
                now,
                signalRepository.findAllByIncidentId(incident.getId()));
        incident.setState(result.state());
        incident.setConfidence(result.confidence());
        incident.setResolvedAt(result.resolved() ? now : null);
        return incidentRepository.save(incident);
    }

    private boolean isOpen(UtilityIncident incident) {
        return !incident.isDismissed()
                && incident.getResolvedAt() == null
                && incident.getState() != IncidentState.RESOLVED
                && incident.getState() != IncidentState.STALE;
    }

    private void ensureWritable(UtilityIncident incident) {
        if (!isOpen(incident)) {
            throw new IllegalArgumentException("This incident is closed for new status updates");
        }
    }

    private void validateIncidentUtility(UtilityType utilityType) {
        if (utilityType != UtilityType.ELECTRICITY
                && utilityType != UtilityType.BROADBAND
                && utilityType != UtilityType.MOBILE_NETWORK) {
            throw new IllegalArgumentException(
                    "This utility uses the detailed community report flow");
        }
    }
}
