package com.azizul.asenaki.location;

import com.azizul.asenaki.common.BaseEntity;
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
@Table(name = "areas")
public class Area extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String postcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thana_id", nullable = false)
    private Thana thana;

    public Area(String name, String postcode, Thana thana) {
        this.name = name;
        this.postcode = postcode;
        this.thana = thana;
    }

    public String getDisplayName() {
        return name + ", " + thana.getName();
    }
}
