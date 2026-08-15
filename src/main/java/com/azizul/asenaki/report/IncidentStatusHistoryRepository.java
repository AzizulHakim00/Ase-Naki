package com.azizul.asenaki.report;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentStatusHistoryRepository
        extends JpaRepository<IncidentStatusHistory, Long> {
}
