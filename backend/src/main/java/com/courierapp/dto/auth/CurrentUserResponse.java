package com.courierapp.dto.auth;

import java.util.List;
import java.util.Set;

public record CurrentUserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        List<String> roles,
        Set<String> permissions
) {
}
