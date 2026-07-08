package com.courierapp.admin.service.impl;

import com.courierapp.admin.dto.PageResponse;
import com.courierapp.admin.dto.audit.AuditLogResponse;
import com.courierapp.admin.entity.AuditLog;
import com.courierapp.admin.repository.AuditLogRepository;
import com.courierapp.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
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

    @Override
    public PageResponse<AuditLogResponse> getEntityHistory(String module, Long entityId, Pageable pageable) {
        return PageResponse.from(
                auditLogRepository.findByModuleAndEntityId(module, entityId, pageable),
                e -> new AuditLogResponse(e.getId(), e.getModule(), e.getAction(), e.getEntityId(),
                        e.getEntityName(), e.getPerformedBy(), e.getDetails(), e.getCreatedAt()));
    }
}
