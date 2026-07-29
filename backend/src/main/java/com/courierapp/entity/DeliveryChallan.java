package com.courierapp.entity;

import com.courierapp.enums.DcStatus;
import jakarta.persistence.*;
import lombok.*;

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

    /** One DC per booking. */
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /** Sending branch/unit — source of the FROM address printed on the DC. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "vehicle_number", length = 30)
    private String vehicleNumber;

    @Column(name = "driver_name", length = 100)
    private String driverName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DcStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
