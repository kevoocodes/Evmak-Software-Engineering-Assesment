package com.evmak.parking_management.config;

import com.evmak.parking_management.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow access to API documentation
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                // Allow access to health endpoints
                .requestMatchers("/api/v1/health", "/actuator/**").permitAll()
                // Allow access to authentication endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Allow public access to facilities and spots (GET only)
                .requestMatchers(HttpMethod.GET, "/api/v1/facilities/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/spots/*/available").permitAll()
                // Allow data seeding endpoint
                .requestMatchers("/api/v1/data/seed").permitAll()
                // Allow payment provider info endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/payments/providers", "/api/v1/payments/test").permitAll()
                // Allow cache performance test
                .requestMatchers(HttpMethod.GET, "/api/v1/cache/performance-test").permitAll()
                // Require authentication for all other API endpoints
                .requestMatchers("/api/v1/**").authenticated()
                // Require authentication for everything else
                .anyRequest().authenticated()
            );

        // Add JWT authentication filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}