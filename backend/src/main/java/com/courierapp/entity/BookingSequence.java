package com.courierapp.entity;

import jakarta.persistence.*;
import lombok.*;

/** Per-day sequence counter for generating booking numbers. */
@Entity
@Table(name = "booking_sequence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSequence {

    @Id
    @Column(name = "seq_date", length = 8)
    private String seqDate;

    @Column(name = "last_value", nullable = false)
    private long lastValue;
}
