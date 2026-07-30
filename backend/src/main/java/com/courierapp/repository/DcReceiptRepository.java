package com.courierapp.repository;

import com.courierapp.entity.DcReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DcReceiptRepository
        extends JpaRepository<DcReceipt, Long>, JpaSpecificationExecutor<DcReceipt> {
    boolean existsByDeliveryChallanId(Long dcId);
}
