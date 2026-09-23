package com.azizul.asenaki.location;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AreaRepository extends MongoRepository<Area, String> {
    List<Area> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCaseAndDistrictIgnoreCase(String name, String district);
}
