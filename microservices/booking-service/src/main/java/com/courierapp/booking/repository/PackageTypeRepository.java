package com.courierapp.booking.repository;

import com.courierapp.booking.entity.PackageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageTypeRepository extends JpaRepository<PackageType, Long> {
    List<PackageType> findByActiveTrueOrderByNameAsc();
}
