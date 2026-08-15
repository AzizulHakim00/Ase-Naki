package com.azizul.asenaki.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegistrationForm form) {
        if (userRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new IllegalArgumentException("This email is already registered");
        }

        UserAccount user = new UserAccount();
        user.setName(form.getName().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(form.getPassword()));

        UserProfile profile = new UserProfile();
        profile.setPhone(form.getPhone().trim());
        profile.setAddress(form.getAddress() == null
                ? null : form.getAddress().trim());
        user.setProfile(profile);

        userRepository.save(user);
    }

    public UserAccount findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
