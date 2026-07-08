package com.courierapp.admin.dto.admin;

public record PasswordPolicyResponse(
        Long id, Integer restrictLastPasswords, Integer passwordExpiryDays, Integer expiryReminderDays,
        Integer sessionTimeoutHours, Integer sessionTimeoutMinutes, Integer maxLoginAttempts,
        Integer minPasswordLength, boolean requireUppercase, boolean requireLowercase,
        boolean requireDigit, boolean requireSpecialChar
) {}
