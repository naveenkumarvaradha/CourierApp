package com.courierapp.mapper;

import com.courierapp.dto.master.PartyResponse;
import com.courierapp.entity.Party;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PartyMapper {
    PartyResponse toResponse(Party party);
}
