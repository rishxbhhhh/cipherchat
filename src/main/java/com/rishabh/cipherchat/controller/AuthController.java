package com.rishabh.cipherchat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rishabh.cipherchat.dto.LoginRequest;
import com.rishabh.cipherchat.dto.RefreshRequest;
import com.rishabh.cipherchat.dto.RegisterRequest;
import com.rishabh.cipherchat.service.AuthService;
import com.rishabh.cipherchat.service.JwtService;
import com.rishabh.cipherchat.service.RefreshTokenService;

import jakarta.validation.Valid;

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

    public AuthController(AuthService authService, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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
        String newAccess = jwtService.generateToken(token.getUser().getEmail());
        return ResponseEntity.ok(Map.of(
                "accessToken", newAccess, "tokenType", "Bearer", "expiresIn", jwtService.getExpirySeconds()));
    }

}
