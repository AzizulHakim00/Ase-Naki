package com.azizul.asenaki.notification;

import com.azizul.asenaki.common.BaseEntity;
import com.azizul.asenaki.report.UtilityReport;
import com.azizul.asenaki.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private UtilityReport report;

    @Column(nullable = false, length = 250)
    private String message;

    @Column(nullable = false)
    private boolean readByUser;
}
