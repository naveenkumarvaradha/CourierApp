package com.courierapp.party.controller;

import com.courierapp.party.dto.PageResponse;
import com.courierapp.party.dto.approval.ApprovalInfoResponse;
import com.courierapp.party.dto.master.PartyRequest;
import com.courierapp.party.dto.master.PartyResponse;
import com.courierapp.party.entity.Party;
import com.courierapp.party.exception.ResourceNotFoundException;
import com.courierapp.party.repository.PartyRepository;
import com.courierapp.party.service.ApprovalAuthorizationService;
import com.courierapp.party.service.PartyService;
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
public class PartyController {

    private final PartyService partyService;
    private final PartyRepository partyRepository;
    private final ApprovalAuthorizationService approvalAuthorizationService;

    public PartyController(PartyService partyService,
                           PartyRepository partyRepository,
                           ApprovalAuthorizationService approvalAuthorizationService) {
        this.partyService = partyService;
        this.partyRepository = partyRepository;
        this.approvalAuthorizationService = approvalAuthorizationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    public PageResponse<PartyResponse> list(@RequestParam(required = false) String name,
                                            @RequestParam(required = false) String city,
                                            @RequestParam(required = false) String pincode,
                                            @RequestParam(required = false) Boolean active,
                                            @PageableDefault(size = 20, sort = "partyName") Pageable pageable) {
        return partyService.list(name, city, pincode, active, pageable);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('MASTER_VIEW') or hasAuthority('BOOKING_VIEW')")
    public List<PartyResponse> listActive() {
        return partyService.listAllActive();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    public PartyResponse get(@PathVariable Long id) {
        return partyService.get(id);
    }

    @GetMapping("/{id}/approval-info")
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    public ApprovalInfoResponse approvalInfo(@PathVariable Long id) {
        Party p = partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party", id));
        String creator = p.getCreatedBy();
        int currentLevel = p.getCurrentApprovalLevel();
        int maxLevel = approvalAuthorizationService.getMaxLevel(creator, "MASTER");
        List<String> approvers = approvalAuthorizationService
                .resolveApproversAtLevel(creator, "MASTER", currentLevel);
        return new ApprovalInfoResponse(currentLevel, maxLevel, approvers,
                "Level " + currentLevel + " of " + maxLevel);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MASTER_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PartyResponse create(@Valid @RequestBody PartyRequest request) {
        return partyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_UPDATE')")
    public PartyResponse update(@PathVariable Long id, @Valid @RequestBody PartyRequest request) {
        return partyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        partyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MASTER_APPROVE') or hasAuthority('ADMIN_VIEW')")
    public PartyResponse approve(@PathVariable Long id, Authentication authentication) {
        return partyService.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MASTER_APPROVE') or hasAuthority('ADMIN_VIEW')")
    public PartyResponse reject(@PathVariable Long id,
                                @RequestParam(required = false) String remarks,
                                Authentication authentication) {
        return partyService.reject(id, authentication.getName(), remarks);
    }
}
