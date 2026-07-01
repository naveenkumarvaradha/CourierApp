package com.courierapp.repository;

import com.courierapp.entity.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySettingsRepository extends JpaRepository<CompanySettings, Long> {
}
