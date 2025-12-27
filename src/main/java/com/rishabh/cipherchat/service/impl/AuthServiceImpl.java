package com.rishabh.cipherchat.service.impl;

import com.rishabh.cipherchat.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.rishabh.cipherchat.dto.LoginRequest;
import com.rishabh.cipherchat.dto.RegisterRequest;
import com.rishabh.cipherchat.entity.User;
import com.rishabh.cipherchat.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
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

        userRepository.save(user);
        log.info("User with email " + registerRequest.getEmail() + " registered successfully.");
    }

    public void login(LoginRequest loginRequest) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),
                loginRequest.getPassword());
        authenticationManager.authenticate(authentication);
        log.info("User with email " + loginRequest.getEmail() + " logged in.");
    }

}
