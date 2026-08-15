package com.azizul.asenaki.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                                "/", "/login", "/register",
                                "/css/**", "/js/**", "/images/**",
                                "/actuator/health", "/error"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/reports/view/**", "/evidence/**"
                        ).permitAll()
                        .requestMatchers("/admin/**")
                        .hasAnyRole("ADMIN", "MODERATOR")
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
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/access-denied")
                )
                .rememberMe(remember -> remember
                        .key("ase-naki-remember-me")
                        .tokenValiditySeconds(14 * 24 * 60 * 60)
                );

        return http.build();
    }
}
