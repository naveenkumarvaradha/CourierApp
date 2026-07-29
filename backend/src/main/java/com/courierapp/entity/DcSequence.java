package com.courierapp.entity;

import jakarta.persistence.*;
import lombok.*;

/** Per-day sequence counter for generating delivery challan numbers. */
@Entity
@Table(name = "dc_sequence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DcSequence {

    @Id
    @Column(name = "seq_date", length = 8)
    private String seqDate;

    @Column(name = "last_value", nullable = false)
    private long lastValue;
}
