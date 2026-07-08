package com.courierapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sticker_field_config",
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "field_key"}))
@Getter @Setter
public class StickerFieldConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "field_key", nullable = false, length = 50)
    private String fieldKey;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "section", nullable = false, length = 30)
    private String section = "HEADER";
}
