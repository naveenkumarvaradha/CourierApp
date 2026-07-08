package com.courierapp.booking.entity;

import com.courierapp.booking.enums.PartyStatus;
import com.courierapp.booking.enums.PartyType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Local read-only snapshot of Party as used by Booking.
 * The authoritative Party data lives in party-service.
 */
@Entity
@Table(name = "parties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Party extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_code", nullable = false, unique = true, length = 30)
    private String partyCode;

    @Column(name = "party_name", nullable = false, length = 150)
    private String partyName;

    @Column(name = "address_line1", nullable = false, length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "pincode", nullable = false, length = 20)
    private String pincode;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "gstin", length = 20)
    private String gstin;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 20)
    private PartyType partyType;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_status", nullable = false, length = 30)
    private PartyStatus partyStatus;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "current_approval_level", nullable = false)
    private int currentApprovalLevel = 1;

    @Column(name = "company_id")
    private Long companyId;
}
