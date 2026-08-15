package com.azizul.asenaki.user;

import com.azizul.asenaki.report.UtilityReport;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private boolean enabled = true;

    // One user has one profile.
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private UserProfile profile;

    // One user can create many reports.
    @OneToMany(mappedBy = "reporter")
    private List<UtilityReport> reports = new ArrayList<>();

    public void setProfile(UserProfile profile) {
        this.profile = profile;
        profile.setUser(this);
    }
}
