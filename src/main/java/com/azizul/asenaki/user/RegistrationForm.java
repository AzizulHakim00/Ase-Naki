package com.azizul.asenaki.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationForm {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(?:\\+?88)?01[3-9]\\d{8}$",
            message = "Enter a valid Bangladesh mobile number")
    private String phone;

    @Size(max = 200, message = "Address can be at most 200 characters")
    private String address;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72,
            message = "Password must be 8 to 72 characters")
    private String password;

    @AssertTrue(message = "Please accept the community rules")
    private boolean acceptedRules;
}
