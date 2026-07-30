package com.courierapp.controller;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.dc.DcResponse;
import com.courierapp.dto.dcreceipt.DcReceiptRequest;
import com.courierapp.dto.dcreceipt.DcReceiptResponse;
import com.courierapp.service.DcReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/dc-receipts")
@Tag(name = "DC Receipts")
public class DcReceiptController {

    private final DcReceiptService dcReceiptService;

    public DcReceiptController(DcReceiptService dcReceiptService) {
        this.dcReceiptService = dcReceiptService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RECEIPT_VIEW')")
    public PageResponse<DcReceiptResponse> search(
            @RequestParam(required = false) String receiptNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return dcReceiptService.search(receiptNumber, fromDate, toDate, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RECEIPT_VIEW')")
    public DcReceiptResponse get(@PathVariable Long id) {
        return dcReceiptService.get(id);
    }

    @GetMapping("/eligible")
    @PreAuthorize("hasAuthority('RECEIPT_VIEW') or hasAuthority('RECEIPT_CREATE')")
    @Operation(summary = "List Returnable DCs (Issued/Delivered) awaiting a receipt confirmation")
    public PageResponse<DcResponse> eligible(@PageableDefault(size = 50, sort = "id") Pageable pageable) {
        return dcReceiptService.listEligibleDcs(pageable);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RECEIPT_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Confirm receipt of a returned DC — moves the DC to RETURNED status")
    public DcReceiptResponse create(@Valid @RequestBody DcReceiptRequest request) {
        return dcReceiptService.create(request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('RECEIPT_DELETE')")
    @Operation(summary = "Undo a receipt confirmation — restores the DC to its prior status")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dcReceiptService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
