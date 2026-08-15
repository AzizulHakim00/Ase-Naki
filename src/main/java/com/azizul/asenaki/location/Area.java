package com.azizul.asenaki.location;

import com.azizul.asenaki.report.UtilityReport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_areas")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String district;

    // One area can have many utility reports.
    @OneToMany(mappedBy = "area")
    private List<UtilityReport> reports = new ArrayList<>();

    public Area(String name, String district) {
        this.name = name;
        this.district = district;
    }

    public String getDisplayName() {
        return name + ", " + district;
    }
}
