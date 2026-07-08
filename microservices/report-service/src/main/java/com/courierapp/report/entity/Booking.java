package com.courierapp.report.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Read-only projection of the bookings table for report queries.
 */
@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor
public class Booking {

    @Id
    private Long id;

    @Column(name = "booking_number")
    private String bookingNumber;

    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Column(name = "courier_way_id")
    private Long courierWayId;

    @Column(name = "package_type_id")
    private Long packageTypeId;

    @Column(name = "item_description")
    private String itemDescription;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "no_of_packages")
    private Integer noOfPackages;

    @Column(name = "courier_mode")
    private String courierMode;

    @Column(name = "status")
    private String status;

    @Column(name = "awb_number")
    private String awbNumber;

    @Column(name = "company_po_no")
    private String companyPoNo;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "company_id")
    private Long companyId;
}
