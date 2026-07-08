package com.courierapp.service;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.audit.AuditLogResponse;
import com.courierapp.entity.AuditLog;
import com.courierapp.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AuditLogService {

    private final AuditLogRepository repo;

    public AuditLogService(AuditLogRepository repo) {
        this.repo = repo;
    }

    /**
     * Record an audit event in its own transaction so it never rolls back with
     * the caller, and asynchronously so it does not slow down the main flow.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String module, String action, Long entityId, String entityName,
                    String performedBy, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .module(module.toUpperCase())
                    .action(action.toUpperCase())
                    .entityId(entityId)
                    .entityName(entityName)
                    .performedBy(performedBy != null ? performedBy : "system")
                    .details(details)
                    .build();
            repo.save(entry);
        } catch (Exception ex) {
            // Audit failure must never affect the business flow
            log.error("Failed to save audit log module={} action={} entity={}: {}",
                    module, action, entityId, ex.getMessage());
        }
    }

    /** Convenience overload without details. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String module, String action, Long entityId, String entityName, String performedBy) {
        log(module, action, entityId, entityName, performedBy, null);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String module, String action,
                                                  String performedBy, LocalDate fromDate,
                                                  LocalDate toDate, Pageable pageable) {
        Specification<AuditLog> spec = buildSpec(module, action, performedBy, fromDate, toDate);
        Page<AuditLog> page = repo.findAll(spec, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    private Specification<AuditLog> buildSpec(String module, String action,
                                               String performedBy, LocalDate fromDate,
                                               LocalDate toDate) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (StringUtils.hasText(module)) {
                preds.add(cb.equal(cb.upper(root.get("module")), module.toUpperCase()));
            }
            if (StringUtils.hasText(action)) {
                preds.add(cb.equal(cb.upper(root.get("action")), action.toUpperCase()));
            }
            if (StringUtils.hasText(performedBy)) {
                preds.add(cb.like(cb.lower(root.get("performedBy")),
                        "%" + performedBy.toLowerCase() + "%"));
            }
            if (fromDate != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        fromDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (toDate != null) {
                preds.add(cb.lessThan(root.get("createdAt"),
                        toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getEntityHistory(String module, Long entityId, Pageable pageable) {
        Page<AuditLog> page = repo.findByModuleAndEntityIdOrderByCreatedAtDesc(module.toUpperCase(), entityId, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getModule(), a.getAction(),
                a.getEntityId(), a.getEntityName(), a.getPerformedBy(),
                a.getDetails(), a.getCreatedAt());
    }
}
