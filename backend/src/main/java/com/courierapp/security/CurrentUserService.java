package com.courierapp.security;

import com.courierapp.entity.User;
import com.courierapp.exception.BusinessException;
import com.courierapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated caller's tenant (company) fresh from the database on every call,
 * rather than trusting a claim baked into the JWT at login time — company reassignment takes
 * effect immediately instead of only after the token expires.
 */
@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }
        return auth.getName();
    }

    /** The current user's company id. Every tenant-scoped read/write must go through this. */
    public Long requireCompanyId() {
        User user = userRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new BusinessException("Authenticated user not found", HttpStatus.UNAUTHORIZED));
        if (user.getCompany() == null) {
            throw new BusinessException("Your account is not assigned to a company. Contact an administrator.",
                    HttpStatus.FORBIDDEN);
        }
        return user.getCompany().getId();
    }
}
