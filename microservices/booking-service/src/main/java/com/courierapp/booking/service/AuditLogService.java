package com.courierapp.booking.service;

public interface AuditLogService {
    void log(String module, String action, Long entityId, String entityName, String performedBy, String details);
}
