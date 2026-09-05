package com.azizul.asenaki.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MonitoringPersistenceTest {

    @Autowired
    private PowerSnapshotRepository powerRepository;

    @Autowired
    private InternetStatusSnapshotRepository internetRepository;

    @Test
    void repositoriesReturnLatestSavedSnapshots() {
        PowerSnapshot power = new PowerSnapshot();
        power.setObservedAt(LocalDateTime.of(2026, 9, 6, 1, 0));
        power.setFetchedAt(LocalDateTime.of(2026, 9, 6, 1, 5));
        power.setDemandMw(15000);
        power.setSupplyMw(14500);
        power.setLoadSheddingMw(500);
        power.setSource("Power Grid Bangladesh PLC");
        powerRepository.saveAndFlush(power);

        InternetStatusSnapshot internet = new InternetStatusSnapshot();
        internet.setCheckedAt(LocalDateTime.of(2026, 9, 6, 1, 6));
        internet.setCloudflareState(MonitoringState.NORMAL);
        internet.setIodaState(MonitoringState.NORMAL);
        internet.setOverallState(MonitoringState.NORMAL);
        internet.setSummary("No major disruption detected.");
        internetRepository.saveAndFlush(internet);

        assertThat(powerRepository.findTopByOrderByFetchedAtDesc())
                .contains(power);
        assertThat(internetRepository.findTopByOrderByCheckedAtDesc())
                .contains(internet);
    }
}
