package com.courierapp.mapper;

import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.entity.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PartyMapper.class})
public interface BookingMapper {
    BookingResponse toResponse(Booking booking);
}
