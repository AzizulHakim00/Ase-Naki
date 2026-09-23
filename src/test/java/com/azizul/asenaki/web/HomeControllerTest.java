package com.azizul.asenaki.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.ReportService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

class HomeControllerTest {

    @Test
    void homepageAddsMongoCrudData() {
        var reportService = mock(ReportService.class);
        var areaRepository = mock(AreaRepository.class);
        var model = mock(Model.class);

        when(reportService.getAllReports()).thenReturn(List.of());
        when(areaRepository.count()).thenReturn(64L);

        var controller = new HomeController(reportService, areaRepository);
        var view = controller.home(model);

        assertThat(view).isEqualTo("home");
        verify(model).addAttribute(eq("reports"), eq(List.of()));
        verify(model).addAttribute(eq("areaCount"), eq(64L));
    }
}
