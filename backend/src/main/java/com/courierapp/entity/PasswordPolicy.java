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
    @Builder.Default
    private int restrictLastPasswords = 5;

    @Column(name = "password_expiry_days", nullable = false)
    @Builder.Default
    private int passwordExpiryDays = 90;

    @Column(name = "expiry_reminder_days", nullable = false)
    @Builder.Default
    private int expiryReminderDays = 5;

    @Column(name = "session_timeout_hours", nullable = false)
    @Builder.Default
    private int sessionTimeoutHours = 0;

    @Column(name = "session_timeout_minutes", nullable = false)
    @Builder.Default
    private int sessionTimeoutMinutes = 30;

    @Column(name = "max_login_attempts", nullable = false)
    @Builder.Default
    private int maxLoginAttempts = 5;

    @Column(name = "min_password_length", nullable = false)
    @Builder.Default
    private int minPasswordLength = 8;

    @Column(name = "require_uppercase", nullable = false)
    @Builder.Default
    private boolean requireUppercase = true;

    @Column(name = "require_lowercase", nullable = false)
    @Builder.Default
    private boolean requireLowercase = true;

    @Column(name = "require_digit", nullable = false)
    @Builder.Default
    private boolean requireDigit = true;

    @Column(name = "require_special_char", nullable = false)
    @Builder.Default
    private boolean requireSpecialChar = false;
}
