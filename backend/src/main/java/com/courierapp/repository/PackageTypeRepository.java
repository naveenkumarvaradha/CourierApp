package com.courierapp.repository;

import com.courierapp.entity.PackageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageTypeRepository extends JpaRepository<PackageType, Long> {
    List<PackageType> findByActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
