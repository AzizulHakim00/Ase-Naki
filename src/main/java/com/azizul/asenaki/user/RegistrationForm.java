package com.azizul.asenaki.user;

import com.azizul.asenaki.user.validation.UniqueEmail;
import com.azizul.asenaki.user.validation.ValidBangladeshPhone;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name is too long")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @UniqueEmail
    private String email;

    @NotBlank(message = "Phone number is required")
    @ValidBangladeshPhone
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
    private String password;

    @AssertTrue(message = "Please accept the community rules")
    private boolean acceptedRules;
}
