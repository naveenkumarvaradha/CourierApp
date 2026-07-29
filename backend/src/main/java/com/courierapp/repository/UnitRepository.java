package com.courierapp.repository;

import com.courierapp.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findByCompanyIdOrderByUnitNameAsc(Long companyId);
    List<Unit> findByCompanyIdAndActiveTrueOrderByUnitNameAsc(Long companyId);
    Optional<Unit> findByCompanyIdAndDefaultUnitTrue(Long companyId);
    boolean existsByCompanyIdAndUnitNameIgnoreCase(Long companyId, String unitName);
}
