package com.courierapp.auth.service;

public interface AuditLogService {
    void log(String module, String action, Long userId, String username, String performedBy, String details);
}
