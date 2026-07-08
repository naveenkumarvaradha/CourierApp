package com.courierapp.auth.entity;

import com.courierapp.auth.enums.ActionType;
import com.courierapp.auth.enums.ModuleType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_permission_module_action",
                columnNames = {"module", "action"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 30)
    private ModuleType module;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private ActionType action;

    /** Canonical authority string, e.g. BOOKING_APPROVE. */
    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "description", length = 255)
    private String description;
}
