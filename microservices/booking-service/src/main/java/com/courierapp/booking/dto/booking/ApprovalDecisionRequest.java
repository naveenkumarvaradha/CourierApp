package com.courierapp.booking.dto.booking;

import jakarta.validation.constraints.NotBlank;

public record ApprovalDecisionRequest(@NotBlank String decision, String remarks) {}
