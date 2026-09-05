package com.azizul.asenaki.monitoring;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "monitor_internet_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class InternetStatusSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime checkedAt;

    @Enumerated(EnumType.STRING)
    private MonitoringState cloudflareState;

    @Enumerated(EnumType.STRING)
    private MonitoringState iodaState;

    @Enumerated(EnumType.STRING)
    private MonitoringState overallState;

    private String summary;
    private String affectedNetwork;
}
