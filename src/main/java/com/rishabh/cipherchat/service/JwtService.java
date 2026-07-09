package com.rishabh.cipherchat.service;

import java.util.Date;

public interface JwtService {
    public String generateToken(String email, String role);
    public long getExpirySeconds();
    public String extractEmail(String token);
    public String extractRole(String token);
    public String extractTokenId(String token);
    public Date extractExpiration(String token);
    public boolean validateToken(String token);
}
