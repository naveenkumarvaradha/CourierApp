package com.courierapp.repository;

import com.courierapp.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    Optional<Booking> findByBookingNumber(String bookingNumber);
    List<Booking> findByBookingDateBetween(LocalDate from, LocalDate to);
}
