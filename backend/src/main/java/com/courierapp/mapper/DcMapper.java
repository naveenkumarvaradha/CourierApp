package com.courierapp.mapper;

import com.courierapp.dto.dc.DcResponse;
import com.courierapp.entity.DeliveryChallan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {UnitMapper.class, PartyMapper.class, CourierWayMapper.class, PackageTypeMapper.class})
public interface DcMapper {
    @Mapping(target = "pendingApprovers", ignore = true)
    DcResponse toResponse(DeliveryChallan deliveryChallan);
}
