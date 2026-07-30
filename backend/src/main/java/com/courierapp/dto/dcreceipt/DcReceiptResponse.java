package com.courierapp.dto.dcreceipt;

import com.courierapp.dto.dc.DcResponse;

import java.time.Instant;
import java.time.LocalDate;

public record DcReceiptResponse(
        Long id,
        String receiptNumber,
        LocalDate receiptDate,
        DcResponse dc,
        String receivedBy,
        String remarks,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
}
