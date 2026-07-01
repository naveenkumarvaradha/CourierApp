package com.courierapp.service;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.master.PartyRequest;
import com.courierapp.dto.master.PartyResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PartyService {
    PageResponse<PartyResponse> list(String name, String city, String pincode, Boolean active, Pageable pageable);
    List<PartyResponse> listAllActive();
    PartyResponse get(Long id);
    PartyResponse create(PartyRequest request);
    PartyResponse update(Long id, PartyRequest request);
    void delete(Long id);
    PartyResponse approve(Long id, String approverUsername);
    PartyResponse reject(Long id, String approverUsername, String remarks);
}
