package com.azizul.asenaki.monitoring;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InternetStatusSnapshotRepository
        extends JpaRepository<InternetStatusSnapshot, Long> {
    Optional<InternetStatusSnapshot> findTopByOrderByCheckedAtDesc();
}
