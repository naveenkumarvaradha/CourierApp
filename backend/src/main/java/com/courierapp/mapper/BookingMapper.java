package com.courierapp.mapper;

import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.entity.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PartyMapper.class, CourierWayMapper.class, PackageTypeMapper.class, UnitMapper.class})
public interface BookingMapper {
    @org.mapstruct.Mapping(target = "pendingApprovers", ignore = true)
    BookingResponse toResponse(Booking booking);
}
