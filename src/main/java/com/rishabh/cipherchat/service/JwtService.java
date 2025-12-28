package com.rishabh.cipherchat.service;

public interface JwtService {
    public String generateToken(String email);
    public long getExpirySeconds();
    public String extractEmail(String token);
    public boolean validateToken(String token);
}
