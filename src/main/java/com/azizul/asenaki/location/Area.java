package com.azizul.asenaki.location;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "areas")
@CompoundIndex(name = "area_name_district_idx", def = "{'name': 1, 'district': 1}", unique = true)
public class Area {
    @Id
    private String id;
    private String name;
    private String district;

    public Area(String name, String district) {
        this.name = name;
        this.district = district;
    }

    public String getDisplayName() {
        return name + ", " + district;
    }
}
