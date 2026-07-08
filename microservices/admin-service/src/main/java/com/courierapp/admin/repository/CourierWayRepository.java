package com.courierapp.admin.repository;

import com.courierapp.admin.entity.CourierWay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourierWayRepository extends JpaRepository<CourierWay, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<CourierWay> findByActiveTrueOrderByNameAsc();
}
