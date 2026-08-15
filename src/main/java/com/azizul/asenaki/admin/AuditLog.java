package com.azizul.asenaki.admin;

import com.azizul.asenaki.common.BaseEntity;
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
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private UserAccount actor;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(nullable = false, length = 60)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 500)
    private String details;
}
