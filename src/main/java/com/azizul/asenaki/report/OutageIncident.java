package com.azizul.asenaki.report;

import com.azizul.asenaki.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "outage_incidents")
public class OutageIncident extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private UtilityReport report;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentState state;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private List<IncidentStatusHistory> statusHistory = new ArrayList<>();
}
