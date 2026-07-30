package com.courierapp.mapper;

import com.courierapp.dto.admin.UnitResponse;
import com.courierapp.entity.Unit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UnitMapper {
    UnitResponse toResponse(Unit unit);
}
