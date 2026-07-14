package com.courierapp.controller;

import com.courierapp.dto.admin.*;
import com.courierapp.dto.flexfield.FlexFieldValueRequest;
import com.courierapp.dto.flexfield.FlexFieldValueResponse;
import com.courierapp.service.FlexFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Tag(name = "Flex Fields")
public class FlexFieldController {

    private final FlexFieldService flexFieldService;

    public FlexFieldController(FlexFieldService flexFieldService) {
        this.flexFieldService = flexFieldService;
    }

    // ----- Field definitions (admin) -----

    @GetMapping("/admin/flex-fields")
    @PreAuthorize("hasAuthority('ADMIN_VIEW')")
    @Operation(summary = "List flex fields, optionally filtered by module")
    public List<FlexFieldDefinitionResponse> listFields(@RequestParam(required = false) String module) {
        return flexFieldService.listFields(module);
    }

    @GetMapping("/flex-fields/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List active flex fields for a module (used by forms)")
    public List<FlexFieldDefinitionResponse> listActiveFields(@RequestParam String module) {
        return flexFieldService.listActiveFields(module);
    }

    @PostMapping("/admin/flex-fields")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public FlexFieldDefinitionResponse createField(@Valid @RequestBody FlexFieldDefinitionRequest request) {
        log.info("POST /admin/flex-fields module={} name={}", request.module(), request.fieldName());
        return flexFieldService.createField(request);
    }

    @PutMapping("/admin/flex-fields/{id}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public FlexFieldDefinitionResponse updateField(@PathVariable Long id,
                                                   @Valid @RequestBody FlexFieldDefinitionRequest request) {
        log.info("PUT /admin/flex-fields/{}", id);
        return flexFieldService.updateField(id, request);
    }

    @DeleteMapping("/admin/flex-fields/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteField(@PathVariable Long id) {
        log.info("DELETE /admin/flex-fields/{}", id);
        flexFieldService.deleteField(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Options -----

    @PostMapping("/admin/flex-fields/{fieldId}/options")
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public FlexFieldOptionResponse addOption(@PathVariable Long fieldId,
                                             @Valid @RequestBody FlexFieldOptionRequest request) {
        log.info("POST /admin/flex-fields/{}/options value={}", fieldId, request.optionValue());
        return flexFieldService.addOption(fieldId, request);
    }

    @PutMapping("/admin/flex-fields/{fieldId}/options/{optionId}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public FlexFieldOptionResponse updateOption(@PathVariable Long fieldId,
                                                @PathVariable Long optionId,
                                                @Valid @RequestBody FlexFieldOptionRequest request) {
        return flexFieldService.updateOption(fieldId, optionId, request);
    }

    @DeleteMapping("/admin/flex-fields/{fieldId}/options/{optionId}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteOption(@PathVariable Long fieldId, @PathVariable Long optionId) {
        flexFieldService.deleteOption(fieldId, optionId);
        return ResponseEntity.noContent().build();
    }

    // ----- Field values (per entity) -----

    @GetMapping("/flex-field-values/{module}/{entityId}")
    @PreAuthorize("hasAuthority('BOOKING_VIEW') or hasAuthority('MASTER_VIEW') or hasAuthority('ADMIN_VIEW')")
    public FlexFieldValueResponse getValues(@PathVariable String module, @PathVariable Long entityId) {
        return flexFieldService.getValues(module, entityId);
    }

    @PostMapping("/flex-field-values/{module}/{entityId}")
    @PreAuthorize("hasAuthority('BOOKING_UPDATE') or hasAuthority('MASTER_UPDATE') or hasAuthority('ADMIN_UPDATE')")
    public FlexFieldValueResponse saveValues(@PathVariable String module,
                                             @PathVariable Long entityId,
                                             @RequestBody FlexFieldValueRequest request) {
        log.info("POST /flex-field-values/{}/{}", module, entityId);
        return flexFieldService.saveValues(module, entityId, request);
    }
}
