package com.azizul.asenaki.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select log from AuditLog log
            join fetch log.actor actor
            join fetch actor.profile
            order by log.createdAt desc
            """)
    List<AuditLog> findAllWithActor();
}
