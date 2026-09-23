package com.azizul.asenaki.report;

import com.azizul.asenaki.location.Area;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "reports")
public class UtilityReport {
    @Id
    private String id;
    @Indexed
    private UtilityType utilityType;
    @Indexed
    private UtilityStatus status;
    private String description;
    private LocalDateTime reportedAt;
    private LocalDateTime updatedAt;
    private String reporterId;
    private String reporterName;
    @Indexed
    private String reporterEmail;
    private Area area;
}
