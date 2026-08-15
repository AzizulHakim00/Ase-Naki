package com.azizul.asenaki.config;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.report.UtilityReport;
import com.azizul.asenaki.report.UtilityReportRepository;
import com.azizul.asenaki.report.UtilityStatus;
import com.azizul.asenaki.report.UtilityType;
import com.azizul.asenaki.user.UserAccount;
import com.azizul.asenaki.user.UserProfile;
import com.azizul.asenaki.user.UserRepository;
import com.azizul.asenaki.user.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final UtilityReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedAreas();
        seedUsers();
        seedReports();
    }

    private void seedAreas() {
        if (areaRepository.count() > 0) {
            return;
        }

        areaRepository.saveAll(List.of(
                new Area("Mirpur 10", "Dhaka"),
                new Area("Dhanmondi 27", "Dhaka"),
                new Area("Mohammadpur", "Dhaka"),
                new Area("GEC Circle", "Chattogram"),
                new Area("Anderkilla", "Chattogram"),
                new Area("Shaheb Bazar", "Rajshahi")
        ));
    }

    private void seedUsers() {
        createUserIfMissing(
                "Demo Student", "demo@asenaki.bd", "01700000002",
                "Dhaka", "Demo123!", UserRole.USER);
        createUserIfMissing(
                "Ase Naki Admin", "admin@asenaki.bd", "01700000001",
                "Dhaka", adminPassword, UserRole.ADMIN);
    }

    private void createUserIfMissing(
            String name, String email, String phone, String address,
            String password, UserRole role) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        UserAccount user = new UserAccount();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        UserProfile profile = new UserProfile();
        profile.setPhone(phone);
        profile.setAddress(address);
        user.setProfile(profile);

        userRepository.save(user);
    }

    private void seedReports() {
        if (reportRepository.count() > 0) {
            return;
        }

        UserAccount demo = userRepository
                .findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();
        List<Area> areas = areaRepository.findAllByOrderByNameAsc();

        reportRepository.saveAll(List.of(
                sample(demo, findArea(areas, "Mirpur 10"),
                        UtilityType.ELECTRICITY, UtilityStatus.UNAVAILABLE,
                        "Electricity has been unavailable since this morning.", 12),
                sample(demo, findArea(areas, "Dhanmondi 27"),
                        UtilityType.GAS, UtilityStatus.LOW_PRESSURE,
                        "Gas pressure is low in nearby homes.", 30),
                sample(demo, findArea(areas, "GEC Circle"),
                        UtilityType.BROADBAND, UtilityStatus.UNSTABLE,
                        "The broadband connection disconnects often.", 45),
                sample(demo, findArea(areas, "Shaheb Bazar"),
                        UtilityType.WATER, UtilityStatus.AVAILABLE,
                        "Water supply is working normally now.", 8)
        ));
    }

    private UtilityReport sample(
            UserAccount user, Area area, UtilityType utility,
            UtilityStatus status, String description, long minutesAgo) {
        UtilityReport report = new UtilityReport();
        report.setReporter(user);
        report.setArea(area);
        report.setUtilityType(utility);
        report.setStatus(status);
        report.setDescription(description);
        report.setReportedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        return report;
    }

    private Area findArea(List<Area> areas, String name) {
        return areas.stream()
                .filter(area -> area.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
