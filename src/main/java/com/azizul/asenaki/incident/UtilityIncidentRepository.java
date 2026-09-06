package com.azizul.asenaki.incident;

import com.azizul.asenaki.report.UtilityType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilityIncidentRepository extends JpaRepository<UtilityIncident, Long> {

    List<UtilityIncident> findAllByDismissedFalseOrderByLastSignalAtDesc();

    Optional<UtilityIncident> findByIdAndDismissedFalse(Long id);

    List<UtilityIncident> findByAreaIdAndUtilityTypeAndProviderIsNullAndDismissedFalseOrderByLastSignalAtDesc(
            Long areaId, UtilityType utilityType);

    List<UtilityIncident> findByAreaIdAndUtilityTypeAndProviderIgnoreCaseAndDismissedFalseOrderByLastSignalAtDesc(
            Long areaId, UtilityType utilityType, String provider);

    List<UtilityIncident> findTop20ByAreaIdOrderByLastSignalAtDesc(Long areaId);
}
