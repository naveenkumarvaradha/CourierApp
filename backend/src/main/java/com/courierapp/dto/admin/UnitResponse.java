package com.courierapp.dto.admin;

public record UnitResponse(
        Long id,
        String unitName,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode,
        String country,
        String phone,
        String email,
        String gstin,
        boolean defaultUnit,
        boolean active
) {
}
