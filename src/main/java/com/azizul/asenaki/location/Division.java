package com.azizul.asenaki.location;

import com.azizul.asenaki.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "divisions")
public class Division extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @OneToMany(mappedBy = "division", cascade = CascadeType.ALL)
    private List<District> districts = new ArrayList<>();

    public Division(String name) {
        this.name = name;
    }
}
