package com.courierapp.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FlexFieldOptionRequest(
        @NotBlank @Size(max = 200) String optionValue,
        int sortOrder,
        boolean active
) {}
