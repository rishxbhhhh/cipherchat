package com.rishabh.cipherchat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rishabh.cipherchat.dto.LoginRequest;
import com.rishabh.cipherchat.dto.RefreshRequest;
import com.rishabh.cipherchat.dto.RegisterRequest;
import com.rishabh.cipherchat.entity.TokenBlacklist;
import com.rishabh.cipherchat.repository.TokenBlacklistRepository;
import com.rishabh.cipherchat.service.AuthService;
import com.rishabh.cipherchat.service.JwtService;
import com.rishabh.cipherchat.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    public AuthController(AuthService authService, JwtService jwtService,
            RefreshTokenService refreshTokenService, TokenBlacklistRepository tokenBlacklistRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.ok("User registered successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest refreshRequest) {
        var token = refreshTokenService.verify(refreshRequest.getRefreshToken());
        String newAccess = jwtService.generateToken(token.getUser().getEmail(), token.getUser().getRole().name());
        return ResponseEntity.ok(Map.of(
                "accessToken", newAccess, "tokenType", "Bearer", "expiresIn", jwtService.getExpirySeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.validateToken(token)) {
                String tokenId = jwtService.extractTokenId(token);
                java.util.Date expiration = jwtService.extractExpiration(token);
                if (tokenId != null && expiration != null) {
                    tokenBlacklistRepository.save(
                            new TokenBlacklist(tokenId, Instant.ofEpochMilli(expiration.getTime())));
                }
            }
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }
}
