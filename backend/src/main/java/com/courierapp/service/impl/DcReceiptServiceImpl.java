package com.courierapp.service.impl;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.dc.DcResponse;
import com.courierapp.dto.dcreceipt.DcReceiptRequest;
import com.courierapp.dto.dcreceipt.DcReceiptResponse;
import com.courierapp.entity.DcReceipt;
import com.courierapp.entity.DeliveryChallan;
import com.courierapp.enums.DcStatus;
import com.courierapp.enums.DcType;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.DcMapper;
import com.courierapp.mapper.DcReceiptMapper;
import com.courierapp.repository.CompanySettingsRepository;
import com.courierapp.repository.DcReceiptRepository;
import com.courierapp.repository.DeliveryChallanRepository;
import com.courierapp.service.AuditLogService;
import com.courierapp.service.DcReceiptNumberGenerator;
import com.courierapp.service.DcReceiptService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
public class DcReceiptServiceImpl implements DcReceiptService {

    private final DcReceiptRepository dcReceiptRepository;
    private final DeliveryChallanRepository deliveryChallanRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final DcReceiptNumberGenerator dcReceiptNumberGenerator;
    private final DcReceiptMapper dcReceiptMapper;
    private final DcMapper dcMapper;
    private final AuditLogService auditLogService;

    public DcReceiptServiceImpl(DcReceiptRepository dcReceiptRepository,
                                DeliveryChallanRepository deliveryChallanRepository,
                                CompanySettingsRepository companySettingsRepository,
                                DcReceiptNumberGenerator dcReceiptNumberGenerator,
                                DcReceiptMapper dcReceiptMapper,
                                DcMapper dcMapper,
                                AuditLogService auditLogService) {
        this.dcReceiptRepository = dcReceiptRepository;
        this.deliveryChallanRepository = deliveryChallanRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.dcReceiptNumberGenerator = dcReceiptNumberGenerator;
        this.dcReceiptMapper = dcReceiptMapper;
        this.dcMapper = dcMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DcReceiptResponse> search(String receiptNumber, LocalDate fromDate, LocalDate toDate,
                                                  Pageable pageable) {
        Specification<DcReceipt> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(receiptNumber)) {
                predicates.add(cb.like(cb.lower(root.get("receiptNumber")), "%" + receiptNumber.toLowerCase() + "%"));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receiptDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receiptDate"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<DcReceipt> page = dcReceiptRepository.findAll(spec, pageable);
        return PageResponse.from(page, dcReceiptMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DcReceiptResponse get(Long id) {
        return dcReceiptMapper.toResponse(findReceipt(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DcResponse> listEligibleDcs(Pageable pageable) {
        Specification<DeliveryChallan> spec = (root, query, cb) -> {
            Predicate typePred = cb.equal(root.get("dcType"), DcType.RETURNABLE);
            Predicate statusPred = root.get("status").in(List.of(DcStatus.ISSUED, DcStatus.DELIVERED));

            Subquery<Long> sub = java.util.Objects.requireNonNull(query).subquery(Long.class);
            Root<DcReceipt> receiptRoot = sub.from(DcReceipt.class);
            sub.select(receiptRoot.get("id"))
                    .where(cb.equal(receiptRoot.get("deliveryChallan").get("id"), root.get("id")));
            Predicate noReceiptPred = cb.not(cb.exists(sub));

            return cb.and(typePred, statusPred, noReceiptPred);
        };
        Page<DeliveryChallan> page = deliveryChallanRepository.findAll(spec, pageable);
        return PageResponse.from(page, dcMapper::toResponse);
    }

    @Override
    public DcReceiptResponse create(DcReceiptRequest request) {
        DeliveryChallan dc = deliveryChallanRepository.findById(request.dcId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery challan", request.dcId()));
        if (dc.getDcType() != DcType.RETURNABLE) {
            throw new BusinessException("Only Returnable DCs can have a receipt confirmed");
        }
        if (dc.getStatus() != DcStatus.ISSUED && dc.getStatus() != DcStatus.DELIVERED) {
            throw new BusinessException("Only ISSUED or DELIVERED DCs can be received back");
        }
        if (dcReceiptRepository.existsByDeliveryChallanId(dc.getId())) {
            throw new BusinessException("This DC already has a receipt confirmed");
        }

        DcReceipt receipt = new DcReceipt();
        receipt.setDeliveryChallan(dc);
        receipt.setPreviousDcStatus(dc.getStatus());
        receipt.setReceivedBy(currentUsername());
        receipt.setRemarks(request.remarks());

        String companyCode = resolveCompanyCode();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        receipt.setReceiptDate(today);
        receipt.setReceiptNumber(dcReceiptNumberGenerator.next(today, companyCode));

        DcReceipt saved = dcReceiptRepository.save(receipt);

        dc.setStatus(DcStatus.RETURNED);
        deliveryChallanRepository.save(dc);

        log.info("DC receipt confirmed: id={}, receiptNumber={}, dcId={}",
                saved.getId(), saved.getReceiptNumber(), dc.getId());
        auditLogService.log("DC_RECEIPT", "CREATE", saved.getId(), saved.getReceiptNumber(),
                currentUsername(), "DC=" + dc.getDcNumber());
        return dcReceiptMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        DcReceipt receipt = findReceipt(id);
        DeliveryChallan dc = receipt.getDeliveryChallan();
        dc.setStatus(receipt.getPreviousDcStatus());
        deliveryChallanRepository.save(dc);

        String number = receipt.getReceiptNumber();
        dcReceiptRepository.delete(receipt);
        auditLogService.log("DC_RECEIPT", "DELETE", id, number, currentUsername(),
                "Reverted DC=" + dc.getDcNumber() + " to " + dc.getStatus());
    }

    private DcReceipt findReceipt(Long id) {
        return dcReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DC receipt", id));
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
}
