package com.courierapp.repository;

import com.courierapp.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findByBookingIdOrderByEventTimeDesc(Long bookingId);
}
