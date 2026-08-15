package com.azizul.asenaki.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilityTypeRepository extends JpaRepository<UtilityType, Long> {

    Optional<UtilityType> findBySlug(String slug);
}
