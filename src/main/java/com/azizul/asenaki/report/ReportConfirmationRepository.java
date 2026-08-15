package com.azizul.asenaki.report;

import com.azizul.asenaki.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportConfirmationRepository
        extends JpaRepository<ReportConfirmation, Long> {

    Optional<ReportConfirmation> findByReportAndUser(
            UtilityReport report, UserAccount user);

    long countByReportAndChoice(
            UtilityReport report, ConfirmationChoice choice);
}
