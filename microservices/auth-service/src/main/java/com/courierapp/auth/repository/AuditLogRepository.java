package com.courierapp.auth.repository;

import com.courierapp.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>,
        JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByModuleAndEntityIdOrderByCreatedAtDesc(String module, Long entityId, Pageable pageable);
    List<AuditLog> findByModuleAndEntityIdOrderByCreatedAtAsc(String module, Long entityId);
    List<AuditLog> findByModuleAndCreatedAtBetweenOrderByCreatedAtAsc(String module, Instant from, Instant to);
}
