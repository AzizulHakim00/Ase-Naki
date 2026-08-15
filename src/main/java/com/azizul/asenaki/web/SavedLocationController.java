package com.azizul.asenaki.web;

import com.azizul.asenaki.favorite.SavedLocationService;
import com.azizul.asenaki.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/saved-locations")
public class SavedLocationController {

    private final SavedLocationService savedLocationService;
    private final UserService userService;

    @GetMapping
    public String list(Authentication authentication, Model model) {
        var user = userService.findByEmail(authentication.getName());
        model.addAttribute(
                "savedLocations", savedLocationService.findForUser(user));
        return "saved-locations";
    }

    @PostMapping("/toggle/{areaId}")
    public String toggle(
            @PathVariable Long areaId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        var user = userService.findByEmail(authentication.getName());
        boolean saved = savedLocationService.toggle(user, areaId);
        redirectAttributes.addFlashAttribute(
                "success", saved ? "Location saved." : "Location removed.");
        return "redirect:/?areaId=" + areaId;
    }
}
