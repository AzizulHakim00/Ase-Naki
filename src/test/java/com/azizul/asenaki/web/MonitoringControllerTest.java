package com.azizul.asenaki.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.azizul.asenaki.monitoring.MonitoringRefreshService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MonitoringControllerTest {

    @Test
    void wrongSecretIsForbidden() {
        var refreshService = mock(MonitoringRefreshService.class);
        var controller = new MonitoringController(refreshService, "correct-secret");

        var response = controller.refresh("wrong-secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(refreshService, never()).refreshAll();
    }

    @Test
    void correctSecretTriggersRefresh() {
        var refreshService = mock(MonitoringRefreshService.class);
        var controller = new MonitoringController(refreshService, "correct-secret");

        var response = controller.refresh("correct-secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(refreshService).refreshAll();
    }

    @Test
    void blankConfiguredSecretAlwaysRejectsRequests() {
        var refreshService = mock(MonitoringRefreshService.class);
        var controller = new MonitoringController(refreshService, "");

        var response = controller.refresh("");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(refreshService, never()).refreshAll();
    }
}
