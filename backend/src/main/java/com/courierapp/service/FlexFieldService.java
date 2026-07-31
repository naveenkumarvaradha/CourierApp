package com.courierapp.service;

import com.courierapp.dto.admin.*;
import com.courierapp.dto.flexfield.FlexFieldValueRequest;
import com.courierapp.dto.flexfield.FlexFieldValueResponse;
import com.courierapp.entity.FlexField;
import com.courierapp.entity.FlexFieldOption;
import com.courierapp.entity.FlexFieldValue;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.repository.FlexFieldOptionRepository;
import com.courierapp.repository.FlexFieldRepository;
import com.courierapp.repository.FlexFieldValueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Service
@Transactional
public class FlexFieldService {

    private final FlexFieldRepository fieldRepo;
    private final FlexFieldOptionRepository optionRepo;
    private final FlexFieldValueRepository valueRepo;
    private final AuditLogService auditLogService;

    public FlexFieldService(FlexFieldRepository fieldRepo,
                            FlexFieldOptionRepository optionRepo,
                            FlexFieldValueRepository valueRepo,
                            AuditLogService auditLogService) {
        this.fieldRepo = fieldRepo;
        this.optionRepo = optionRepo;
        this.valueRepo = valueRepo;
        this.auditLogService = auditLogService;
    }

    // ── Field definitions ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FlexFieldDefinitionResponse> listFields(String module) {
        List<FlexField> fields = module != null
                ? fieldRepo.findByModuleOrderBySortOrderAscFieldNameAsc(module.toUpperCase())
                : fieldRepo.findAll();
        return fields.stream().map(this::toFieldResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FlexFieldDefinitionResponse> listActiveFields(String module) {
        return fieldRepo.findByModuleAndActiveTrueOrderBySortOrderAscFieldNameAsc(module.toUpperCase())
                .stream().map(this::toFieldResponse).toList();
    }

    public FlexFieldDefinitionResponse createField(FlexFieldDefinitionRequest req) {
        log.info("Creating flex field: module={}, name={}, type={}", req.module(), req.fieldName(), req.fieldType());
        String mod = req.module().toUpperCase();
        if (fieldRepo.existsByModuleAndFieldNameIgnoreCase(mod, req.fieldName())) {
            throw new BusinessException("Field '" + req.fieldName() + "' already exists for module " + mod);
        }
        FlexField field = new FlexField();
        applyField(field, req);
        FlexField saved = fieldRepo.save(field);
        log.info("Flex field created id={}", saved.getId());
        auditLogService.log("FLEX_FIELD", "CREATE", saved.getId(),
                saved.getModule() + "." + saved.getFieldName(), currentUsername(),
                "type=" + saved.getFieldType());
        return toFieldResponse(saved);
    }

    public FlexFieldDefinitionResponse updateField(Long id, FlexFieldDefinitionRequest req) {
        log.info("Updating flex field id={}", id);
        FlexField field = findField(id);
        String mod = req.module().toUpperCase();
        if (!field.getFieldName().equalsIgnoreCase(req.fieldName())
                && fieldRepo.existsByModuleAndFieldNameIgnoreCase(mod, req.fieldName())) {
            throw new BusinessException("Field '" + req.fieldName() + "' already exists for module " + mod);
        }
        applyField(field, req);
        FlexField saved = fieldRepo.save(field);
        auditLogService.log("FLEX_FIELD", "UPDATE", saved.getId(),
                saved.getModule() + "." + saved.getFieldName(), currentUsername(), null);
        return toFieldResponse(saved);
    }

    public void deleteField(Long id) {
        log.info("Deleting flex field id={}", id);
        FlexField f = findField(id);
        String name = f.getModule() + "." + f.getFieldName();
        fieldRepo.delete(f);
        auditLogService.log("FLEX_FIELD", "DELETE", id, name, currentUsername(), null);
    }

    private void applyField(FlexField field, FlexFieldDefinitionRequest req) {
        field.setModule(req.module().toUpperCase());
        field.setFieldName(req.fieldName());
        field.setFieldLabel(req.fieldLabel());
        field.setFieldType(req.fieldType());
        field.setRequired(req.required());
        field.setActive(req.active());
        field.setSortOrder(req.sortOrder());
    }

    // ── Dropdown options ─────────────────────────────────────────

    public FlexFieldOptionResponse addOption(Long fieldId, FlexFieldOptionRequest req) {
        FlexField field = findField(fieldId);
        log.info("Adding option '{}' to field id={}", req.optionValue(), fieldId);
        FlexFieldOption opt = new FlexFieldOption();
        opt.setField(field);
        opt.setOptionValue(req.optionValue());
        opt.setSortOrder(req.sortOrder());
        opt.setActive(req.active());
        return toOptionResponse(optionRepo.save(opt));
    }

    public FlexFieldOptionResponse updateOption(Long fieldId, Long optionId, FlexFieldOptionRequest req) {
        FlexFieldOption opt = optionRepo.findById(optionId)
                .filter(o -> o.getField().getId().equals(fieldId))
                .orElseThrow(() -> new ResourceNotFoundException("Option", optionId));
        opt.setOptionValue(req.optionValue());
        opt.setSortOrder(req.sortOrder());
        opt.setActive(req.active());
        return toOptionResponse(optionRepo.save(opt));
    }

    public void deleteOption(Long fieldId, Long optionId) {
        FlexFieldOption opt = optionRepo.findById(optionId)
                .filter(o -> o.getField().getId().equals(fieldId))
                .orElseThrow(() -> new ResourceNotFoundException("Option", optionId));
        optionRepo.delete(opt);
    }

    // ── Field values (per entity) ─────────────────────────────────

    public FlexFieldValueResponse saveValues(String module, Long entityId, FlexFieldValueRequest req) {
        log.info("Saving {} flex values for module={} entityId={}", req.values().size(), module, entityId);
        String mod = module.toUpperCase();
        req.values().forEach((fieldId, value) -> {
            FlexFieldValue fv = valueRepo.findByModuleAndEntityIdAndFieldId(mod, entityId, fieldId)
                    .orElse(new FlexFieldValue());
            fv.setModule(mod);
            fv.setEntityId(entityId);
            fv.setField(findField(fieldId));
            fv.setFieldValue(value);
            valueRepo.save(fv);
        });
        return getValues(module, entityId);
    }

    @Transactional(readOnly = true)
    public FlexFieldValueResponse getValues(String module, Long entityId) {
        Map<Long, String> map = new LinkedHashMap<>();
        valueRepo.findByModuleAndEntityId(module.toUpperCase(), entityId)
                .forEach(fv -> map.put(fv.getField().getId(), fv.getFieldValue()));
        return new FlexFieldValueResponse(map);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private FlexField findField(Long id) {
        return fieldRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Flex field", id));
    }

    private FlexFieldDefinitionResponse toFieldResponse(FlexField f) {
        List<FlexFieldOptionResponse> opts = f.getOptions().stream()
                .map(this::toOptionResponse).toList();
        return new FlexFieldDefinitionResponse(f.getId(), f.getModule(), f.getFieldName(),
                f.getFieldLabel(), f.getFieldType(), f.isRequired(), f.isActive(), f.getSortOrder(), opts);
    }

    private FlexFieldOptionResponse toOptionResponse(FlexFieldOption o) {
        return new FlexFieldOptionResponse(o.getId(), o.getOptionValue(), o.getSortOrder(), o.isActive());
    }
}
