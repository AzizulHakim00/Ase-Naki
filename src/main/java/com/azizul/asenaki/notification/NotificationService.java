package com.azizul.asenaki.notification;

import com.azizul.asenaki.favorite.SavedLocationRepository;
import com.azizul.asenaki.report.UtilityReport;
import com.azizul.asenaki.user.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SavedLocationRepository savedLocationRepository;

    public void notifySavedLocationUsers(UtilityReport report) {
        var notifications = savedLocationRepository.findByArea(report.getArea())
                .stream()
                .filter(saved -> !saved.getUser().getId()
                        .equals(report.getReporter().getId()))
                .map(saved -> buildNotification(saved.getUser(), report))
                .toList();
        notificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public List<Notification> findForUser(UserAccount user) {
        return notificationRepository.findAllForUser(user);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UserAccount user) {
        return notificationRepository.countByUserAndReadByUserFalse(user);
    }

    @Transactional
    public void markAllRead(UserAccount user) {
        List<Notification> notifications =
                notificationRepository.findAllForUser(user);
        notifications.forEach(item -> item.setReadByUser(true));
    }

    private Notification buildNotification(
            UserAccount user, UtilityReport report) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setReport(report);
        notification.setMessage(report.getUtilityType().getName()
                + " is " + report.getStatus().getLabel().toLowerCase()
                + " in " + report.getArea().getName());
        return notification;
    }
}
