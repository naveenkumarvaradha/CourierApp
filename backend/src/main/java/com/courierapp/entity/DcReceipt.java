package com.courierapp.entity;

import com.courierapp.enums.DcStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "dc_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DcReceipt extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 40)
    private String receiptNumber;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    /** One receipt per DC. */
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "dc_id", nullable = false, unique = true)
    private DeliveryChallan deliveryChallan;

    /** The DC's status immediately before this receipt was confirmed (ISSUED or DELIVERED) — lets an undo restore it. */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_dc_status", nullable = false, length = 20)
    private DcStatus previousDcStatus;

    @Column(name = "received_by", length = 60)
    private String receivedBy;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
