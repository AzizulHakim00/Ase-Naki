package com.azizul.asenaki.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegistrationForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("This email is already registered");
        }

        UserAccount user = new UserAccount();
        user.setName(form.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(UserRole.USER);
        user.setEnabled(true);

        UserProfile profile = new UserProfile();
        profile.setPhone(form.getPhone().trim());
        profile.setAddress(form.getAddress() == null ? null : form.getAddress().trim());
        user.setProfile(profile);

        userRepository.save(user);
    }

    public UserAccount findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<UserAccount> getAllUsers() {
        return userRepository.findAllByOrderByNameAsc();
    }

    public void toggleEnabled(String id) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    public void deleteUser(String id) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if ("admin@asenaki.bd".equalsIgnoreCase(user.getEmail())
                || "demo@asenaki.bd".equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Demo accounts cannot be deleted");
        }
        userRepository.delete(user);
    }
}
