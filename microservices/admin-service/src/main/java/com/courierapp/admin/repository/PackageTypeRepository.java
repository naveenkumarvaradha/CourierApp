package com.courierapp.admin.repository;

import com.courierapp.admin.entity.PackageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageTypeRepository extends JpaRepository<PackageType, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<PackageType> findByActiveTrueOrderByNameAsc();
}
