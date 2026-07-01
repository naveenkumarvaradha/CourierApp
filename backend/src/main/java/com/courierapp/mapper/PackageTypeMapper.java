package com.courierapp.mapper;

import com.courierapp.dto.admin.PackageTypeResponse;
import com.courierapp.entity.PackageType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PackageTypeMapper {
    PackageTypeResponse toResponse(PackageType packageType);
}
