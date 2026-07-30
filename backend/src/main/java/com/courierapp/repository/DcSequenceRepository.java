package com.courierapp.repository;

import com.courierapp.entity.DcSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface DcSequenceRepository extends JpaRepository<DcSequence, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DcSequence> findBySeqDate(String seqDate);
}
