package com.azizul.asenaki.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegistrationForm form) {
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role is missing"));

        UserAccount user = new UserAccount();
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.addRole(userRole);

        UserProfile profile = new UserProfile();
        profile.setFullName(form.getFullName().trim());
        profile.setPhone(normalizePhone(form.getPhone()));
        user.setProfile(profile);

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserAccount findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private String normalizePhone(String phone) {
        String cleaned = phone.replace(" ", "");
        if (cleaned.startsWith("+880")) {
            return "0" + cleaned.substring(4);
        }
        if (cleaned.startsWith("880")) {
            return "0" + cleaned.substring(3);
        }
        return cleaned;
    }
}
