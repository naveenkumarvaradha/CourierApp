package com.courierapp.report.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Read-only projection of the parties table for report queries.
 */
@Entity
@Table(name = "parties")
@Getter
@NoArgsConstructor
public class Party {

    @Id
    private Long id;

    @Column(name = "party_code")
    private String partyCode;

    @Column(name = "party_name")
    private String partyName;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "gstin")
    private String gstin;

    @Column(name = "party_type")
    private String partyType;

    @Column(name = "active")
    private boolean active;

    @Column(name = "party_status")
    private String partyStatus;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private Instant createdAt;
}
