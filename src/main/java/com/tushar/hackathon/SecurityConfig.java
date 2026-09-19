package com.tushar.hackathon;

import com.tushar.hackathon.service.SentinelProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Role-based access enforced at the API layer, not only in a UI.
 *
 * <p>ANALYST reads the alert queue; ADMIN also ingests data and sees unmasked customer
 * detail. Passwords come from the environment.
 */
@Configuration
public class SecurityConfig {

    private static final String ANALYST = "ANALYST";
    private static final String ADMIN = "ADMIN";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager users(SentinelProperties properties, PasswordEncoder encoder) {
        var security = properties.security();
        return new InMemoryUserDetailsManager(
                User.withUsername("analyst")
                        .password(encoder.encode(security.analystPassword()))
                        .roles(ANALYST).build(),
                User.withUsername("admin")
                        .password(encoder.encode(security.adminPassword()))
                        .roles(ADMIN).build());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Stateless API with HTTP Basic, so there is no session cookie for CSRF to protect.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/v1/ingestion/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/transactions/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/alerts/**").hasAnyRole(ANALYST, ADMIN)
                        .anyRequest().authenticated())
                .httpBasic(basic -> {
                })
                .build();
    }
}
