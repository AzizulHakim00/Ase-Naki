package com.azizul.asenaki.report;

import com.azizul.asenaki.common.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReportBusinessRulesTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UtilityReportRepository reportRepository;

    @Test
    void reporterCannotVoteOnOwnReport() {
        UtilityReport report = reportRepository.findAllWithDetails().getFirst();

        assertThatThrownBy(() -> reportService.vote(
                report.getId(),
                ConfirmationChoice.CONFIRM,
                report.getReporter().getEmail()
        ))
                .isInstanceOf(AppException.class)
                .hasMessage("You cannot vote on your own report");
    }

    @Test
    void duplicateRecentReportIsBlocked() {
        UtilityReport existing = reportRepository.findAllWithDetails().getFirst();
        ReportForm form = new ReportForm();
        form.setAreaId(existing.getArea().getId());
        form.setUtilityTypeId(existing.getUtilityType().getId());
        form.setStatus(existing.getStatus());

        assertThatThrownBy(() -> reportService.create(
                form, existing.getReporter().getEmail()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Please wait");
    }
}
