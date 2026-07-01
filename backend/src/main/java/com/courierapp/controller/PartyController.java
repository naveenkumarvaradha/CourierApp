package com.courierapp.controller;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.master.PartyRequest;
import com.courierapp.dto.master.PartyResponse;
import com.courierapp.service.PartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return partyService.list(name, city, pincode, active, pageable);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('MASTER_VIEW') or hasAuthority('BOOKING_VIEW')")
    @Operation(summary = "List all active parties (for dropdowns)")
    public List<PartyResponse> listActive() {
        return partyService.listAllActive();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_VIEW')")
    public PartyResponse get(@PathVariable Long id) {
        return partyService.get(id);
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
}
