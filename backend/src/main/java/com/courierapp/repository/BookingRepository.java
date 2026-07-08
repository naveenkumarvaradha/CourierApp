package com.courierapp.repository;

import com.courierapp.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    Optional<Booking> findByBookingNumber(String bookingNumber);
    Optional<Booking> findByAwbNumber(String awbNumber);
    List<Booking> findByBookingDateBetween(LocalDate from, LocalDate to);
    List<Booking> findByBookingDateBetweenAndStatusOrderByBookingDateAsc(
            LocalDate from, LocalDate to, com.courierapp.enums.BookingStatus status);
    long countByStatus(com.courierapp.enums.BookingStatus status);
}
