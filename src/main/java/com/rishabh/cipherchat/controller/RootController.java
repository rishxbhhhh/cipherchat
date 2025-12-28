package com.rishabh.cipherchat.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "app", "CipherChat API",
                "status", "UP",
                "version", "1.0",
                "docs", "/health/ping");
    }

}
