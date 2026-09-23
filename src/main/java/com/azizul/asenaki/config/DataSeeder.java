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
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final List<String> DHAKA_AREAS = List.of(
            "Abdullahpur", "Adabor", "Agargaon", "Aftabnagar", "Airport",
            "Azimpur", "Badda", "Banani", "Banani DOHS", "Banasree",
            "Baridhara", "Bashundhara R/A", "Cantonment", "Dhanmondi",
            "Dhanmondi 27", "Farmgate", "Gulshan 1", "Gulshan 2", "Jatrabari",
            "Kafrul", "Kalabagan", "Kallyanpur", "Khilgaon", "Khilkhet",
            "Lalbagh", "Mahakhali", "Malibagh", "Mirpur 1", "Mirpur 2",
            "Mirpur 6", "Mirpur 10", "Mirpur 11", "Mirpur 12",
            "Mohammadpur", "Motijheel", "Mugda", "New Market", "Pallabi",
            "Panthapath", "Rampura", "Shahbagh", "Shantinagar", "Shyamoli",
            "Tejgaon", "Uttara Sector 3", "Uttara Sector 4", "Uttara Sector 5",
            "Uttara Sector 6", "Uttara Sector 7", "Uttara Sector 8",
            "Uttara Sector 9", "Uttara Sector 10", "Uttara Sector 11",
            "Uttara Sector 12", "Uttara Sector 13", "Uttara Sector 14",
            "Uttara Sector 15", "Uttara Sector 16", "Uttara Sector 17",
            "Uttara Sector 18", "Wari"
    );

    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final UtilityReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAreas();
        seedUsers();
        seedReports();
    }

    private void seedAreas() {
        List<Area> newAreas = new ArrayList<>();
        for (String name : DHAKA_AREAS) {
            if (!areaRepository.existsByNameIgnoreCaseAndDistrictIgnoreCase(name, "Dhaka")) {
                newAreas.add(new Area(name, "Dhaka"));
            }
        }
        addAreaIfMissing(newAreas, "GEC Circle", "Chattogram");
        addAreaIfMissing(newAreas, "Anderkilla", "Chattogram");
        addAreaIfMissing(newAreas, "Shaheb Bazar", "Rajshahi");
        if (!newAreas.isEmpty()) {
            areaRepository.saveAll(newAreas);
        }
    }

    private void addAreaIfMissing(List<Area> newAreas, String name, String district) {
        if (!areaRepository.existsByNameIgnoreCaseAndDistrictIgnoreCase(name, district)) {
            newAreas.add(new Area(name, district));
        }
    }

    private void seedUsers() {
        ensureDemoUser("Demo Student", "demo@asenaki.bd", "01700000002",
                "Dhaka", "Demo123!", UserRole.USER);
        ensureDemoUser("Ase Naki Admin", "admin@asenaki.bd", "01700000001",
                "Dhaka", "Admin123!", UserRole.ADMIN);
    }

    private void ensureDemoUser(
            String name, String email, String phone, String address,
            String plainPassword, UserRole role) {
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(UserAccount::new);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(true);
        if (user.getPassword() == null
                || !passwordEncoder.matches(plainPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(plainPassword));
        }

        UserProfile profile = user.getProfile() == null
                ? new UserProfile() : user.getProfile();
        profile.setPhone(phone);
        profile.setAddress(address);
        user.setProfile(profile);
        userRepository.save(user);
    }

    private void seedReports() {
        UserAccount demo = userRepository.findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();
        List<Area> areas = areaRepository.findAllByOrderByNameAsc();

        addSampleIfMissing(demo, areas, "Mirpur 10", UtilityType.ELECTRICITY,
                UtilityStatus.UNAVAILABLE,
                "Electricity has been unavailable since this morning.", 12);
        addSampleIfMissing(demo, areas, "Dhanmondi 27", UtilityType.GAS,
                UtilityStatus.LOW_PRESSURE,
                "Gas pressure is low in nearby homes.", 30);
        addSampleIfMissing(demo, areas, "GEC Circle", UtilityType.BROADBAND,
                UtilityStatus.UNSTABLE,
                "The broadband connection disconnects often.", 45);
        addSampleIfMissing(demo, areas, "Shaheb Bazar", UtilityType.WATER,
                UtilityStatus.AVAILABLE,
                "Water supply is working normally now.", 8);
    }

    private void addSampleIfMissing(
            UserAccount user, List<Area> areas, String areaName,
            UtilityType utility, UtilityStatus status,
            String description, long minutesAgo) {
        if (reportRepository.existsByDescription(description)) {
            return;
        }
        Area area = areas.stream()
                .filter(item -> item.getName().equals(areaName))
                .findFirst()
                .orElseThrow();

        UtilityReport report = new UtilityReport();
        report.setReporterId(user.getId());
        report.setReporterName(user.getName());
        report.setReporterEmail(user.getEmail());
        report.setArea(area);
        report.setUtilityType(utility);
        report.setStatus(status);
        report.setDescription(description);
        report.setReportedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        report.setUpdatedAt(report.getReportedAt());
        reportRepository.save(report);
    }
}
