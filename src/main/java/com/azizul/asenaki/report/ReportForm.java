package com.azizul.asenaki.report;

import com.azizul.asenaki.report.validation.NoDuplicateRecentReport;
import com.azizul.asenaki.report.validation.ValidEvidenceFile;
import com.azizul.asenaki.report.validation.ValidUtilityStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ValidUtilityStatus
@NoDuplicateRecentReport
public class ReportForm {

    @NotNull(message = "Choose an area")
    private Long areaId;

    @NotNull(message = "Choose a utility")
    private Long utilityTypeId;

    @NotNull(message = "Choose the current status")
    private UtilityStatus status;

    @Size(max = 500, message = "Description can be at most 500 characters")
    private String description;

    @ValidEvidenceFile
    private MultipartFile evidence;
}
