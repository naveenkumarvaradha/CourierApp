package com.courierapp.mapper;

import com.courierapp.dto.master.PartyResponse;
import com.courierapp.entity.Party;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PartyMapper {

    @Mapping(target = "companyName", source = "companyName")
    @Mapping(target = "pendingApprovers", ignore = true)
    PartyResponse toResponse(Party party);
}
