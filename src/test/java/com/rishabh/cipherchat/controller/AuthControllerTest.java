package com.rishabh.cipherchat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishabh.cipherchat.dto.LoginRequest;
import com.rishabh.cipherchat.dto.LoginResponse;
import com.rishabh.cipherchat.dto.RegisterRequest;
import com.rishabh.cipherchat.entity.RefreshToken;
import com.rishabh.cipherchat.entity.User;
import com.rishabh.cipherchat.exception.ConflictException;
import com.rishabh.cipherchat.repository.TokenBlacklistRepository;
import com.rishabh.cipherchat.service.AuthService;
import com.rishabh.cipherchat.service.JwtService;
import com.rishabh.cipherchat.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.authentication.BadCredentialsException;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        @Primary
        RefreshTokenService refreshTokenService() {
            return mock(RefreshTokenService.class);
        }
    }

    @Test
    void shouldRegisterNewUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully."));
    }

    @Test
    void shouldRejectDuplicateRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@example.com");
        request.setPassword("password123");

        doThrow(new ConflictException("Email already registered."))
                .when(authService).register(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setEmail("login-test@example.com");
        login.setPassword("secret123");

        LoginResponse response = new LoginResponse("jwt-token-abc", "Bearer ", 3600L, "refresh-token-xyz");
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token-abc"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-xyz"));
    }

    @Test
    void shouldRejectInvalidPassword() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setEmail("bad@example.com");
        login.setPassword("wrong");

        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Invalid username or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRefreshToken() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");
        user.setRole(com.rishabh.cipherchat.entity.Role.USER);
        RefreshToken refreshToken = RefreshToken.builder()
                .token("old-refresh-token")
                .user(user)
                .expiry(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenService.verify("old-refresh-token")).thenReturn(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh-token\"}"))
                .andExpect(status().isOk());
    }
}
