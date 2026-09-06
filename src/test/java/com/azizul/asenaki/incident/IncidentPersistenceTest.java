package com.azizul.asenaki.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.UtilityType;
import com.azizul.asenaki.user.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IncidentPersistenceTest {

    @Autowired
    private UtilityIncidentRepository incidentRepository;

    @Autowired
    private IncidentSignalRepository signalRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsOneIncidentWithUniqueSignalsFromDifferentUsers() {
        var area = areaRepository.findAllByOrderByNameAsc().stream()
                .filter(candidate -> candidate.getName().equals("Mirpur 10"))
                .findFirst()
                .orElseThrow();
        var demo = userRepository.findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();
        var admin = userRepository.findByEmailIgnoreCase("admin@asenaki.bd").orElseThrow();
        var now = LocalDateTime.of(2026, 9, 6, 13, 0);

        UtilityIncident incident = new UtilityIncident();
        incident.setArea(area);
        incident.setUtilityType(UtilityType.ELECTRICITY);
        incident.setState(IncidentState.POSSIBLE_ISSUE);
        incident.setConfidence(IncidentConfidence.LOW);
        incident.setFirstSeenAt(now);
        incident.setLastSignalAt(now);
        incident = incidentRepository.saveAndFlush(incident);

        IncidentSignal first = new IncidentSignal();
        first.setIncident(incident);
        first.setUser(demo);
        first.setSignalType(IncidentSignalType.SAME_PROBLEM);
        first.setCreatedAt(now);
        first.setUpdatedAt(now);
        signalRepository.saveAndFlush(first);

        IncidentSignal second = new IncidentSignal();
        second.setIncident(incident);
        second.setUser(admin);
        second.setSignalType(IncidentSignalType.WORKING_FOR_ME);
        second.setCreatedAt(now.plusMinutes(1));
        second.setUpdatedAt(now.plusMinutes(1));
        signalRepository.saveAndFlush(second);

        assertThat(incident.getId()).isNotNull();
        assertThat(signalRepository.findAllByIncidentId(incident.getId())).hasSize(2);
        assertThat(signalRepository.findByIncidentIdAndUserId(
                incident.getId(), demo.getId())).contains(first);
    }
}
