package com.courierapp.dto.admin;

public record PasswordPolicyResponse(
        Long id,
        int restrictLastPasswords,
        int passwordExpiryDays,
        int expiryReminderDays,
        int sessionTimeoutHours,
        int sessionTimeoutMinutes,
        int maxLoginAttempts,
        int minPasswordLength,
        boolean requireUppercase,
        boolean requireLowercase,
        boolean requireDigit,
        boolean requireSpecialChar
) {}
