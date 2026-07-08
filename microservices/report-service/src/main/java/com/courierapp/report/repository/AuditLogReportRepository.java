package com.courierapp.report.repository;

import com.courierapp.report.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogReportRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByModuleAndEntityIdOrderByCreatedAtDesc(String module, Long entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.module = :module AND a.createdAt >= :from AND a.createdAt <= :to ORDER BY a.createdAt ASC")
    List<AuditLog> findByModuleAndDateRange(@Param("module") String module,
                                             @Param("from") Instant from,
                                             @Param("to") Instant to);
}
