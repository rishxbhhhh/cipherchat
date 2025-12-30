package com.rishabh.cipherchat.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.rishabh.cipherchat.entity.Role;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // default strength is 10
    }

    // TO-DO: for now logout is just cosmetic, add revoke JWT logic later in
    // AuthController where DB lookup table will be used
    // TO-DO: make actuator endpoints acessible for ADMIN authority only except
    // health
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/").permitAll()

                        .requestMatchers("/api/admin/**").hasAuthority(Role.ADMIN.name())

                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers("/health/ping").permitAll()

                        .requestMatchers("/health/test").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())

                        .requestMatchers("/h2/**").permitAll()

                        .requestMatchers("/actuator/**").hasAuthority(Role.ADMIN.name())

                        .requestMatchers("/api/conversations/**").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())

                        .requestMatchers("/api/messages/**").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())

                        .anyRequest().authenticated())

                .logout(logout -> logout.logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.getWriter().write("Logged out successfully.");
                }))

                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        // Register JWT filter BEFORE username/password filter
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager AuthenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
