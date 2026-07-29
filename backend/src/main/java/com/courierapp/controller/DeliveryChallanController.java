package com.courierapp.controller;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.dc.DcRequest;
import com.courierapp.dto.dc.DcResponse;
import com.courierapp.dto.dc.DcStatusUpdateRequest;
import com.courierapp.enums.DcStatus;
import com.courierapp.repository.DeliveryChallanRepository;
import com.courierapp.service.DeliveryChallanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/dc")
@Tag(name = "Delivery Challans")
public class DeliveryChallanController {

    private final DeliveryChallanService deliveryChallanService;
    private final DeliveryChallanRepository deliveryChallanRepository;

    public DeliveryChallanController(DeliveryChallanService deliveryChallanService,
                                     DeliveryChallanRepository deliveryChallanRepository) {
        this.deliveryChallanService = deliveryChallanService;
        this.deliveryChallanRepository = deliveryChallanRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DELIVERY_CHALLAN_VIEW')")
    public PageResponse<DcResponse> search(
            @RequestParam(required = false) String dcNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) DcStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return deliveryChallanService.search(dcNumber, fromDate, toDate, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_CHALLAN_VIEW')")
    public DcResponse get(@PathVariable Long id) {
        return deliveryChallanService.get(id);
    }

    @GetMapping("/by-booking/{bookingId}")
    @PreAuthorize("hasAuthority('DELIVERY_CHALLAN_VIEW')")
    @Operation(summary = "Check whether a booking already has a delivery challan, and fetch it if so")
    public ResponseEntity<DcResponse> getByBooking(@PathVariable Long bookingId) {
        return deliveryChallanRepository.findByBookingId(bookingId)
                .map(dc -> ResponseEntity.ok(deliveryChallanService.get(dc.getId())))
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DELIVERY_CHALLAN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public DcResponse create(@Valid @RequestBody DcRequest request) {
        return deliveryChallanService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_CHALLAN_UPDATE')")
    public DcResponse update(@PathVariable Long id, @Valid @RequestBody DcRequest request) {
        return deliveryChallanService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELIVERY_CHALLAN_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deliveryChallanService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('DELIVERY_CHALLAN_UPDATE')")
    public DcResponse changeStatus(@PathVariable Long id, @Valid @RequestBody DcStatusUpdateRequest request) {
        return deliveryChallanService.changeStatus(id, request.status());
    }

    @GetMapping(value = "/{id}/print", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('DELIVERY_CHALLAN_PRINT')")
    @Operation(summary = "Generate the printable Delivery Challan PDF")
    public ResponseEntity<byte[]> print(@PathVariable Long id) {
        byte[] pdf = deliveryChallanService.generatePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=dc-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
