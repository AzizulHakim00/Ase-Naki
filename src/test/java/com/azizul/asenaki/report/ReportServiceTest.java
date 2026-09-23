package com.azizul.asenaki.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.user.UserService;
import org.junit.jupiter.api.Test;

class ReportServiceTest {

    private final ReportService service = new ReportService(
            mock(UtilityReportRepository.class),
            mock(AreaRepository.class),
            mock(UserService.class));

    @Test
    void ownerAndAdminCanManageReport() {
        UtilityReport report = new UtilityReport();
        report.setReporterEmail("demo@asenaki.bd");

        assertThat(service.canManage(report, "demo@asenaki.bd", false)).isTrue();
        assertThat(service.canManage(report, "other@example.com", true)).isTrue();
        assertThat(service.canManage(report, "other@example.com", false)).isFalse();
    }
}
