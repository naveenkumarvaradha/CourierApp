package com.courierapp.entity;

import com.courierapp.enums.CourierMode;
import com.courierapp.enums.DcStatus;
import com.courierapp.enums.DcType;
import com.courierapp.enums.ReceiverType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "delivery_challans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryChallan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dc_number", nullable = false, unique = true, length = 40)
    private String dcNumber;

    @Column(name = "dc_date", nullable = false)
    private LocalDate dcDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "dc_type", nullable = false, length = 20)
    private DcType dcType;

    /** Sending branch/unit — source of the FROM address printed on the DC. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_type", nullable = false, length = 10)
    private ReceiverType receiverType;

    /** Set when receiverType == PARTY — an external customer/receiver. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_party_id")
    private Party receiverParty;

    /** Set when receiverType == UNIT — an inter-branch transfer to another of the company's units. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_unit_id")
    private Unit receiverUnit;

    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "no_of_packages", nullable = false)
    private Integer noOfPackages;

    @Enumerated(EnumType.STRING)
    @Column(name = "courier_mode", nullable = false, length = 20)
    private CourierMode courierMode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "courier_way_id")
    private CourierWay courierWay;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "package_type_id")
    private PackageType packageType;

    @Column(name = "vehicle_number", length = 30)
    private String vehicleNumber;

    @Column(name = "driver_name", length = 100)
    private String driverName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DcStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "current_approval_level", nullable = false)
    @Builder.Default
    private int currentApprovalLevel = 1;

    @Column(name = "approver_username", length = 60)
    private String approverUsername;

    @Column(name = "approval_timestamp")
    private Instant approvalTimestamp;

    @Column(name = "approval_remarks", length = 500)
    private String approvalRemarks;
}
