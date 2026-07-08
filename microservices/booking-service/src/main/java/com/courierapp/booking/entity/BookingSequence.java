package com.courierapp.booking.entity;

import jakarta.persistence.*;
import lombok.*;

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
