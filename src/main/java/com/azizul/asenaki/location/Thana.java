package com.azizul.asenaki.location;

import com.azizul.asenaki.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "thanas")
public class Thana extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @OneToMany(mappedBy = "thana", cascade = CascadeType.ALL)
    private List<Area> areas = new ArrayList<>();

    public Thana(String name, District district) {
        this.name = name;
        this.district = district;
    }
}
