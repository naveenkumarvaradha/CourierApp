package com.courierapp.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "password_policy")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restrict_last_passwords")
    private Integer restrictLastPasswords;

    @Column(name = "password_expiry_days")
    private Integer passwordExpiryDays;

    @Column(name = "expiry_reminder_days")
    private Integer expiryReminderDays;

    @Column(name = "session_timeout_hours")
    private Integer sessionTimeoutHours;

    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes;

    @Column(name = "max_login_attempts")
    private Integer maxLoginAttempts;

    @Column(name = "min_password_length")
    private Integer minPasswordLength;

    @Column(name = "require_uppercase")
    private boolean requireUppercase;

    @Column(name = "require_lowercase")
    private boolean requireLowercase;

    @Column(name = "require_digit")
    private boolean requireDigit;

    @Column(name = "require_special_char")
    private boolean requireSpecialChar;
}
