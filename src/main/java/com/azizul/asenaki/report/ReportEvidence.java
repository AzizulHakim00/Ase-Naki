package com.azizul.asenaki.report;

import com.azizul.asenaki.common.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "report_evidence")
public class ReportEvidence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private UtilityReport report;

    @Column(nullable = false, length = 200)
    private String originalFileName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] fileData;

    public boolean isImage() {
        return contentType.startsWith("image/");
    }
}
