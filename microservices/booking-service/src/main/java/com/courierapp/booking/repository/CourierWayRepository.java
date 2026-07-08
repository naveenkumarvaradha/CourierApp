package com.courierapp.booking.repository;

import com.courierapp.booking.entity.CourierWay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourierWayRepository extends JpaRepository<CourierWay, Long> {
    List<CourierWay> findByActiveTrueOrderByNameAsc();
}
