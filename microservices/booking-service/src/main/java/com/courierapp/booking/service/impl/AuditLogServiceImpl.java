package com.courierapp.booking.service.impl;

import com.courierapp.booking.entity.AuditLog;
import com.courierapp.booking.repository.AuditLogRepository;
import com.courierapp.booking.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    public void log(String module, String action, Long entityId, String entityName, String performedBy, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setModule(module);
            entry.setAction(action);
            entry.setEntityId(entityId);
            entry.setEntityName(entityName);
            entry.setPerformedBy(performedBy);
            entry.setDetails(details);
            entry.setCreatedAt(Instant.now());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to persist audit log [{}/{}]: {}", module, action, e.getMessage());
        }
    }
}
