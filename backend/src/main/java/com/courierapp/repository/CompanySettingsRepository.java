package com.courierapp.repository;

import com.courierapp.entity.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanySettingsRepository extends JpaRepository<CompanySettings, Long> {
    Optional<CompanySettings> findByCompanyId(Long companyId);
}
