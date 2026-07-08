package com.courierapp.auth.service.impl;

import com.courierapp.auth.entity.AuditLog;
import com.courierapp.auth.repository.AuditLogRepository;
import com.courierapp.auth.service.AuditLogService;
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
    public void log(String module, String action, Long userId, String username, String performedBy, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .module(module)
                    .action(action)
                    .entityId(userId)
                    .entityName(username)
                    .performedBy(performedBy != null ? performedBy : "system")
                    .details(details)
                    .createdAt(Instant.now())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to persist audit log [{}/{}]: {}", module, action, e.getMessage());
        }
    }
}
