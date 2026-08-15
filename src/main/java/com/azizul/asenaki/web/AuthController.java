package com.azizul.asenaki.web;

import com.azizul.asenaki.user.RegistrationForm;
import com.azizul.asenaki.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(
            @ModelAttribute("form") RegistrationForm form) {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult result) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(form);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException exception) {
            result.rejectValue("email", "duplicate", exception.getMessage());
            return "auth/register";
        }
    }
}
