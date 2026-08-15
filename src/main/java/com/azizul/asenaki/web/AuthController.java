package com.azizul.asenaki.web;

import com.azizul.asenaki.user.RegistrationForm;
import com.azizul.asenaki.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegistrationForm registrationForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        userService.register(registrationForm);
        redirectAttributes.addFlashAttribute(
                "success", "Account created. You can sign in now.");
        return "redirect:/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }
}
