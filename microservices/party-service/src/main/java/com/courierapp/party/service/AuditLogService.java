package com.courierapp.party.service;

public interface AuditLogService {
    void log(String module, String action, Long entityId, String entityName, String performedBy, String details);
}
