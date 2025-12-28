package com.rishabh.cipherchat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok("Cipher Chat v1.0 is up.");
    }

    @GetMapping("/test")
    public ResponseEntity<?> testJwt() {
        return ResponseEntity.ok("Jwt token working as expected.");
    }

}
