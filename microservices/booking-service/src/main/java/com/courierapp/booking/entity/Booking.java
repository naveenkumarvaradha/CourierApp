package com.courierapp.booking.entity;

import com.courierapp.booking.enums.BookingStatus;
import com.courierapp.booking.enums.CourierMode;
import com.courierapp.booking.enums.PaymentMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_number", nullable = false, unique = true, length = 40)
    private String bookingNumber;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Party sender;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Party receiver;

    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "no_of_packages", nullable = false)
    private Integer noOfPackages;

    @Enumerated(EnumType.STRING)
    @Column(name = "courier_mode", nullable = false, length = 20)
    private CourierMode courierMode;

    @Column(name = "declared_value", precision = 14, scale = 2)
    private BigDecimal declaredValue;

    @Column(name = "freight_charges", precision = 14, scale = 2)
    private BigDecimal freightCharges;

    @Column(name = "total_charges", precision = 14, scale = 2)
    private BigDecimal totalCharges;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 20)
    private PaymentMode paymentMode;

    @Column(name = "special_instructions", length = 1000)
    private String specialInstructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BookingStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "courier_way_id")
    private CourierWay courierWay;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "package_type_id")
    private PackageType packageType;

    @Column(name = "awb_number", unique = true, length = 60)
    private String awbNumber;

    @Column(name = "approver_username", length = 60)
    private String approverUsername;

    @Column(name = "approval_timestamp")
    private Instant approvalTimestamp;

    @Column(name = "approval_remarks", length = 500)
    private String approvalRemarks;

    @Column(name = "company_po_no", length = 100)
    private String companyPoNo;

    @Column(name = "print_taken", nullable = false)
    private boolean printTaken = false;

    @Column(name = "cancellation_remarks", length = 500)
    private String cancellationRemarks;

    @Column(name = "current_approval_level", nullable = false)
    private int currentApprovalLevel = 1;
}
