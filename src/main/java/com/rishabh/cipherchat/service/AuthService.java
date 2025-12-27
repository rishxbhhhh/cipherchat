package com.rishabh.cipherchat.service;

import com.rishabh.cipherchat.dto.RegisterRequest;
import com.rishabh.cipherchat.dto.LoginRequest;

import jakarta.transaction.Transactional;

public interface AuthService {

    @Transactional
    public void register(RegisterRequest registerRequest);

    public void login(LoginRequest loginRequest);
}
