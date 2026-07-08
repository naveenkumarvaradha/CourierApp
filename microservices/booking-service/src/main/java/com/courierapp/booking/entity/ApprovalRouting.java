package com.courierapp.booking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "approval_routing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRouting extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Approver role name (used to match against approver's roles). */
    @Column(name = "approver_role_name", length = 60)
    private String approverRoleName;

    /** Approver username (specific user). */
    @Column(name = "approver_username", length = 60)
    private String approverUsername;

    /** When set, this rule only applies when creator has this role. */
    @Column(name = "creator_role_name", length = 60)
    private String creatorRoleName;

    /** When set, this rule only applies to bookings created by this specific user. */
    @Column(name = "creator_username", length = 60)
    private String creatorUsername;

    @Column(name = "active", nullable = false)
    private boolean active;

    /** BOOKING or MASTER — scopes this rule to a specific workflow. */
    @Column(name = "module", nullable = false, length = 30)
    private String module = "BOOKING";

    /** Approval level (1 = first, 2 = second, …). */
    @Column(name = "level", nullable = false)
    private int level = 1;
}
