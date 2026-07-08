package com.courierapp.booking.repository;

import com.courierapp.booking.entity.BookingSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface BookingSequenceRepository extends JpaRepository<BookingSequence, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BookingSequence> findBySeqDate(String seqDate);
}
