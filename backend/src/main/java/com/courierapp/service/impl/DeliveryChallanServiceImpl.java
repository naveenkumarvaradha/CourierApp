package com.courierapp.service.impl;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.dc.DcRequest;
import com.courierapp.dto.dc.DcResponse;
import com.courierapp.entity.Booking;
import com.courierapp.entity.CompanySettings;
import com.courierapp.entity.DeliveryChallan;
import com.courierapp.entity.Unit;
import com.courierapp.enums.DcStatus;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.DcMapper;
import com.courierapp.repository.BookingRepository;
import com.courierapp.repository.CompanySettingsRepository;
import com.courierapp.repository.DeliveryChallanRepository;
import com.courierapp.repository.UnitRepository;
import com.courierapp.service.AuditLogService;
import com.courierapp.service.DcNumberGenerator;
import com.courierapp.service.DcPdfService;
import com.courierapp.service.DeliveryChallanService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
public class DeliveryChallanServiceImpl implements DeliveryChallanService {

    private final DeliveryChallanRepository deliveryChallanRepository;
    private final BookingRepository bookingRepository;
    private final UnitRepository unitRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final DcNumberGenerator dcNumberGenerator;
    private final DcMapper dcMapper;
    private final DcPdfService dcPdfService;
    private final AuditLogService auditLogService;

    public DeliveryChallanServiceImpl(DeliveryChallanRepository deliveryChallanRepository,
                                      BookingRepository bookingRepository,
                                      UnitRepository unitRepository,
                                      CompanySettingsRepository companySettingsRepository,
                                      DcNumberGenerator dcNumberGenerator,
                                      DcMapper dcMapper,
                                      DcPdfService dcPdfService,
                                      AuditLogService auditLogService) {
        this.deliveryChallanRepository = deliveryChallanRepository;
        this.bookingRepository = bookingRepository;
        this.unitRepository = unitRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.dcNumberGenerator = dcNumberGenerator;
        this.dcMapper = dcMapper;
        this.dcPdfService = dcPdfService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DcResponse> search(String dcNumber, LocalDate fromDate, LocalDate toDate,
                                           DcStatus status, Pageable pageable) {
        Specification<DeliveryChallan> spec = buildSpec(dcNumber, fromDate, toDate, status);
        Page<DeliveryChallan> page = deliveryChallanRepository.findAll(spec, pageable);
        return PageResponse.from(page, dcMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DcResponse get(Long id) {
        return dcMapper.toResponse(findDc(id));
    }

    @Override
    public DcResponse create(DcRequest request) {
        if (deliveryChallanRepository.existsByBookingId(request.bookingId())) {
            throw new BusinessException("This booking already has a delivery challan");
        }
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.bookingId()));
        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit", request.unitId()));

        DeliveryChallan dc = new DeliveryChallan();
        dc.setBooking(booking);
        applyFields(dc, request, unit);

        String companyCode = resolveCompanyCode();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        dc.setDcNumber(dcNumberGenerator.next(today, companyCode));

        DeliveryChallan saved = deliveryChallanRepository.save(dc);
        log.info("Delivery challan created: id={}, dcNumber={}, bookingId={}",
                saved.getId(), saved.getDcNumber(), booking.getId());
        auditLogService.log("DELIVERY_CHALLAN", "CREATE", saved.getId(), saved.getDcNumber(),
                currentUsername(), "Booking=" + booking.getBookingNumber());
        return dcMapper.toResponse(saved);
    }

    @Override
    public DcResponse update(Long id, DcRequest request) {
        DeliveryChallan dc = findDc(id);
        if (!dc.getBooking().getId().equals(request.bookingId())) {
            throw new BusinessException("A delivery challan's linked booking cannot be changed");
        }
        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit", request.unitId()));
        applyFields(dc, request, unit);
        DeliveryChallan saved = deliveryChallanRepository.save(dc);
        auditLogService.log("DELIVERY_CHALLAN", "UPDATE", saved.getId(), saved.getDcNumber(),
                currentUsername(), "status=" + saved.getStatus());
        return dcMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        DeliveryChallan dc = findDc(id);
        String number = dc.getDcNumber();
        deliveryChallanRepository.delete(dc);
        auditLogService.log("DELIVERY_CHALLAN", "DELETE", id, number, currentUsername(), null);
    }

    @Override
    public DcResponse changeStatus(Long id, DcStatus status) {
        DeliveryChallan dc = findDc(id);
        dc.setStatus(status);
        DeliveryChallan saved = deliveryChallanRepository.save(dc);
        auditLogService.log("DELIVERY_CHALLAN", "STATUS_CHANGE", saved.getId(), saved.getDcNumber(),
                currentUsername(), "status=" + status);
        return dcMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long id) {
        DeliveryChallan dc = findDc(id);
        Long companyId = dc.getUnit().getCompany() != null ? dc.getUnit().getCompany().getId() : null;
        CompanySettings settings = (companyId != null)
                ? companySettingsRepository.findByCompanyId(companyId).orElse(null)
                : companySettingsRepository.findAll().stream().findFirst().orElse(null);
        byte[] pdf = dcPdfService.generate(dc, settings);
        auditLogService.log("DELIVERY_CHALLAN", "PRINT", dc.getId(), dc.getDcNumber(), currentUsername(), null);
        return pdf;
    }

    private void applyFields(DeliveryChallan dc, DcRequest request, Unit unit) {
        dc.setUnit(unit);
        dc.setDcDate(request.dcDate());
        dc.setVehicleNumber(request.vehicleNumber());
        dc.setDriverName(request.driverName());
        dc.setStatus(request.status());
        dc.setRemarks(request.remarks());
    }

    private DeliveryChallan findDc(Long id) {
        return deliveryChallanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery challan", id));
    }

    private String resolveCompanyCode() {
        return companySettingsRepository.findAll().stream().findFirst()
                .map(s -> s.getCompany() != null ? s.getCompany().getCompanyCode() : null)
                .orElse(null);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private Specification<DeliveryChallan> buildSpec(String dcNumber, LocalDate fromDate, LocalDate toDate,
                                                      DcStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(dcNumber)) {
                predicates.add(cb.like(cb.lower(root.get("dcNumber")), "%" + dcNumber.toLowerCase() + "%"));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dcDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dcDate"), toDate));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
