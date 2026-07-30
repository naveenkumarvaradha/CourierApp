package com.courierapp.dto.dc;

import com.courierapp.enums.DcStatus;
import jakarta.validation.constraints.NotNull;

public record DcStatusUpdateRequest(
        @NotNull(message = "Target status is required")
        DcStatus status
) {
}
