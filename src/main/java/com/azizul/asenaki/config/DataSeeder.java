package com.azizul.asenaki.config;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.location.District;
import com.azizul.asenaki.location.DistrictRepository;
import com.azizul.asenaki.location.Division;
import com.azizul.asenaki.location.DivisionRepository;
import com.azizul.asenaki.location.Thana;
import com.azizul.asenaki.location.ThanaRepository;
import com.azizul.asenaki.report.ReportState;
import com.azizul.asenaki.report.UtilityReport;
import com.azizul.asenaki.report.UtilityReportRepository;
import com.azizul.asenaki.report.UtilityStatus;
import com.azizul.asenaki.report.UtilityType;
import com.azizul.asenaki.report.UtilityTypeRepository;
import com.azizul.asenaki.user.Role;
import com.azizul.asenaki.user.RoleName;
import com.azizul.asenaki.user.RoleRepository;
import com.azizul.asenaki.user.UserAccount;
import com.azizul.asenaki.user.UserProfile;
import com.azizul.asenaki.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final DistrictRepository districtRepository;
    private final ThanaRepository thanaRepository;
    private final AreaRepository areaRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final UtilityReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedLocations();
        seedUtilities();
        seedUsers();
        seedReports();
        log.info("Demo data ready: {} users, {} areas, {} utilities and {} reports",
                userRepository.count(), areaRepository.count(),
                utilityTypeRepository.count(), reportRepository.count());
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
            }
        }
    }

    private void seedLocations() {
        if (areaRepository.count() > 0) {
            return;
        }

        Division dhaka = divisionRepository.save(new Division("Dhaka"));
        Division chattogram =
                divisionRepository.save(new Division("Chattogram"));
        Division rajshahi =
                divisionRepository.save(new Division("Rajshahi"));

        District dhakaDistrict =
                districtRepository.save(new District("Dhaka", dhaka));
        District chattogramDistrict = districtRepository.save(
                new District("Chattogram", chattogram));
        District rajshahiDistrict =
                districtRepository.save(new District("Rajshahi", rajshahi));

        Thana mirpur = thanaRepository.save(
                new Thana("Mirpur", dhakaDistrict));
        Thana dhanmondi = thanaRepository.save(
                new Thana("Dhanmondi", dhakaDistrict));
        Thana mohammadpur = thanaRepository.save(
                new Thana("Mohammadpur", dhakaDistrict));
        Thana panchlaish = thanaRepository.save(
                new Thana("Panchlaish", chattogramDistrict));
        Thana kotwali = thanaRepository.save(
                new Thana("Kotwali", chattogramDistrict));
        Thana boalia = thanaRepository.save(
                new Thana("Boalia", rajshahiDistrict));

        areaRepository.saveAll(List.of(
                new Area("Mirpur 10", "1216", mirpur),
                new Area("Pallabi", "1216", mirpur),
                new Area("Kazipara", "1216", mirpur),
                new Area("Dhanmondi 27", "1209", dhanmondi),
                new Area("Jigatola", "1209", dhanmondi),
                new Area("Mohammadpur", "1207", mohammadpur),
                new Area("Bosila", "1207", mohammadpur),
                new Area("GEC Circle", "4000", panchlaish),
                new Area("Nasirabad", "4000", panchlaish),
                new Area("Anderkilla", "4000", kotwali),
                new Area("Shaheb Bazar", "6100", boalia)
        ));
    }

    private void seedUtilities() {
        if (utilityTypeRepository.count() > 0) {
            return;
        }

        utilityTypeRepository.saveAll(List.of(
                new UtilityType("Electricity", "electricity", "?",
                        List.of(
                                UtilityStatus.AVAILABLE,
                                UtilityStatus.UNAVAILABLE,
                                UtilityStatus.UNSTABLE,
                                UtilityStatus.MAINTENANCE,
                                UtilityStatus.RESTORED
                        )),
                new UtilityType("Gas", "gas", "??",
                        List.of(
                                UtilityStatus.AVAILABLE,
                                UtilityStatus.UNAVAILABLE,
                                UtilityStatus.LOW_PRESSURE,
                                UtilityStatus.MAINTENANCE,
                                UtilityStatus.RESTORED
                        )),
                new UtilityType("Water", "water", "??",
                        List.of(
                                UtilityStatus.AVAILABLE,
                                UtilityStatus.UNAVAILABLE,
                                UtilityStatus.LOW_PRESSURE,
                                UtilityStatus.MAINTENANCE,
                                UtilityStatus.RESTORED
                        )),
                new UtilityType("Broadband", "broadband", "?",
                        List.of(
                                UtilityStatus.AVAILABLE,
                                UtilityStatus.UNAVAILABLE,
                                UtilityStatus.SLOW,
                                UtilityStatus.UNSTABLE,
                                UtilityStatus.PARTIAL_OUTAGE,
                                UtilityStatus.RESTORED
                        )),
                new UtilityType("Mobile network", "mobile-network", "?",
                        List.of(
                                UtilityStatus.AVAILABLE,
                                UtilityStatus.UNAVAILABLE,
                                UtilityStatus.SLOW,
                                UtilityStatus.UNSTABLE,
                                UtilityStatus.PARTIAL_OUTAGE,
                                UtilityStatus.RESTORED
                        ))
        ));
    }

    private void seedUsers() {
        if (userRepository.findByEmailIgnoreCase("admin@asenaki.bd").isEmpty()) {
            UserAccount admin = buildUser(
                    "Ase Naki Admin",
                    "admin@asenaki.bd",
                    "01700000001",
                    adminPassword
            );
            admin.addRole(requiredRole(RoleName.ADMIN));
            userRepository.save(admin);
        }

        if (userRepository.findByEmailIgnoreCase("demo@asenaki.bd").isEmpty()) {
            UserAccount demo = buildUser(
                    "Demo Reporter",
                    "demo@asenaki.bd",
                    "01700000002",
                    "Demo123!"
            );
            demo.addRole(requiredRole(RoleName.TRUSTED_REPORTER));
            userRepository.save(demo);
        }
    }

    private void seedReports() {
        if (reportRepository.count() > 0) {
            return;
        }

        UserAccount reporter = userRepository
                .findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();
        List<Area> areas = areaRepository.findAllWithLocation();
        List<UtilityType> utilities = utilityTypeRepository.findAll();

        createSample(reporter, findArea(areas, "Mirpur 10"),
                findUtility(utilities, "electricity"),
                UtilityStatus.UNAVAILABLE,
                "Transformer maintenance is in progress near block C.",
                42, 12);
        createSample(reporter, findArea(areas, "Dhanmondi 27"),
                findUtility(utilities, "gas"),
                UtilityStatus.LOW_PRESSURE,
                "Pressure has been low since early morning.",
                61, 35);
        createSample(reporter, findArea(areas, "GEC Circle"),
                findUtility(utilities, "broadband"),
                UtilityStatus.UNSTABLE,
                "Connection drops every few minutes.",
                54, 48);
        createSample(reporter, findArea(areas, "Shaheb Bazar"),
                findUtility(utilities, "water"),
                UtilityStatus.AVAILABLE,
                "Water supply is normal now.",
                78, 7);
    }

    private UserAccount buildUser(
            String name, String email, String phone, String rawPassword) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.addRole(requiredRole(RoleName.USER));

        UserProfile profile = new UserProfile();
        profile.setFullName(name);
        profile.setPhone(phone);
        user.setProfile(profile);
        return user;
    }

    private Role requiredRole(RoleName name) {
        return roleRepository.findByName(name).orElseThrow();
    }

    private Area findArea(List<Area> areas, String name) {
        return areas.stream()
                .filter(area -> area.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private UtilityType findUtility(
            List<UtilityType> utilities, String slug) {
        return utilities.stream()
                .filter(item -> item.getSlug().equals(slug))
                .findFirst()
                .orElseThrow();
    }

    private void createSample(
            UserAccount reporter,
            Area area,
            UtilityType utility,
            UtilityStatus status,
            String description,
            int confidence,
            long minutesAgo) {
        UtilityReport report = new UtilityReport();
        report.setReporter(reporter);
        report.setArea(area);
        report.setUtilityType(utility);
        report.setStatus(status);
        report.setState(ReportState.ACTIVE);
        report.setDescription(description);
        report.setConfidence(confidence);
        report.setReportedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        report.setExpiresAt(LocalDateTime.now().plusHours(24));
        reportRepository.save(report);
    }
}
