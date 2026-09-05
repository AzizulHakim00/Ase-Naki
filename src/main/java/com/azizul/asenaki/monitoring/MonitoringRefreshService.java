package com.azizul.asenaki.monitoring;

import com.azizul.asenaki.monitoring.external.CloudflareRadarService;
import com.azizul.asenaki.monitoring.external.IodaService;
import com.azizul.asenaki.monitoring.external.PowerGridService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonitoringRefreshService {

    private final PowerGridService powerGridService;
    private final CloudflareRadarService cloudflareRadarService;
    private final IodaService iodaService;
    private final PowerSnapshotRepository powerSnapshotRepository;
    private final InternetStatusSnapshotRepository internetStatusSnapshotRepository;
    private final MonitoringAggregationService aggregationService;

    @Transactional
    public void refreshAll() {
        powerGridService.fetchLatest().ifPresent(powerSnapshotRepository::save);

        ProviderSignal cloudflare = cloudflareRadarService.checkBangladesh();
        ProviderSignal ioda = iodaService.checkBangladesh();
        MonitoringState overall = aggregationService.aggregate(cloudflare, ioda);

        InternetStatusSnapshot snapshot = new InternetStatusSnapshot();
        snapshot.setCheckedAt(LocalDateTime.now());
        snapshot.setCloudflareState(cloudflare.state());
        snapshot.setIodaState(ioda.state());
        snapshot.setOverallState(overall);
        snapshot.setSummary(aggregationService.summarize(cloudflare, ioda, overall));
        snapshot.setAffectedNetwork(firstNonBlank(
                cloudflare.affectedNetwork(), ioda.affectedNetwork()));
        internetStatusSnapshotRepository.save(snapshot);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }
}
