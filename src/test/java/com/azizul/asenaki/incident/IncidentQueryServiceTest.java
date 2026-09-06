package com.azizul.asenaki.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.UtilityType;
import com.azizul.asenaki.user.UserRepository;
import com.azizul.asenaki.user.UserService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IncidentQueryServiceTest {

    @Autowired
    private IncidentQueryService queryService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AreaRepository areaRepository;

    private Area mirpur;
    private Area uttara;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        mirpur = findArea("Mirpur 10");
        uttara = findArea("Uttara Sector 7");
        now = LocalDateTime.of(2026, 9, 6, 13, 0);
    }

    @Test
    void summaryCountsAffectedWorkingAndRestoredSignals() {
        var demo = userRepository.findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();
        var admin = userRepository.findByEmailIgnoreCase("admin@asenaki.bd").orElseThrow();

        UtilityIncident incident = incidentService.submitObservation(
                mirpur, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now);
        incidentService.submitObservation(
                mirpur, UtilityType.ELECTRICITY, null, admin,
                IncidentSignalType.WORKING_FOR_ME, now.plusMinutes(1));

        IncidentSummary summary = queryService.summary(incident.getId()).orElseThrow();

        assertThat(summary.affected()).isEqualTo(1);
        assertThat(summary.working()).isEqualTo(1);
        assertThat(summary.restored()).isZero();
        assertThat(summary.total()).isEqualTo(2);
        assertThat(summary.lastUpdated()).isEqualTo(now.plusMinutes(1));
    }

    @Test
    void preferredAreaIncidentsSortBeforeNewerOtherAreaIncidents() {
        var demo = userRepository.findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();
        var admin = userRepository.findByEmailIgnoreCase("admin@asenaki.bd").orElseThrow();

        UtilityIncident preferred = incidentService.submitObservation(
                mirpur, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now);
        UtilityIncident newer = incidentService.submitObservation(
                uttara, UtilityType.ELECTRICITY, null, admin,
                IncidentSignalType.SAME_PROBLEM, now.plusMinutes(10));
        userService.setPreferredArea(demo.getEmail(), mirpur.getId());

        var summaries = queryService.activeSummaries(demo.getEmail());

        assertThat(summaries).hasSize(2);
        assertThat(summaries.getFirst().incident().getId()).isEqualTo(preferred.getId());
        assertThat(summaries.get(1).incident().getId()).isEqualTo(newer.getId());
    }

    @Test
    void guestOrderingUsesNewestIncidentFirst() {
        var demo = userRepository.findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();
        var admin = userRepository.findByEmailIgnoreCase("admin@asenaki.bd").orElseThrow();

        UtilityIncident older = incidentService.submitObservation(
                mirpur, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now);
        UtilityIncident newer = incidentService.submitObservation(
                uttara, UtilityType.ELECTRICITY, null, admin,
                IncidentSignalType.SAME_PROBLEM, now.plusMinutes(10));

        var summaries = queryService.activeSummaries(null);

        assertThat(summaries.getFirst().incident().getId()).isEqualTo(newer.getId());
        assertThat(summaries.get(1).incident().getId()).isEqualTo(older.getId());
    }

    @Test
    void contributionCountUsesUniqueSignalRows() {
        var demo = userRepository.findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();

        incidentService.submitObservation(
                mirpur, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now);
        incidentService.submitObservation(
                uttara, UtilityType.ELECTRICITY, null, demo,
                IncidentSignalType.SAME_PROBLEM, now.plusMinutes(10));

        assertThat(queryService.contributionCount(demo.getEmail())).isEqualTo(2);
    }

    private Area findArea(String name) {
        return areaRepository.findAllByOrderByNameAsc().stream()
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
