package com.azizul.asenaki.favorite;

import com.azizul.asenaki.common.BaseEntity;
import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.user.UserAccount;
import jakarta.persistence.Entity;
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
        name = "saved_locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_saved_location_user_area",
                columnNames = {"user_id", "area_id"}
        )
)
public class SavedLocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;
}
