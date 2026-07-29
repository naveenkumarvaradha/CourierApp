package com.courierapp.service;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.dc.DcRequest;
import com.courierapp.dto.dc.DcResponse;
import com.courierapp.enums.DcStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface DeliveryChallanService {
    PageResponse<DcResponse> search(String dcNumber, LocalDate fromDate, LocalDate toDate,
                                    DcStatus status, Pageable pageable);
    DcResponse get(Long id);
    DcResponse create(DcRequest request);
    DcResponse update(Long id, DcRequest request);
    void delete(Long id);
    DcResponse changeStatus(Long id, DcStatus status);
    byte[] generatePdf(Long id);
}
