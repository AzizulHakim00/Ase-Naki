package com.azizul.asenaki.favorite;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedLocationRepository
        extends JpaRepository<SavedLocation, Long> {

    Optional<SavedLocation> findByUserAndArea(UserAccount user, Area area);

    boolean existsByUserAndArea(UserAccount user, Area area);

    @Query("""
            select saved from SavedLocation saved
            join fetch saved.area area
            join fetch area.thana thana
            join fetch thana.district
            where saved.user = :user
            order by area.name
            """)
    List<SavedLocation> findAllForUser(@Param("user") UserAccount user);

    List<SavedLocation> findByArea(Area area);
}
