package com.courierapp.repository;

import com.courierapp.entity.DeliveryChallan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeliveryChallanRepository
        extends JpaRepository<DeliveryChallan, Long>, JpaSpecificationExecutor<DeliveryChallan> {
}
