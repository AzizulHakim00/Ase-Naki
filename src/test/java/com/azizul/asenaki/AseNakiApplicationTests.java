package com.azizul.asenaki;

import static org.assertj.core.api.Assertions.assertThat;

import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.ReportEvidence;
import com.azizul.asenaki.report.ReportEvidenceRepository;
import com.azizul.asenaki.report.UtilityReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class AseNakiApplicationTests {

    @Autowired
    private UtilityReportRepository reportRepository;

    @Autowired
    private ReportEvidenceRepository evidenceRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Test
    void applicationStarts() {
    }

    @Test
    void databaseContainsBroadDhakaCoverage() {
        var areaNames = areaRepository.findAllByOrderByNameAsc().stream()
                .map(area -> area.getName())
                .toList();

        assertThat(areaNames.size()).isGreaterThan(140);
        assertThat(areaNames).contains(
                "Uttara Sector 7", "Mirpur 10", "Gulshan 2",
                "Badda", "Motijheel", "Jatrabari", "Wari");
    }

    @Test
    @Transactional
    void databaseStoresAnUploadedImage() {
        ReportEvidence evidence = new ReportEvidence();
        evidence.setFileName("test.png");
        evidence.setContentType("image/png");
        evidence.setFileData(new byte[1_000]);
        evidence.setReport(reportRepository.findAll().getFirst());

        ReportEvidence saved = evidenceRepository.saveAndFlush(evidence);

        assertThat(saved.getId()).isNotNull();
    }
}
