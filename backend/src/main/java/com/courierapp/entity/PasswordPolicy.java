package com.courierapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "password_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "restrict_last_passwords", nullable = false)
    private int restrictLastPasswords = 5;

    @Column(name = "password_expiry_days", nullable = false)
    private int passwordExpiryDays = 90;

    @Column(name = "expiry_reminder_days", nullable = false)
    private int expiryReminderDays = 5;

    @Column(name = "session_timeout_hours", nullable = false)
    private int sessionTimeoutHours = 0;

    @Column(name = "session_timeout_minutes", nullable = false)
    private int sessionTimeoutMinutes = 30;

    @Column(name = "max_login_attempts", nullable = false)
    private int maxLoginAttempts = 5;

    @Column(name = "min_password_length", nullable = false)
    private int minPasswordLength = 8;

    @Column(name = "require_uppercase", nullable = false)
    private boolean requireUppercase = true;

    @Column(name = "require_lowercase", nullable = false)
    private boolean requireLowercase = true;

    @Column(name = "require_digit", nullable = false)
    private boolean requireDigit = true;

    @Column(name = "require_special_char", nullable = false)
    private boolean requireSpecialChar = false;
}
