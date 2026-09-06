package com.azizul.asenaki.incident;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.report.UtilityReport;
import com.azizul.asenaki.report.UtilityType;
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

@Entity
@Table(name = "app_utility_incidents")
@Getter
@Setter
@NoArgsConstructor
public class UtilityIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UtilityType utilityType;

    @Column(length = 100)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IncidentState state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentConfidence confidence;

    @Column(nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(nullable = false)
    private LocalDateTime lastSignalAt;

    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private boolean dismissed;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncidentSignal> signals = new ArrayList<>();

    @OneToMany(mappedBy = "incident")
    private List<UtilityReport> reports = new ArrayList<>();
}
