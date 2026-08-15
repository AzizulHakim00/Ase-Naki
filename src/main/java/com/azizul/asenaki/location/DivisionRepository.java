package com.azizul.asenaki.location;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DivisionRepository extends JpaRepository<Division, Long> {

    boolean existsByNameIgnoreCase(String name);
}
