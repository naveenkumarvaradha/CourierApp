package com.courierapp.repository;

import com.courierapp.entity.CourierWay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourierWayRepository extends JpaRepository<CourierWay, Long> {
    List<CourierWay> findByActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
