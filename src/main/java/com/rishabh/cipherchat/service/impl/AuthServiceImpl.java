package com.rishabh.cipherchat.service.impl;

import com.rishabh.cipherchat.service.AuthService;
import com.rishabh.cipherchat.service.JwtService;
import com.rishabh.cipherchat.service.RefreshTokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.rishabh.cipherchat.dto.LoginRequest;
import com.rishabh.cipherchat.dto.LoginResponse;
import com.rishabh.cipherchat.dto.RegisterRequest;
import com.rishabh.cipherchat.entity.RefreshToken;
import com.rishabh.cipherchat.entity.Role;
import com.rishabh.cipherchat.entity.User;
import com.rishabh.cipherchat.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.error("Email already registered.");
            throw new IllegalStateException("Email already registered.");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);
        log.info("User with email " + registerRequest.getEmail() + " registered successfully.");
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),
                loginRequest.getPassword());
        authenticationManager.authenticate(authentication);
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        String jwt = jwtService.generateToken(loginRequest.getEmail());
        RefreshToken refreshToken = refreshTokenService.create(user);
        log.info("User with email " + loginRequest.getEmail() + " logged in.");
        return new LoginResponse(jwt, "Bearer ", jwtService.getExpirySeconds(), refreshToken.getToken());
    }

}
