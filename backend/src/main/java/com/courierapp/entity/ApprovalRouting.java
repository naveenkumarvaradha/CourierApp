package com.courierapp.entity;

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

    /** Either role or user must be set (designated approver for BOOKING). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    /** When set, this rule only applies to bookings created by users who have this role. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_role_id")
    private Role creatorRole;

    /** When set, this rule only applies to bookings created by this specific user. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_user_id")
    private User creatorUser;

    @Column(name = "active", nullable = false)
    private boolean active;

    /** BOOKING or MASTER — scopes this rule to a specific workflow. */
    @Column(name = "module", nullable = false, length = 30)
    private String module = "BOOKING";

    /** Approval level (1 = first, 2 = second, …). Bookings/parties move through levels in order. */
    @Column(name = "level", nullable = false)
    private int level = 1;
}
