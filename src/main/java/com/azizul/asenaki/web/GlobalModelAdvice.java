package com.azizul.asenaki.web;

import com.azizul.asenaki.notification.NotificationService;
import com.azizul.asenaki.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final UserService userService;
    private final NotificationService notificationService;

    @ModelAttribute("signedInUser")
    public Object signedInUser(Authentication authentication) {
        if (isSignedIn(authentication)) {
            return userService.findByEmail(authentication.getName());
        }
        return null;
    }

    @ModelAttribute("unreadNotifications")
    public long unreadNotifications(Authentication authentication) {
        if (isSignedIn(authentication)) {
            var user = userService.findByEmail(authentication.getName());
            return notificationService.unreadCount(user);
        }
        return 0;
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
