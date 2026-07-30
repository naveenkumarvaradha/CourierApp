package com.courierapp.service.impl;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.booking.ApprovalDecisionRequest;
import com.courierapp.dto.dc.DcRequest;
import com.courierapp.dto.dc.DcResponse;
import com.courierapp.entity.CompanySettings;
import com.courierapp.entity.CourierWay;
import com.courierapp.entity.DeliveryChallan;
import com.courierapp.entity.PackageType;
import com.courierapp.entity.Party;
import com.courierapp.entity.Unit;
import com.courierapp.enums.DcStatus;
import com.courierapp.enums.ReceiverType;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.DcMapper;
import com.courierapp.repository.CompanySettingsRepository;
import com.courierapp.repository.CourierWayRepository;
import com.courierapp.repository.DeliveryChallanRepository;
import com.courierapp.repository.PackageTypeRepository;
import com.courierapp.repository.PartyRepository;
import com.courierapp.repository.UnitRepository;
import com.courierapp.service.ApprovalAuthorizationService;
import com.courierapp.service.AuditLogService;
import com.courierapp.service.DcNumberGenerator;
import com.courierapp.service.DcPdfService;
import com.courierapp.service.DeliveryChallanService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@Transactional
public class DeliveryChallanServiceImpl implements DeliveryChallanService {

    private static final String MODULE = "DELIVERY_CHALLAN";

    private static final Map<DcStatus, Set<DcStatus>> TRANSITIONS = new EnumMap<>(DcStatus.class);

    static {
        TRANSITIONS.put(DcStatus.APPROVED, Set.of(DcStatus.ISSUED));
        TRANSITIONS.put(DcStatus.ISSUED, Set.of(DcStatus.DELIVERED));
    }

    private final DeliveryChallanRepository deliveryChallanRepository;
    private final UnitRepository unitRepository;
    private final PartyRepository partyRepository;
    private final CourierWayRepository courierWayRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final DcNumberGenerator dcNumberGenerator;
    private final DcMapper dcMapper;
    private final DcPdfService dcPdfService;
    private final AuditLogService auditLogService;
    private final ApprovalAuthorizationService approvalAuthorizationService;

    public DeliveryChallanServiceImpl(DeliveryChallanRepository deliveryChallanRepository,
                                      UnitRepository unitRepository,
                                      PartyRepository partyRepository,
                                      CourierWayRepository courierWayRepository,
                                      PackageTypeRepository packageTypeRepository,
                                      CompanySettingsRepository companySettingsRepository,
                                      DcNumberGenerator dcNumberGenerator,
                                      DcMapper dcMapper,
                                      DcPdfService dcPdfService,
                                      AuditLogService auditLogService,
                                      ApprovalAuthorizationService approvalAuthorizationService) {
        this.deliveryChallanRepository = deliveryChallanRepository;
        this.unitRepository = unitRepository;
        this.partyRepository = partyRepository;
        this.courierWayRepository = courierWayRepository;
        this.packageTypeRepository = packageTypeRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.dcNumberGenerator = dcNumberGenerator;
        this.dcMapper = dcMapper;
        this.dcPdfService = dcPdfService;
        this.auditLogService = auditLogService;
        this.approvalAuthorizationService = approvalAuthorizationService;
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
        DeliveryChallan dc = new DeliveryChallan();
        applyFields(dc, request);
        dc.setStatus(DcStatus.DRAFT);
        dc.setCurrentApprovalLevel(1);

        String companyCode = resolveCompanyCode();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        dc.setDcDate(today);
        dc.setDcNumber(dcNumberGenerator.next(today, companyCode));

        DeliveryChallan saved = deliveryChallanRepository.save(dc);
        log.info("Delivery challan created: id={}, dcNumber={}", saved.getId(), saved.getDcNumber());
        auditLogService.log("DELIVERY_CHALLAN", "CREATE", saved.getId(), saved.getDcNumber(),
                currentUsername(), "Unit=" + saved.getUnit().getUnitName());
        return dcMapper.toResponse(saved);
    }

    @Override
    public DcResponse update(Long id, DcRequest request) {
        DeliveryChallan dc = findDc(id);
        if (dc.getStatus() != DcStatus.DRAFT && dc.getStatus() != DcStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only delivery challans in DRAFT or PENDING_APPROVAL state can be edited");
        }
        applyFields(dc, request);
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
    public DcResponse changeStatus(Long id, DcStatus target) {
        DeliveryChallan dc = findDc(id);
        DcStatus current = dc.getStatus();
        Set<DcStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException("Cannot move delivery challan from " + current + " to " + target);
        }
        dc.setStatus(target);
        DeliveryChallan saved = deliveryChallanRepository.save(dc);
        auditLogService.log("DELIVERY_CHALLAN", "STATUS_CHANGE", saved.getId(), saved.getDcNumber(),
                currentUsername(), "status=" + target);
        return dcMapper.toResponse(saved);
    }

