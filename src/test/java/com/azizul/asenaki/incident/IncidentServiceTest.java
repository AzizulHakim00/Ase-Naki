package com.azizul.asenaki.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.UtilityType;
import com.azizul.asenaki.user.UserAccount;
import com.azizul.asenaki.user.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IncidentServiceTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UtilityIncidentRepository incidentRepository;

    @Autowired
    private IncidentSignalRepository signalRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private UserRepository userRepository;

    private Area area;
    private UserAccount demo;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        area = areaRepository.findAllByOrderByNameAsc().stream()
                .filter(candidate -> candidate.getName().equals("Mirpur 10"))
                .findFirst()
                .orElseThrow();
        demo = userRepository.findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();
        now = LocalDateTime.of(2026, 9, 6, 13, 0);
    }

    @Test
    void firstAffectedObservationCreatesIncidentAndOneSignal() {
        UtilityIncident incident = incidentService.submitObservation(
                area, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now);

        assertThat(incident.getId()).isNotNull();
        assertThat(incident.getState()).isEqualTo(IncidentState.POSSIBLE_ISSUE);
        assertThat(incident.getConfidence()).isEqualTo(IncidentConfidence.LOW);
        assertThat(signalRepository.findAllByIncidentId(incident.getId())).hasSize(1);
    }

    @Test
    void sameUserUpdatesExistingSignalInsteadOfDoubleCounting() {
        UtilityIncident incident = incidentService.submitObservation(
                area, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now);

        incidentService.submitObservation(
                area, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.STILL_OUT, now.plusMinutes(3));

        var signals = signalRepository.findAllByIncidentId(incident.getId());
        assertThat(signals).hasSize(1);
        assertThat(signals.getFirst().getSignalType()).isEqualTo(IncidentSignalType.STILL_OUT);
        assertThat(signals.getFirst().getUpdatedAt()).isEqualTo(now.plusMinutes(3));
    }

    @Test
    void changingSignalInsideTwoMinutesIsRejected() {
        incidentService.submitObservation(
                area, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now);

        assertThatThrownBy(() -> incidentService.submitObservation(
                area, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.WORKING_FOR_ME, now.plusMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 minutes");
    }

    @Test
    void immediateRestoredTransitionBypassesCooldown() {
        UtilityIncident incident = incidentService.submitObservation(
                area, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now);

        incidentService.submitObservation(
                area, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.RESTORED, now.plusSeconds(30));

        var signal = signalRepository.findByIncidentIdAndUserId(
                incident.getId(), demo.getId()).orElseThrow();
        assertThat(signal.getSignalType()).isEqualTo(IncidentSignalType.RESTORED);
    }

    @Test
    void workingOnlyObservationCannotCreateOutageIncident() {
        assertThatThrownBy(() -> incidentService.submitObservation(
                area, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.WORKING_FOR_ME, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active incident");

        assertThat(incidentRepository.findAllByDismissedFalseOrderByLastSignalAtDesc())
                .isEmpty();
    }

    @Test
    void providerSpecificBroadbandIncidentStaysSeparateFromAreaWideIncident() {
        UtilityIncident link3 = incidentService.submitObservation(
                area, UtilityType.BROADBAND, "Link3", demo,
                IncidentSignalType.SAME_PROBLEM, now);

        UtilityIncident areaWide = incidentService.submitObservation(
                area, UtilityType.BROADBAND, null, demo,
                IncidentSignalType.SAME_PROBLEM, now.plusMinutes(3));

        assertThat(link3.getId()).isNotEqualTo(areaWide.getId());
        assertThat(link3.getProvider()).isEqualTo("LINK3");
        assertThat(areaWide.getProvider()).isNull();
    }

    @Test
    void resolvedAndDismissedIncidentsAreNotWritableThroughNormalSignalRoute() {
        UtilityIncident incident = incidentService.submitObservation(
                area, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now);

        incidentService.resolve(incident.getId(), now.plusMinutes(5));
        assertThatThrownBy(() -> incidentService.submitSignal(
                incident.getId(), demo.getEmail(), IncidentSignalType.STILL_OUT,
                now.plusMinutes(6)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("closed");

        UtilityIncident another = incidentService.submitObservation(
                area, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now.plusMinutes(10));
        incidentService.dismiss(another.getId());
        assertThatThrownBy(() -> incidentService.submitSignal(
                another.getId(), demo.getEmail(), IncidentSignalType.STILL_OUT,
                now.plusMinutes(13)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
