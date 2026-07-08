package com.courierapp.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "flex_field_values",
        uniqueConstraints = @UniqueConstraint(columnNames = {"module", "entity_id", "field_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FlexFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private FlexField field;

    @Column(name = "field_value", length = 500)
    private String fieldValue;
}
