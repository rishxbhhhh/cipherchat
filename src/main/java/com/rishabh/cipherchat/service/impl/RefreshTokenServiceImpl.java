package com.rishabh.cipherchat.service.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rishabh.cipherchat.entity.RefreshToken;
import com.rishabh.cipherchat.entity.User;
import com.rishabh.cipherchat.repository.RefreshTokenRepository;
import com.rishabh.cipherchat.service.RefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${cipherchat.refresh.expiry-seconds}")
    private long expirySeconds;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public RefreshToken create(User user) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiry(Instant.now().plusSeconds(expirySeconds));
        return refreshTokenRepository.save(token);
    }

    @Override
    public RefreshToken verify(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalStateException("Invalid refresh token."));
        if (token.getExpiry().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new IllegalStateException("Refresh token expired.");
        }
        return token;
    }
}
