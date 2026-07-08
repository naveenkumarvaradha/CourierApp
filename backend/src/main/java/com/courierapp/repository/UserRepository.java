package com.courierapp.repository;

import com.courierapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String username, String fullName, Pageable pageable);
    List<User> findByCreatedAtBetween(Instant from, Instant to);
    List<User> findByInactiveAtBetween(Instant from, Instant to);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ADMIN' AND u.active = true AND u.id <> :excludeId")
    long countActiveAdminsExcluding(@org.springframework.data.repository.query.Param("excludeId") Long excludeId);
}
