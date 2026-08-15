package com.azizul.asenaki.report;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UtilityReportRepository
        extends JpaRepository<UtilityReport, Long> {

    @Query("""
            select distinct report from UtilityReport report
            join fetch report.reporter reporter
            join fetch reporter.profile
            join fetch report.area area
            join fetch area.thana thana
            join fetch thana.district district
            join fetch report.utilityType
            where report.state = :state
              and (:areaId is null or area.id = :areaId)
              and (:utilityTypeId is null or report.utilityType.id = :utilityTypeId)
            order by report.reportedAt desc
            """)
    List<UtilityReport> findCards(
            @Param("state") ReportState state,
            @Param("areaId") Long areaId,
            @Param("utilityTypeId") Long utilityTypeId
    );

    @Query("""
            select distinct report from UtilityReport report
            join fetch report.reporter reporter
            join fetch reporter.profile
            join fetch report.area area
            join fetch area.thana thana
            join fetch thana.district district
            join fetch district.division
            join fetch report.utilityType
            where report.id = :id
            """)
    Optional<UtilityReport> findDetailedById(@Param("id") Long id);

    boolean existsByReporterAndAreaAndUtilityTypeAndReportedAtAfterAndState(
            UserAccount reporter,
            Area area,
            UtilityType utilityType,
            LocalDateTime reportedAfter,
            ReportState state
    );

    List<UtilityReport> findByStateAndExpiresAtBefore(
            ReportState state, LocalDateTime now);

    long countByState(ReportState state);

    @Query("""
            select distinct report from UtilityReport report
            join fetch report.reporter reporter
            join fetch reporter.profile
            join fetch report.area area
            join fetch area.thana
            join fetch report.utilityType
            order by report.reportedAt desc
            """)
    List<UtilityReport> findAllWithDetails();
}
