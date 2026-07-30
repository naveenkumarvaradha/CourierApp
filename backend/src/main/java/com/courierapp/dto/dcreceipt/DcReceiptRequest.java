package com.courierapp.dto.dcreceipt;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DcReceiptRequest(
        @NotNull(message = "DC is required")
        Long dcId,

        @Size(max = 500)
        String remarks
) {
}
