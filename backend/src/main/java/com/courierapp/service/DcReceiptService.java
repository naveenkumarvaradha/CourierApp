package com.courierapp.service;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.dc.DcResponse;
import com.courierapp.dto.dcreceipt.DcReceiptRequest;
import com.courierapp.dto.dcreceipt.DcReceiptResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface DcReceiptService {
    PageResponse<DcReceiptResponse> search(String receiptNumber, LocalDate fromDate, LocalDate toDate, Pageable pageable);
    DcReceiptResponse get(Long id);
    PageResponse<DcResponse> listEligibleDcs(Pageable pageable);
    DcReceiptResponse create(DcReceiptRequest request);
    void delete(Long id);
}
