package com.azizul.asenaki.monitoring;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonitoringQueryService {

    private final PowerSnapshotRepository powerSnapshotRepository;
    private final InternetStatusSnapshotRepository internetStatusSnapshotRepository;

    @Transactional(readOnly = true)
    public Optional<PowerSnapshot> latestPower() {
        return powerSnapshotRepository.findTopByOrderByFetchedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<InternetStatusSnapshot> latestInternet() {
        return internetStatusSnapshotRepository.findTopByOrderByCheckedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<PowerSnapshot> recentPower() {
        return powerSnapshotRepository.findTop24ByOrderByObservedAtDesc();
    }
}