    @Override
    public DcResponse submitForApproval(Long id) {
        DeliveryChallan dc = findDc(id);
        if (dc.getStatus() != DcStatus.DRAFT) {
            throw new BusinessException("Only DRAFT delivery challans can be submitted for approval");
        }
        dc.setStatus(DcStatus.PENDING_APPROVAL);
        dc.setCurrentApprovalLevel(1);
        DeliveryChallan saved = deliveryChallanRepository.save(dc);
        auditLogService.log("DELIVERY_CHALLAN", "SUBMIT", saved.getId(), saved.getDcNumber(),
                currentUsername(), "Submitted for approval — awaiting Level 1");
        return dcMapper.toResponse(saved);
    }

    @Override
    public DcResponse approve(Long id, ApprovalDecisionRequest request, String approverUsername) {
        DeliveryChallan dc = requirePendingApproval(id, approverUsername);
        String creator = dc.getCreatedBy();
        int currentLevel = dc.getCurrentApprovalLevel();
        int maxLevel = approvalAuthorizationService.getMaxLevel(creator, MODULE);

        dc.setApproverUsername(approverUsername);
        dc.setApprovalTimestamp(Instant.now());
        dc.setApprovalRemarks(request != null ? request.remarks() : null);

        if (currentLevel < maxLevel) {
            int nextLevel = currentLevel + 1;
            dc.setCurrentApprovalLevel(nextLevel);
            DeliveryChallan saved = deliveryChallanRepository.save(dc);
            auditLogService.log("DELIVERY_CHALLAN", "APPROVE", saved.getId(), saved.getDcNumber(),
                    approverUsername, "Level " + currentLevel + " approved — escalated to Level " + nextLevel);
            return dcMapper.toResponse(saved);
        } else {
            dc.setStatus(DcStatus.APPROVED);
            DeliveryChallan saved = deliveryChallanRepository.save(dc);
            auditLogService.log("DELIVERY_CHALLAN", "APPROVE", saved.getId(), saved.getDcNumber(),
                    approverUsername, "Final approval (Level " + currentLevel + "). Remarks: "
                            + (request != null ? request.remarks() : ""));
            return dcMapper.toResponse(saved);
        }
    }

    @Override
    public DcResponse reject(Long id, ApprovalDecisionRequest request, String approverUsername) {
        DeliveryChallan dc = requirePendingApproval(id, approverUsername);
        dc.setStatus(DcStatus.REJECTED);
        dc.setApproverUsername(approverUsername);
        dc.setApprovalTimestamp(Instant.now());
        dc.setApprovalRemarks(request != null ? request.remarks() : null);
        DeliveryChallan saved = deliveryChallanRepository.save(dc);
        auditLogService.log("DELIVERY_CHALLAN", "REJECT", saved.getId(), saved.getDcNumber(),
                approverUsername, "Remarks: " + (request != null ? request.remarks() : ""));
        return dcMapper.toResponse(saved);
    }

    private DeliveryChallan requirePendingApproval(Long id, String approverUsername) {
        DeliveryChallan dc = findDc(id);
        if (dc.getStatus() != DcStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only delivery challans in PENDING_APPROVAL state can be approved or rejected");
        }
        String creator = dc.getCreatedBy();
        int level = dc.getCurrentApprovalLevel();
        if (!approvalAuthorizationService.isAuthorizedApproverAtLevel(approverUsername, creator, MODULE, level)) {
            throw new BusinessException(
                    "You are not a designated approver for this delivery challan at Level " + level,
                    HttpStatus.FORBIDDEN);
        }
        return dc;
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

    private void applyFields(DeliveryChallan dc, DcRequest request) {
        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit", request.unitId()));

        Party receiverParty = null;
        Unit receiverUnit = null;
        if (request.receiverType() == ReceiverType.PARTY) {
            if (request.receiverPartyId() == null) {
                throw new BusinessException("Receiver party is required when receiver type is PARTY");
            }
            receiverParty = partyRepository.findById(request.receiverPartyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receiver party", request.receiverPartyId()));
        } else {
            if (request.receiverUnitId() == null) {
                throw new BusinessException("Receiver unit is required when receiver type is UNIT");
            }
            if (request.receiverUnitId().equals(request.unitId())) {
                throw new BusinessException("Receiver unit cannot be the same as the sending unit");
            }
            receiverUnit = unitRepository.findById(request.receiverUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receiver unit", request.receiverUnitId()));
        }

        CourierWay courierWay = courierWayRepository.findById(request.courierWayId())
                .orElseThrow(() -> new ResourceNotFoundException("Courier way", request.courierWayId()));
        if (!courierWay.isActive()) {
            throw new BusinessException("Courier way '" + courierWay.getName() + "' is not active");
        }
        PackageType packageType = null;
        if (request.packageTypeId() != null) {
            packageType = packageTypeRepository.findById(request.packageTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Package type", request.packageTypeId()));
        }

        dc.setUnit(unit);
        dc.setDcType(request.dcType());
        dc.setReceiverType(request.receiverType());
        dc.setReceiverParty(receiverParty);
        dc.setReceiverUnit(receiverUnit);
        dc.setCourierWay(courierWay);
        dc.setPackageType(packageType);
        dc.setItemDescription(request.itemDescription());
        dc.setWeightKg(request.weightKg());
        dc.setNoOfPackages(request.noOfPackages());
        dc.setCourierMode(request.courierMode());
        dc.setVehicleNumber(request.vehicleNumber());
        dc.setDriverName(request.driverName());
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
