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

    @Column(name = "active", nullable = false)
    private boolean active;
}
