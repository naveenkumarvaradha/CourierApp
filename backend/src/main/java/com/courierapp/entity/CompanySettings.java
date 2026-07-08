package com.courierapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Arrays;

@Entity
@Table(name = "company_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanySettings extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

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

    // ── SMTP mail config (optional — falls back to application.yml if null) ──
    @Column(name = "smtp_host", length = 200)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username", length = 200)
    private String smtpUsername;

    @Column(name = "smtp_password", length = 500)
    private String smtpPassword;

    @Column(name = "smtp_from_name", length = 100)
    private String smtpFromName;

    @Column(name = "smtp_tls")
    private Boolean smtpTls;

    // ── Company logo ──
    @Column(name = "logo_data", columnDefinition = "BYTEA")
    private byte[] logoData;

    @Column(name = "logo_content_type", length = 100)
    private String logoContentType;

    /** The party record that mirrors this company — used as the sender on every booking. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "linked_party_id")
    private Party linkedParty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;
}
