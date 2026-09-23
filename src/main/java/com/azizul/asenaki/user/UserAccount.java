package com.azizul.asenaki.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "users")
public class UserAccount {
    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String email;
    private String password;
    private UserRole role = UserRole.USER;
    private boolean enabled = true;
    private UserProfile profile;

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }
}
