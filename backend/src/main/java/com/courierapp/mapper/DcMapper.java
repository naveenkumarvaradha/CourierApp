package com.courierapp.mapper;

import com.courierapp.dto.dc.DcResponse;
import com.courierapp.entity.DeliveryChallan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {BookingMapper.class, UnitMapper.class})
public interface DcMapper {
    DcResponse toResponse(DeliveryChallan deliveryChallan);
}
