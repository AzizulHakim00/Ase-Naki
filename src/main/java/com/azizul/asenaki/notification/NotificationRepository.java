package com.azizul.asenaki.notification;

import com.azizul.asenaki.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    @Query("""
            select notification from Notification notification
            join fetch notification.report report
            join fetch report.area area
            join fetch area.thana
            join fetch report.utilityType
            where notification.user = :user
            order by notification.createdAt desc
            """)
    List<Notification> findAllForUser(@Param("user") UserAccount user);

    long countByUserAndReadByUserFalse(UserAccount user);
}
