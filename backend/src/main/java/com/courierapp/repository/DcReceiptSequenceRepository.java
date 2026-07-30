package com.courierapp.repository;

import com.courierapp.entity.DcReceiptSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface DcReceiptSequenceRepository extends JpaRepository<DcReceiptSequence, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DcReceiptSequence> findBySeqDate(String seqDate);
}
