package com.rishabh.cipherchat.service.impl;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rishabh.cipherchat.repository.TokenBlacklistRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TokenBlacklistPurgeService {

    private final TokenBlacklistRepository tokenBlacklistRepository;

    public TokenBlacklistPurgeService(TokenBlacklistRepository tokenBlacklistRepository) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void purgeExpired() {
        int deleted = tokenBlacklistRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Purged {} expired token blacklist entries.", deleted);
        }
    }
}
