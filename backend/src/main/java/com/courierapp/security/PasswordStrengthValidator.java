package com.courierapp.security;

import com.courierapp.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Enforces enterprise password policy.
 * Min 8 chars, uppercase, lowercase, digit, special char.
 * Blocks common weak passwords.
 */
@Component
public class PasswordStrengthValidator {

    private static final int MIN_LENGTH = 8;

    private static final List<String> COMMON_PASSWORDS = List.of(
        "password", "password1", "password123", "123456789", "12345678",
        "qwerty123", "admin123", "welcome1", "courier123", "shipdesk1"
    );

    public void validate(String password, String username) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (password != null) {
            if (!password.matches(".*[A-Z].*")) errors.add("Must contain at least one uppercase letter");
            if (!password.matches(".*[a-z].*")) errors.add("Must contain at least one lowercase letter");
            if (!password.matches(".*[0-9].*")) errors.add("Must contain at least one digit");
            if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))
                errors.add("Must contain at least one special character");
            if (username != null && password.toLowerCase().contains(username.toLowerCase()))
                errors.add("Password must not contain your username");
            if (COMMON_PASSWORDS.stream().anyMatch(p -> p.equalsIgnoreCase(password)))
                errors.add("This password is too common");
        }

        if (!errors.isEmpty()) {
            throw new BusinessException("Weak password: " + String.join("; ", errors), HttpStatus.BAD_REQUEST);
        }
    }
}
