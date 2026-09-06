package com.azizul.asenaki.user;

import com.azizul.asenaki.location.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AreaRepository areaRepository;

    @Transactional
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

    @Transactional(readOnly = true)
    public UserAccount findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public void setPreferredArea(String email, Long areaId) {
        UserAccount user = findByEmail(email);
        var area = areaRepository.findById(areaId)
                .orElseThrow(() -> new IllegalArgumentException("Area not found"));
        user.getProfile().setPreferredArea(area);
        userRepository.save(user);
    }
}
