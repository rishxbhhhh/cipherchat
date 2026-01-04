package com.rishabh.cipherchat.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import com.rishabh.cipherchat.entity.Role;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,@Value("#{'${cipherchat.allowed-origins}'.split(',')}") List<String> allowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public CorsConfigurationSource configurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
                .cors(cors -> cors.configurationSource(configurationSource()))
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
