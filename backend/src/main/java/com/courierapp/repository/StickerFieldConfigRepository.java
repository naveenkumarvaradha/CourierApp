package com.courierapp.repository;

import com.courierapp.entity.StickerFieldConfig;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StickerFieldConfigRepository extends JpaRepository<StickerFieldConfig, Long> {
    List<StickerFieldConfig> findByCompanyIdOrderBySortOrder(Long companyId);

    @Transactional
    void deleteByCompanyId(Long companyId);
}
