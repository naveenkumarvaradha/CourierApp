package com.courierapp.admin.repository;

import com.courierapp.admin.entity.StickerFieldConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StickerFieldConfigRepository extends JpaRepository<StickerFieldConfig, Long> {
    List<StickerFieldConfig> findByCompanyIdOrderBySortOrder(Long companyId);
    void deleteByCompanyId(Long companyId);
}
