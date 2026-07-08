package com.courierapp.party.entity;

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

    @Column(name = "approver_role_name", length = 60)
    private String approverRoleName;

    @Column(name = "approver_username", length = 60)
    private String approverUsername;

    @Column(name = "creator_role_name", length = 60)
    private String creatorRoleName;

    @Column(name = "creator_username", length = 60)
    private String creatorUsername;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "module", nullable = false, length = 30)
    private String module = "BOOKING";

    @Column(name = "level", nullable = false)
    private int level = 1;
}
