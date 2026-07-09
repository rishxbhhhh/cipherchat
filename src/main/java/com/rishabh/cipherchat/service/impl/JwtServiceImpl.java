package com.rishabh.cipherchat.service.impl;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rishabh.cipherchat.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${cipherchat.jwt.expiry-seconds}")
    private long expiryS;

    private final String SECRET = "super-secret-key-change-later-please-use-a-very-very-long-secret-key-123456789";

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    @Override
    public String generateToken(String email, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + (expiryS * 1000)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS512)
                .compact();
    }

    @Override
    public long getExpirySeconds() {
        return expiryS;
    }

    @Override
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    @Override
    public String extractTokenId(String token) {
        return parseClaims(token).getId();
    }

    @Override
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            log.error("Error while validating token. " + ex.getMessage(), ex);
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
