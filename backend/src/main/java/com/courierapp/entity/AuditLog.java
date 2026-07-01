package com.courierapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Module: USER, ROLE, PARTY, BOOKING, COMPANY, COURIER_WAY, PACKAGE_TYPE,
     *         DEPARTMENT, FLEX_FIELD, APPROVAL_ROUTING, AUTH */
    @Column(name = "module", nullable = false, length = 50)
    private String module;

    /** Action: CREATE, UPDATE, DELETE, APPROVE, REJECT, SUBMIT, STATUS_CHANGE,
     *          AWB_UPDATE, LOGIN, LOGIN_FAILED, LOGOUT */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name", length = 300)
    private String entityName;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
