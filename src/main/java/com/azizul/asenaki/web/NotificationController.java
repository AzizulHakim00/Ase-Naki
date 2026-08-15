package com.azizul.asenaki.web;

import com.azizul.asenaki.notification.NotificationService;
import com.azizul.asenaki.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String list(Authentication authentication, Model model) {
        var user = userService.findByEmail(authentication.getName());
        model.addAttribute(
                "notifications", notificationService.findForUser(user));
        return "notifications";
    }

    @PostMapping("/read-all")
    public String readAll(Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        notificationService.markAllRead(user);
        return "redirect:/notifications";
    }
}
