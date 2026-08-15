package com.azizul.asenaki.report;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilityReportRepository
        extends JpaRepository<UtilityReport, Long> {

    List<UtilityReport> findAllByOrderByReportedAtDesc();

    boolean existsByDescription(String description);
}
