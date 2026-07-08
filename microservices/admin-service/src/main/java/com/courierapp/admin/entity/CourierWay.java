package com.courierapp.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courier_ways")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierWay extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;
}
