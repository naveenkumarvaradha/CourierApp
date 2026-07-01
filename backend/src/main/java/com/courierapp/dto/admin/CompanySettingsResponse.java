package com.courierapp.dto.admin;

public record CompanySettingsResponse(
        Long id,
        String companyName,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode,
        String country,
        String phone,
        String email,
        String gstin
) {}
