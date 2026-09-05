package com.azizul.asenaki.monitoring;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "monitor_power_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class PowerSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime observedAt;
    private LocalDateTime fetchedAt;
    private int demandMw;
    private int supplyMw;
    private int loadSheddingMw;
    private String source;
}
