package com.azizul.asenaki.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Long> {

    @Query("""
            select area from Area area
            join fetch area.thana thana
            join fetch thana.district district
            join fetch district.division
            order by district.name, thana.name, area.name
            """)
    List<Area> findAllWithLocation();
}
