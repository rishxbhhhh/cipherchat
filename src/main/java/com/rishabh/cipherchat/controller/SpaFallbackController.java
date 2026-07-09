package com.rishabh.cipherchat.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Forwards client-side SPA routes to React's index.html.
 */
@Controller
public class SpaFallbackController implements ErrorController {

    @GetMapping({"/login", "/register", "/chat", "/admin"})
    public String spaRoutes() {
        return "forward:/index.html";
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        String path = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        if (path == null) {
            path = request.getRequestURI();
        }
        if (path != null &&
            !path.startsWith("/api/") &&
            !path.startsWith("/health/") &&
            !path.startsWith("/ws") &&
            !path.startsWith("/h2") &&
            !path.startsWith("/actuator")) {
            return "forward:/index.html";
        }
        return null;
    }
}
