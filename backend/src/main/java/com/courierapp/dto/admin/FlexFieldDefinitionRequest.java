package com.courierapp.dto.admin;

import com.courierapp.enums.FlexFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FlexFieldDefinitionRequest(
        @NotBlank @Size(max = 50)  String module,
        @NotBlank @Size(max = 100) String fieldName,
        @NotBlank @Size(max = 200) String fieldLabel,
        @NotNull FlexFieldType fieldType,
        boolean required,
        boolean active,
        int sortOrder
) {}
