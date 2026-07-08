package com.courierapp.admin.dto.audit;

import java.time.Instant;

public record AuditLogResponse(Long id, String module, String action, Long entityId, String entityName,
        String performedBy, String details, Instant createdAt) {}
