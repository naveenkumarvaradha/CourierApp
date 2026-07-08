package com.courierapp.dto.admin;

public record StickerFieldDto(
        String fieldKey,
        String label,
        boolean visible,
        int sortOrder,
        String section   // HEADER | FROM | TO | DETAILS | BOTTOM  (read-only metadata)
) {}
