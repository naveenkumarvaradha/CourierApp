package com.courierapp.dto.admin;

import com.courierapp.enums.FlexFieldType;

import java.util.List;

public record FlexFieldDefinitionResponse(
        Long id,
        String module,
        String fieldName,
        String fieldLabel,
        FlexFieldType fieldType,
        boolean required,
        boolean active,
        int sortOrder,
        List<FlexFieldOptionResponse> options
) {}
