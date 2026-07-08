package com.courierapp.auth.controller;

import com.courierapp.auth.dto.auth.CurrentUserResponse;
import com.courierapp.auth.entity.User;
import com.courierapp.auth.exception.ResourceNotFoundException;
import com.courierapp.auth.repository.UserRepository;
import com.courierapp.auth.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Internal endpoints — called only by other microservices via Feign.
 * The API Gateway forwards all requests including these; calling services
 * must pass X-Internal-Service: true header (validated by filter below).
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalFlag) {
        if (!"true".equals(internalFlag)) return ResponseEntity.status(403).build();
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        return ResponseEntity.ok(toMap(user));
    }

    @GetMapping("/users/by-username/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(
            @PathVariable String username,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalFlag) {
        if (!"true".equals(internalFlag)) return ResponseEntity.status(403).build();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        return ResponseEntity.ok(toMap(user));
    }

    private Map<String, Object> toMap(User u) {
        Set<String> perms = new AppUserPrincipal(u).getAuthorityStrings().stream()
                .filter(a -> !a.startsWith("ROLE_")).collect(Collectors.toCollection(TreeSet::new));
        return Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "fullName", u.getFullName() != null ? u.getFullName() : "",
                "email", u.getEmail() != null ? u.getEmail() : "",
                "companyId", u.getCompany() != null ? u.getCompany().getId() : 0L,
                "active", u.isActive(),
                "permissions", perms
        );
    }
}
