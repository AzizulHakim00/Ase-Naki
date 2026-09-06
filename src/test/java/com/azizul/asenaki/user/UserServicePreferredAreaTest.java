package com.azizul.asenaki.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azizul.asenaki.location.AreaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserServicePreferredAreaTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Test
    void savesOnePreferredAreaOnExistingProfile() {
        var area = areaRepository.findAllByOrderByNameAsc().stream()
                .filter(candidate -> candidate.getName().equals("Mirpur 10"))
                .findFirst()
                .orElseThrow();

        userService.setPreferredArea("demo@asenaki.bd", area.getId());

        var demo = userRepository.findByEmailIgnoreCase("demo@asenaki.bd").orElseThrow();
        assertThat(demo.getProfile().getPreferredArea().getId()).isEqualTo(area.getId());
    }

    @Test
    void invalidPreferredAreaIsRejected() {
        assertThatThrownBy(() -> userService.setPreferredArea(
                "demo@asenaki.bd", Long.MAX_VALUE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Area not found");
    }
}
