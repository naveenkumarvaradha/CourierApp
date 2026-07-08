package com.courierapp.admin.repository;

import com.courierapp.admin.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    Page<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String username, String fullName, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE u.active = true AND UPPER(r.name) = 'ADMIN' AND u.id <> :excludeId")
    long countActiveAdminsExcluding(@Param("excludeId") Long excludeId);
}
