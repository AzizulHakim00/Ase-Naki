package com.azizul.asenaki.incident;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentSignalRepository extends JpaRepository<IncidentSignal, Long> {

    Optional<IncidentSignal> findByIncidentIdAndUserId(Long incidentId, Long userId);

    List<IncidentSignal> findAllByIncidentId(Long incidentId);

    long countByUserId(Long userId);
}
