package com.courierapp.dto.auth;

import java.util.List;
import java.util.Set;

public record CurrentUserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        Long companyId,
        String companyCode,
        String companyName,
        List<String> roles,
        Set<String> permissions
) {}
