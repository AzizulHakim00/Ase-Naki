package com.azizul.asenaki.user;

import com.azizul.asenaki.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserAccount extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;

    public boolean hasRole(RoleName roleName) {
        return roles.stream().anyMatch(role -> role.getName() == roleName);
    }

    public boolean isModeratorAccess() {
        return hasRole(RoleName.ADMIN)
                || hasRole(RoleName.MODERATOR);
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
        profile.setUser(this);
    }
}
