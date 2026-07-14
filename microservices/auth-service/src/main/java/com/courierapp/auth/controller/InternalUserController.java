package com.courierapp.auth.controller;

import com.courierapp.auth.dto.auth.CurrentUserResponse;
import com.courierapp.auth.entity.User;
import com.courierapp.auth.exception.ResourceNotFoundException;
import com.courierapp.auth.repository.UserRepository;
import com.courierapp.auth.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Internal endpoints — called only by other microservices, never by external clients.
 * Callers must present the shared internal secret in X-Internal-Auth; the gateway strips
 * any client-supplied copy of this header, so only a service holding the real secret can pass.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @Value("${app.internal.secret}")
    private String internalSecret;

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Auth", required = false) String providedSecret) {
        if (!isValidInternalSecret(providedSecret)) return ResponseEntity.status(403).build();
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        return ResponseEntity.ok(toMap(user));
    }

    @GetMapping("/users/by-username/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(
            @PathVariable String username,
            @RequestHeader(value = "X-Internal-Auth", required = false) String providedSecret) {
        if (!isValidInternalSecret(providedSecret)) return ResponseEntity.status(403).build();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        return ResponseEntity.ok(toMap(user));
    }

    private boolean isValidInternalSecret(String providedSecret) {
        return providedSecret != null && MessageDigest.isEqual(
                providedSecret.getBytes(StandardCharsets.UTF_8), internalSecret.getBytes(StandardCharsets.UTF_8));
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
