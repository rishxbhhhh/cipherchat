package com.rishabh.cipherchat.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rishabh.cipherchat.entity.User;
import com.rishabh.cipherchat.exception.ResourceNotFoundException;
import com.rishabh.cipherchat.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        log.info("Fetching userlist page {} of size {}", page, size);
        Page<User> users = userRepository.searchUsers(
                (search != null && !search.isBlank()) ? search.trim() : null,
                PageRequest.of(page, size));
        log.info("Fetched userlist page {} of size {}", page, size);
        var result = users.map(u -> Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "role", u.getRole().name(),
                "enabled", u.getEnabled(),
                "dateCreated", u.getDateCreated() != null ? u.getDateCreated().toString() : null));

        return ResponseEntity.ok(result);
    }

    @PutMapping("/users/{id}/toggle")
    public ResponseEntity<?> toggleUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // Never disable yourself
        if ("ADMIN".equals(user.getRole().name())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot disable admin accounts."));
        }

        user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "enabled", user.getEnabled()));
    }
}
