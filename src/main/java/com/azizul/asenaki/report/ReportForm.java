package com.azizul.asenaki.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ReportForm {

    @NotNull(message = "Please choose an area")
    private Long areaId;

    @NotNull(message = "Please choose a utility")
    private UtilityType utilityType;

    @NotNull(message = "Please choose the current status")
    private UtilityStatus status;

    @Size(min = 10, max = 500,
            message = "Description must be 10 to 500 characters")
    private String description = "";

    private MultipartFile evidence;
}
