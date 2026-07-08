package com.courierapp.party.repository;

import com.courierapp.party.entity.ApprovalRouting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRoutingRepository extends JpaRepository<ApprovalRouting, Long> {
    List<ApprovalRouting> findByActiveTrue();
}
