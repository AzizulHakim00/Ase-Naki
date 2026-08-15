package com.azizul.asenaki.report;

import com.azizul.asenaki.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "utility_types")
public class UtilityType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(nullable = false, length = 10)
    private String icon;

    @Column(nullable = false, length = 250)
    private String allowedStatuses;

    public UtilityType(String name, String slug, String icon,
                       List<UtilityStatus> statuses) {
        this.name = name;
        this.slug = slug;
        this.icon = icon;
        this.allowedStatuses = String.join(",",
                statuses.stream().map(Enum::name).toList());
    }

    public boolean allows(UtilityStatus status) {
        return getAllowedStatusList().contains(status);
    }

    public List<UtilityStatus> getAllowedStatusList() {
        return Arrays.stream(allowedStatuses.split(","))
                .map(UtilityStatus::valueOf)
                .toList();
    }
}
