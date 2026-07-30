package com.courierapp.mapper;

import com.courierapp.dto.dcreceipt.DcReceiptResponse;
import com.courierapp.entity.DcReceipt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DcMapper.class})
public interface DcReceiptMapper {
    @Mapping(target = "dc", source = "deliveryChallan")
    DcReceiptResponse toResponse(DcReceipt dcReceipt);
}
