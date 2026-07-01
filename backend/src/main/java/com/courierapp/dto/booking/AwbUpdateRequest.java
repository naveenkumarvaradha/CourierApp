package com.courierapp.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AwbUpdateRequest(
        @NotBlank(message = "AWB number is required")
        @Pattern(regexp = "\\d+", message = "AWB number must contain digits only")
        @Size(max = 20, message = "AWB number must be 20 digits or less")
        String awbNumber
) {
}
