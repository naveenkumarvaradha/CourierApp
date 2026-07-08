package com.courierapp.admin.service;

import com.courierapp.admin.dto.PageResponse;
import com.courierapp.admin.dto.audit.AuditLogResponse;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void log(String module, String action, Long entityId, String entityName, String performedBy, String details);
    PageResponse<AuditLogResponse> getEntityHistory(String module, Long entityId, Pageable pageable);
}
