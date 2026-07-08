package com.courierapp.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "flex_field_options")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FlexFieldOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private FlexField field;

    @Column(name = "option_value", nullable = false, length = 200)
    private String optionValue;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;
}
