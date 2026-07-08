package com.courierapp.report.repository;

import com.courierapp.report.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingReportRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b WHERE b.bookingDate >= :from AND b.bookingDate <= :to ORDER BY b.bookingDate ASC")
    List<Booking> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT b FROM Booking b WHERE b.bookingDate >= :from AND b.bookingDate <= :to " +
           "AND (:status IS NULL OR b.status = :status) ORDER BY b.bookingDate ASC")
    List<Booking> findByDateRangeAndStatus(@Param("from") LocalDate from,
                                            @Param("to") LocalDate to,
                                            @Param("status") String status);

    @Query("SELECT COUNT(b), b.status FROM Booking b WHERE b.bookingDate >= :from AND b.bookingDate <= :to GROUP BY b.status")
    List<Object[]> countByStatusInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
