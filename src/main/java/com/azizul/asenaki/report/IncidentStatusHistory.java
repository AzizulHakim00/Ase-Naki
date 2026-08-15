package com.azizul.asenaki.report;

import com.azizul.asenaki.common.BaseEntity;
import com.azizul.asenaki.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "incident_status_history")
public class IncidentStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private OutageIncident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private UserAccount changedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentState state;

    @Column(nullable = false, length = 250)
    private String note;
}
