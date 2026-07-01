package com.courierapp.controller;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.master.PartyRequest;
import com.courierapp.dto.master.PartyResponse;
import com.courierapp.service.PartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/master/parties")
@Tag(name = "Master - Parties")
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    @Operation(summary = "Search/list parties with pagination")
    public PageResponse<PartyResponse> list(@RequestParam(required = false) String name,
                                            @RequestParam(required = false) String city,
                                            @RequestParam(required = false) String pincode,
                                            @RequestParam(required = false) Boolean active,
                                            @PageableDefault(size = 20, sort = "partyName") Pageable pageable) {
        log.debug("GET /master/parties name={} city={} pincode={} active={}", name, city, pincode, active);
        return partyService.list(name, city, pincode, active, pageable);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('MASTER_VIEW') or hasAuthority('BOOKING_VIEW')")
    @Operation(summary = "List all ACTIVE parties (for dropdowns)")
    public List<PartyResponse> listActive() {
        log.debug("GET /master/parties/active");
        return partyService.listAllActive();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    public PartyResponse get(@PathVariable Long id) {
        log.debug("GET /master/parties/{}", id);
        return partyService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MASTER_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a party (goes to PENDING_APPROVAL)")
    public PartyResponse create(@Valid @RequestBody PartyRequest request) {
        log.info("POST /master/parties name={}", request.partyName());
        return partyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_UPDATE')")
    public PartyResponse update(@PathVariable Long id, @Valid @RequestBody PartyRequest request) {
        log.info("PUT /master/parties/{}", id);
        return partyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /master/parties/{}", id);
        partyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MASTER_APPROVE')")
    @Operation(summary = "Approve a pending party (must be a designated master-data approver)")
    public PartyResponse approve(@PathVariable Long id, Authentication authentication) {
        log.info("POST /master/parties/{}/approve by user='{}'", id, authentication.getName());
        return partyService.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MASTER_APPROVE')")
    @Operation(summary = "Reject a pending party")
    public PartyResponse reject(@PathVariable Long id,
                                @RequestParam(required = false) String remarks,
                                Authentication authentication) {
        log.info("POST /master/parties/{}/reject by user='{}'", id, authentication.getName());
        return partyService.reject(id, authentication.getName(), remarks);
    }
}
