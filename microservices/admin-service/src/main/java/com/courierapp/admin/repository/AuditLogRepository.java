package com.courierapp.admin.repository;

import com.courierapp.admin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByModuleAndEntityId(String module, Long entityId, Pageable pageable);
}
