package com.courierapp.dto.booking;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(
        @Size(max = 500, message = "Remarks must be at most 500 characters")
        String remarks
) {
}
