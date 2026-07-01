package com.courierapp.mapper;

import com.courierapp.dto.admin.CourierWayResponse;
import com.courierapp.entity.CourierWay;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourierWayMapper {
    CourierWayResponse toResponse(CourierWay courierWay);
}
