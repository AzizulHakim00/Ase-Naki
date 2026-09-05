package com.azizul.asenaki.monitoring;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PowerSnapshotRepository extends JpaRepository<PowerSnapshot, Long> {
    Optional<PowerSnapshot> findTopByOrderByFetchedAtDesc();
    List<PowerSnapshot> findTop24ByOrderByObservedAtDesc();
}
