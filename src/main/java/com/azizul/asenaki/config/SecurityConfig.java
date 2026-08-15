package com.azizul.asenaki.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {
        http.authorizeHttpRequests(request -> request
                        .requestMatchers(
                                "/", "/login", "/register",
                                "/css/**", "/images/**",
                                "/actuator/health", "/error"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/reports/{id:[0-9]+}", "/evidence/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/?logout")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("ase-naki-remember-me")
                        .tokenValiditySeconds(14 * 24 * 60 * 60)
                );

        return http.build();
    }
}
