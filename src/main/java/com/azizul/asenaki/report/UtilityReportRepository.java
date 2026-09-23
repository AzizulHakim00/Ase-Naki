package com.azizul.asenaki.report;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UtilityReportRepository extends MongoRepository<UtilityReport, String> {
    List<UtilityReport> findAllByOrderByReportedAtDesc();
    List<UtilityReport> findAllByReporterEmailIgnoreCaseOrderByReportedAtDesc(String reporterEmail);
    boolean existsByDescription(String description);
}
