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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "report_confirmations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_confirmation_user",
                columnNames = {"report_id", "user_id"}
        )
)
public class ReportConfirmation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private UtilityReport report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfirmationChoice choice;

    @Column(nullable = false)
    private int weight;
}
