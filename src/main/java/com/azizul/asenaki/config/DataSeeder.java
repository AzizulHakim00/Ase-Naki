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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String DHAKA = "Dhaka";

    // Common neighbourhoods from both Dhaka North and Dhaka South.
    // Keeping the names in one simple list makes the coverage easy to update.
    private static final List<String> DHAKA_AREAS = List.of(
            "Abdullahpur",
            "Adabor",
            "Agargaon",
            "Aftabnagar",
            "Airport",
            "Armanitola",
            "Ashkona",
            "Azampur",
            "Azimpur",
            "Badda",
            "Baily Road",
            "Bakshibazar",
            "Banani",
            "Banani DOHS",
            "Banglamotor",
            "Bangshal",
            "Banasree",
            "Baridhara",
            "Baridhara DOHS",
            "Bashabo",
            "Bashundhara R/A",
            "Baunia",
            "Beraid",
            "Bosila",
            "Cantonment",
            "Central Road",
            "Chawkbazar",
            "Dakshinkhan",
            "Darus Salam",
            "Dayaganj",
            "Demra",
            "Dhanmondi",
            "Dhanmondi 27",
            "Dhaka University Area",
            "Dholai Khal",
            "Diabari",
            "Donia",
            "Duaripara",
            "Elephant Road",
            "English Road",
            "Eskaton",
            "Farmgate",
            "Gabtoli",
            "Gandaria",
            "Gopibagh",
            "Goran",
            "Green Road",
            "Gulistan",
            "Gulshan 1",
            "Gulshan 2",
            "Hatirpool",
            "Hazaribagh",
            "Ibrahimpur",
            "Islampur",
            "Jatrabari",
            "Joar Sahara",
            "Jurain",
            "Kadamtali",
            "Kafrul",
            "Kakrail",
            "Kalabagan",
            "Kalachandpur",
            "Kallyanpur",
            "Kalshi",
            "Kamalapur",
            "Kamrangirchar",
            "Karail",
            "Kathalbagan",
            "Kawla",
            "Khilgaon",
            "Khilkhet",
            "Konapara",
            "Kuril",
            "Lalbagh",
            "Mahakhali",
            "Mahakhali DOHS",
            "Malibagh",
            "Manda",
            "Maniknagar",
            "Matuail",
            "Merul Badda",
            "Middle Badda",
            "Mirpur 1",
            "Mirpur 2",
            "Mirpur 6",
            "Mirpur 10",
            "Mirpur 11",
            "Mirpur 12",
            "Mirpur 13",
            "Mirpur 14",
            "Mirpur DOHS",
            "Moghbazar",
            "Mohammadpur",
            "Monipur",
            "Motijheel",
            "Mugda",
            "Nadda",
            "Nakhalpara",
            "Nandipara",
            "Nawabpur",
            "Naya Paltan",
            "New Market",
            "Niketan",
            "Nikunja 1",
            "Nikunja 2",
            "North Badda",
            "Nurerchala",
            "Old Dhaka",
            "Paikpara",
            "Pallabi",
            "Paltan",
            "Panthapath",
            "Paribagh",
            "Rajarbagh",
            "Ramna",
            "Rampura",
            "Rayer Bazar",
            "Rayerbagh",
            "Rupnagar",
            "Sabujbagh",
            "Sadarghat",
            "Science Lab",
            "Segunbagicha",
            "Shah Ali",
            "Shahbagh",
            "Shahjadpur",
            "Shakhari Bazar",
            "Shantinagar",
            "Shewrapara",
            "Shonir Akhra",
            "Shyamoli",
            "Shyampur",
            "Siddheswari",
            "South Badda",
            "Sutrapur",
            "Swamibagh",
            "Taltola",
            "Tejgaon",
            "Tejgaon Industrial Area",
            "Tikatuli",
            "Uttara Sector 1",
            "Uttara Sector 2",
            "Uttara Sector 3",
            "Uttara Sector 4",
            "Uttara Sector 5",
            "Uttara Sector 6",
            "Uttara Sector 7",
            "Uttara Sector 8",
            "Uttara Sector 9",
            "Uttara Sector 10",
            "Uttara Sector 11",
            "Uttara Sector 12",
            "Uttara Sector 13",
            "Uttara Sector 14",
            "Uttara Sector 15",
            "Uttara Sector 16",
            "Uttara Sector 17",
            "Uttara Sector 18",
            "Uttarkhan",
            "Vatara",
            "Wari",
            "West Rampura",
            "West Shewrapara",
            "Zigatola"
    );

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
        Set<String> existingAreas = new HashSet<>();
        for (Area area : areaRepository.findAll()) {
            existingAreas.add(areaKey(area.getName(), area.getDistrict()));
        }

        List<Area> newAreas = new ArrayList<>();
        for (String areaName : DHAKA_AREAS) {
            addAreaIfMissing(newAreas, existingAreas, areaName, DHAKA);
        }

        // A few areas outside Dhaka are kept for the original class demo.
        addAreaIfMissing(newAreas, existingAreas, "GEC Circle", "Chattogram");
        addAreaIfMissing(newAreas, existingAreas, "Anderkilla", "Chattogram");
        addAreaIfMissing(newAreas, existingAreas, "Shaheb Bazar", "Rajshahi");

        areaRepository.saveAll(newAreas);
    }

    private void addAreaIfMissing(
            List<Area> newAreas, Set<String> existingAreas,
            String name, String district) {
        String key = areaKey(name, district);
        if (existingAreas.add(key)) {
            newAreas.add(new Area(name, district));
        }
    }

    private String areaKey(String name, String district) {
        return (name + "|" + district).toLowerCase();
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
        UserAccount demo = userRepository
                .findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();
        List<Area> areas = areaRepository.findAllByOrderByNameAsc();

        addSampleIfMissing(demo, areas, "Mirpur 10",
                UtilityType.ELECTRICITY, UtilityStatus.UNAVAILABLE,
                "Electricity has been unavailable since this morning.", 12);
        addSampleIfMissing(demo, areas, "Dhanmondi 27",
                UtilityType.GAS, UtilityStatus.LOW_PRESSURE,
                "Gas pressure is low in nearby homes.", 30);
        addSampleIfMissing(demo, areas, "GEC Circle",
                UtilityType.BROADBAND, UtilityStatus.UNSTABLE,
                "The broadband connection disconnects often.", 45);
        addSampleIfMissing(demo, areas, "Shaheb Bazar",
                UtilityType.WATER, UtilityStatus.AVAILABLE,
                "Water supply is working normally now.", 8);
        addSampleIfMissing(demo, areas, "Uttara Sector 7",
                UtilityType.ELECTRICITY, UtilityStatus.AVAILABLE,
                "Electricity is available around the sector this evening.", 16);
        addSampleIfMissing(demo, areas, "Gulshan 2",
                UtilityType.MOBILE_NETWORK, UtilityStatus.UNSTABLE,
                "Mobile data is unstable near the main avenue.", 21);
        addSampleIfMissing(demo, areas, "Badda",
                UtilityType.WATER, UtilityStatus.LOW_PRESSURE,
                "Water pressure is lower than usual in nearby buildings.", 27);
        addSampleIfMissing(demo, areas, "Banani",
                UtilityType.BROADBAND, UtilityStatus.AVAILABLE,
                "Broadband service is working normally in this area.", 34);
        addSampleIfMissing(demo, areas, "Motijheel",
                UtilityType.ELECTRICITY, UtilityStatus.MAINTENANCE,
                "Scheduled electricity maintenance is in progress.", 39);
        addSampleIfMissing(demo, areas, "Jatrabari",
                UtilityType.GAS, UtilityStatus.AVAILABLE,
                "Gas supply is available with normal pressure.", 43);
        addSampleIfMissing(demo, areas, "Mohammadpur",
                UtilityType.WATER, UtilityStatus.AVAILABLE,
                "Water supply returned and is currently available.", 49);
        addSampleIfMissing(demo, areas, "Bashundhara R/A",
                UtilityType.BROADBAND, UtilityStatus.UNSTABLE,
                "Internet speed is fluctuating during the evening.", 55);
        addSampleIfMissing(demo, areas, "Wari",
                UtilityType.ELECTRICITY, UtilityStatus.UNAVAILABLE,
                "A short electricity outage is affecting this neighbourhood.", 61);
        addSampleIfMissing(demo, areas, "Khilgaon",
                UtilityType.GAS, UtilityStatus.LOW_PRESSURE,
                "Gas pressure is low but the supply is still available.", 67);
    }

    private void addSampleIfMissing(
            UserAccount user, List<Area> areas, String areaName,
            UtilityType utility, UtilityStatus status,
            String description, long minutesAgo) {
        if (!reportRepository.existsByDescription(description)) {
            reportRepository.save(sample(
                    user, findArea(areas, areaName), utility,
                    status, description, minutesAgo));
        }
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
