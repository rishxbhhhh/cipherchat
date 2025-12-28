package com.rishabh.cipherchat.service;

import com.rishabh.cipherchat.entity.RefreshToken;
import com.rishabh.cipherchat.entity.User;

public interface RefreshTokenService {
    RefreshToken create(User user);

    RefreshToken verify(String token);
}
