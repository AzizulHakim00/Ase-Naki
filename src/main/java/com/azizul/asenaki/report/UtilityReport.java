package com.azizul.asenaki.report;

import com.azizul.asenaki.common.BaseEntity;
import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.user.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "utility_reports")
public class UtilityReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserAccount reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utility_type_id", nullable = false)
    private UtilityType utilityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UtilityStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportState state = ReportState.ACTIVE;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int confidence;

    @Column(nullable = false)
    private LocalDateTime reportedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<ReportConfirmation> confirmations = new HashSet<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<ReportEvidence> evidenceFiles = new HashSet<>();

    @OneToOne(mappedBy = "report", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private OutageIncident incident;

    public long getAgeInMinutes() {
        return Math.max(0, Duration.between(reportedAt, LocalDateTime.now()).toMinutes());
    }

    public boolean hasEvidence() {
        return !evidenceFiles.isEmpty();
    }

    public long getVoteCount(ConfirmationChoice choice) {
        return confirmations.stream()
                .filter(item -> item.getChoice() == choice)
                .count();
    }


    public long getConfirmCount() {
        return getVoteCount(ConfirmationChoice.CONFIRM);
    }

    public long getDisputeCount() {
        return getVoteCount(ConfirmationChoice.DISPUTE);
    }

    public long getRestoredCount() {
        return getVoteCount(ConfirmationChoice.RESTORED);
    }
}
