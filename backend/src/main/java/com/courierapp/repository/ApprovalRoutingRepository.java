package com.courierapp.repository;

import com.courierapp.entity.ApprovalRouting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRoutingRepository extends JpaRepository<ApprovalRouting, Long> {
    List<ApprovalRouting> findByActiveTrue();
}
