package com.courierapp.admin.entity;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_role_id")
    private Role creatorRole;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_user_id")
    private User creatorUser;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "module", nullable = false, length = 30)
    private String module = "BOOKING";

    @Column(name = "level", nullable = false)
    private int level = 1;
}
