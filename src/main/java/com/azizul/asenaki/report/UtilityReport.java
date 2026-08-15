package com.azizul.asenaki.report;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.user.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_reports")
public class UtilityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UtilityType utilityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UtilityStatus status;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime reportedAt;

    // Many reports can belong to one user.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserAccount reporter;

    // Many reports can belong to one area.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    // One report can contain many uploaded images.
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ReportEvidence> evidenceFiles = new ArrayList<>();

    public void addEvidence(ReportEvidence evidence) {
        evidenceFiles.add(evidence);
        evidence.setReport(this);
    }

    public boolean hasEvidence() {
        return !evidenceFiles.isEmpty();
    }
}
