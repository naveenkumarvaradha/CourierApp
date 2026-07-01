package com.courierapp.mapper;

import com.courierapp.dto.admin.PermissionResponse;
import com.courierapp.entity.Permission;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionResponse toResponse(Permission permission);
    List<PermissionResponse> toResponseList(List<Permission> permissions);
}
