package com.courierapp.service.impl;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.booking.ApprovalDecisionRequest;
import com.courierapp.dto.booking.AwbUpdateRequest;
import com.courierapp.dto.booking.BookingRequest;
import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.dto.booking.StatusUpdateRequest;
import com.courierapp.entity.Booking;
import com.courierapp.entity.CompanySettings;
import com.courierapp.entity.CourierWay;
import com.courierapp.entity.PackageType;
import com.courierapp.entity.Party;
import com.courierapp.entity.Unit;
import com.courierapp.entity.User;
import com.courierapp.enums.BookingStatus;
import com.courierapp.enums.CourierMode;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.BookingMapper;
import com.courierapp.repository.BookingRepository;
import com.courierapp.repository.CompanySettingsRepository;
import com.courierapp.repository.CourierWayRepository;
import com.courierapp.repository.PackageTypeRepository;
import com.courierapp.repository.PartyRepository;
import com.courierapp.repository.UnitRepository;
import com.courierapp.repository.UserRepository;
import com.courierapp.service.ApprovalAuthorizationService;
import com.courierapp.service.AuditLogService;
import com.courierapp.service.BookingNumberGenerator;
import com.courierapp.service.BookingService;
import com.courierapp.service.StickerPdfService;
import com.courierapp.kafka.event.BookingEvent;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import lombok.extern.slf4j.Slf4j;
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
@Service("bookingService")
@Transactional
public class BookingServiceImpl implements BookingService {

    private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS = new EnumMap<>(BookingStatus.class);

    static {
        TRANSITIONS.put(BookingStatus.APPROVED, Set.of(BookingStatus.IN_TRANSIT, BookingStatus.CANCELLED));
        TRANSITIONS.put(BookingStatus.IN_TRANSIT, Set.of(BookingStatus.DELIVERED, BookingStatus.CANCELLED));
    }

    private final BookingRepository bookingRepository;
    private final PartyRepository partyRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final CourierWayRepository courierWayRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final UserRepository userRepository;
    private final BookingNumberGenerator bookingNumberGenerator;
    private final BookingMapper bookingMapper;
    private final StickerPdfService stickerPdfService;
    private final ApprovalAuthorizationService approvalAuthorizationService;
    private final AuditLogService auditLogService;
    private final com.courierapp.repository.StickerFieldConfigRepository stickerFieldConfigRepository;
    private final com.courierapp.kafka.CourierEventProducer eventProducer;
    private final UnitRepository unitRepository;
    private final com.courierapp.security.CurrentUserService currentUserService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              PartyRepository partyRepository,
                              CompanySettingsRepository companySettingsRepository,
                              CourierWayRepository courierWayRepository,
                              PackageTypeRepository packageTypeRepository,
                              UserRepository userRepository,
                              BookingNumberGenerator bookingNumberGenerator,
                              BookingMapper bookingMapper,
                              StickerPdfService stickerPdfService,
                              ApprovalAuthorizationService approvalAuthorizationService,
                              AuditLogService auditLogService,
                              com.courierapp.repository.StickerFieldConfigRepository stickerFieldConfigRepository,
                              com.courierapp.kafka.CourierEventProducer eventProducer,
                              UnitRepository unitRepository,
                              com.courierapp.security.CurrentUserService currentUserService) {
        this.bookingRepository = bookingRepository;
        this.partyRepository = partyRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.courierWayRepository = courierWayRepository;
        this.packageTypeRepository = packageTypeRepository;
        this.userRepository = userRepository;
        this.bookingNumberGenerator = bookingNumberGenerator;
        this.bookingMapper = bookingMapper;
        this.stickerPdfService = stickerPdfService;
        this.approvalAuthorizationService = approvalAuthorizationService;
        this.auditLogService = auditLogService;
        this.stickerFieldConfigRepository = stickerFieldConfigRepository;
        this.unitRepository = unitRepository;
        this.eventProducer = eventProducer;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> search(String bookingNumber, LocalDate fromDate, LocalDate toDate,
                                                BookingStatus status, Long senderId, Long receiverId,
                                                CourierMode mode, String receiverName, String receiverCompanyName,
                                                Pageable pageable) {
        Specification<Booking> spec = buildSpec(bookingNumber, fromDate, toDate, status,
                senderId, receiverId, mode, receiverName, receiverCompanyName);
        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        return PageResponse.from(page, bookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse get(Long id) {
        return bookingMapper.toResponse(findBooking(id));
    }

    @Override
    public BookingResponse create(BookingRequest request) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Booking booking = new Booking();
        apply(booking, request, today);
        String companyCode = resolveCompanyCode();
        booking.setBookingNumber(bookingNumberGenerator.next(today, companyCode));
        booking.setStatus(BookingStatus.BOOKED);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "CREATE", saved.getId(), saved.getBookingNumber(),
                saved.getCreatedBy(), "Receiver=" + saved.getReceiver().getPartyName()
                        + ", mode=" + saved.getCourierMode());
        eventProducer.publishBookingEvent(
                BookingEvent.created(saved.getId(), saved.getBookingNumber(), saved.getCreatedBy(), companyCode));
        return bookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse update(Long id, BookingRequest request) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.BOOKED
                && booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only bookings in BOOKED or PENDING_APPROVAL state can be edited");
        }
        apply(booking, request, booking.getBookingDate());
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "UPDATE", saved.getId(), saved.getBookingNumber(),
                saved.getUpdatedBy(), "Updated booking details");
        return bookingMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessException("Only bookings in BOOKED state can be deleted");
        }
        String number = booking.getBookingNumber();
        String creator = booking.getCreatedBy();
        bookingRepository.delete(booking);
        auditLogService.log("BOOKING", "DELETE", id, number, creator, null);
    }

    @Override
    public BookingResponse submitForApproval(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessException("Only BOOKED bookings can be submitted for approval");
        }
        booking.setStatus(BookingStatus.PENDING_APPROVAL);
        booking.setCurrentApprovalLevel(1);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "SUBMIT", saved.getId(), saved.getBookingNumber(),
                saved.getUpdatedBy(), "Submitted for approval — awaiting Level 1");
        eventProducer.publishBookingEvent(
                BookingEvent.submitted(saved.getId(), saved.getBookingNumber(), saved.getCreatedBy()));
        return bookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse approve(Long id, ApprovalDecisionRequest request, String approverUsername) {
        Booking booking = requirePendingApproval(id, approverUsername);
        String creator = booking.getCreatedBy();
        int currentLevel = booking.getCurrentApprovalLevel();
        int maxLevel = approvalAuthorizationService.getMaxLevel(creator, "BOOKING");

        booking.setApproverUsername(approverUsername);
        booking.setApprovalTimestamp(Instant.now());
        booking.setApprovalRemarks(request != null ? request.remarks() : null);

        if (currentLevel < maxLevel) {
            // Escalate to next level
            int nextLevel = currentLevel + 1;
            booking.setCurrentApprovalLevel(nextLevel);
            Booking saved = bookingRepository.save(booking);
            auditLogService.log("BOOKING", "APPROVE", saved.getId(), saved.getBookingNumber(),
                    approverUsername, "Level " + currentLevel + " approved — escalated to Level " + nextLevel);
            return bookingMapper.toResponse(saved);
        } else {
            // Final level approved
            booking.setStatus(BookingStatus.APPROVED);
            Booking saved = bookingRepository.save(booking);
            auditLogService.log("BOOKING", "APPROVE", saved.getId(), saved.getBookingNumber(),
                    approverUsername, "Final approval (Level " + currentLevel + "). Remarks: "
                            + (request != null ? request.remarks() : ""));
            eventProducer.publishBookingEvent(BookingEvent.approved(saved.getId(), saved.getBookingNumber(),
                    creator, approverUsername, request != null ? request.remarks() : null));
            return bookingMapper.toResponse(saved);
        }
    }

    @Override
    public BookingResponse reject(Long id, ApprovalDecisionRequest request, String approverUsername) {
        Booking booking = requirePendingApproval(id, approverUsername);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setApproverUsername(approverUsername);
        booking.setApprovalTimestamp(Instant.now());
        booking.setApprovalRemarks(request != null ? request.remarks() : null);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "REJECT", saved.getId(), saved.getBookingNumber(),
                approverUsername, "Remarks: " + (request != null ? request.remarks() : ""));
        eventProducer.publishBookingEvent(BookingEvent.rejected(saved.getId(), saved.getBookingNumber(),
                booking.getCreatedBy(), approverUsername, request != null ? request.remarks() : null));
        return bookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse changeStatus(Long id, StatusUpdateRequest request) {
        Booking booking = findBooking(id);
        BookingStatus prev = booking.getStatus();
        BookingStatus target = request.status();
        Set<BookingStatus> allowed = TRANSITIONS.getOrDefault(prev, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException("Cannot transition from " + prev + " to " + target);
        }
        booking.setStatus(target);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "STATUS_CHANGE", saved.getId(), saved.getBookingNumber(),
                saved.getUpdatedBy(), prev + " → " + target);
        return bookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse updateAwb(Long id, AwbUpdateRequest request) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.APPROVED
                && booking.getStatus() != BookingStatus.IN_TRANSIT
                && booking.getStatus() != BookingStatus.DELIVERED) {
            throw new BusinessException("AWB number can only be set after a booking is APPROVED");
        }
        String awb = request.awbNumber().trim();
        bookingRepository.findByAwbNumber(awb).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("AWB number '" + awb + "' is already assigned to booking "
                        + existing.getBookingNumber());
            }
        });
        booking.setAwbNumber(awb);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "AWB_UPDATE", saved.getId(), saved.getBookingNumber(),
                saved.getUpdatedBy(), "AWB=" + awb);
        return bookingMapper.toResponse(saved);
    }

    @Override
    public byte[] generateStickerPdf(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.APPROVED
                && booking.getStatus() != BookingStatus.IN_TRANSIT
                && booking.getStatus() != BookingStatus.DELIVERED) {
            throw new BusinessException("Sticker can only be printed for APPROVED bookings");
        }
        if (booking.getAwbNumber() == null || booking.getAwbNumber().isBlank()) {
            throw new BusinessException("AWB number must be set before printing the sticker");
        }

        // Look up creator's user details for FROM block
        User creator = userRepository.findByUsername(booking.getCreatedBy()).orElse(null);

        // Load company settings by creator's company so logo data is correct
        Long companyId = (creator != null && creator.getCompany() != null)
                ? creator.getCompany().getId() : null;
        CompanySettings settings = (companyId != null)
                ? companySettingsRepository.findByCompanyId(companyId).orElse(null)
                : companySettingsRepository.findAll().stream().findFirst().orElse(null);
        if (companyId == null && settings != null && settings.getCompany() != null) {
            companyId = settings.getCompany().getId();
        }

        java.util.List<com.courierapp.dto.admin.StickerFieldDto> fieldConfig = null;
        if (companyId != null) {
            var saved = stickerFieldConfigRepository.findByCompanyIdOrderBySortOrder(companyId);
            if (!saved.isEmpty()) {
                fieldConfig = saved.stream().map(s -> new com.courierapp.dto.admin.StickerFieldDto(
                        s.getFieldKey(), s.getLabel(), s.isVisible(), s.getSortOrder(),
                        s.getSection() != null ? s.getSection() : "BOTTOM")).toList();
            }
        }

        byte[] pdf = stickerPdfService.generate(booking, creator, settings, booking.getUnit(), fieldConfig);

        // Mark print as taken
        if (!booking.isPrintTaken()) {
            booking.setPrintTaken(true);
            bookingRepository.save(booking);
        }
        auditLogService.log("BOOKING", "PRINT", booking.getId(), booking.getBookingNumber(),
                booking.getUpdatedBy(), "Sticker printed, AWB=" + booking.getAwbNumber());
        return pdf;
    }

    @Override
    public BookingResponse revise(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BusinessException("Only APPROVED bookings can be revised");
        }
        if (booking.getAwbNumber() != null && !booking.getAwbNumber().isBlank()) {
            throw new BusinessException("Cannot revise: AWB number has already been assigned");
        }
        if (booking.isPrintTaken()) {
            throw new BusinessException("Cannot revise: sticker has already been printed");
        }
        booking.setStatus(BookingStatus.BOOKED);
        booking.setApproverUsername(null);
        booking.setApprovalTimestamp(null);
        booking.setApprovalRemarks(null);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "REVISE", saved.getId(), saved.getBookingNumber(),
                saved.getUpdatedBy(), "Booking reset to BOOKED for revision");
        return bookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse requestCancellation(Long id, String remarks) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BusinessException("Only APPROVED bookings can be requested for cancellation");
        }
        if (booking.getAwbNumber() != null && !booking.getAwbNumber().isBlank()) {
            throw new BusinessException("Cannot cancel: AWB number has already been assigned to this booking");
        }
        booking.setStatus(BookingStatus.PENDING_CANCELLATION);
        booking.setCancellationRemarks(remarks);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "CANCEL_REQUEST", saved.getId(), saved.getBookingNumber(),
                saved.getUpdatedBy(), "Cancellation requested. Remarks: " + remarks);
        return bookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse approveCancellation(Long id, String approverUsername) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.PENDING_CANCELLATION) {
            throw new BusinessException("Only PENDING_CANCELLATION bookings can have cancellation approved");
        }
        String creator = booking.getCreatedBy();
        if (!approvalAuthorizationService.isAuthorizedApprover(approverUsername, creator)) {
            throw new BusinessException("You are not a designated approver for this booking",
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "CANCEL_APPROVE", saved.getId(), saved.getBookingNumber(),
                approverUsername, "Cancellation approved");
        return bookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse rejectCancellation(Long id, String approverUsername) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.PENDING_CANCELLATION) {
            throw new BusinessException("Only PENDING_CANCELLATION bookings can have cancellation rejected");
        }
        String creator = booking.getCreatedBy();
        if (!approvalAuthorizationService.isAuthorizedApprover(approverUsername, creator)) {
            throw new BusinessException("You are not a designated approver for this booking",
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }
        booking.setStatus(BookingStatus.APPROVED);
        booking.setCancellationRemarks(null);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "CANCEL_REJECT", saved.getId(), saved.getBookingNumber(),
                approverUsername, "Cancellation rejected, booking restored to APPROVED");
        return bookingMapper.toResponse(saved);
    }

    private Booking requirePendingApproval(Long id, String approverUsername) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only bookings in PENDING_APPROVAL state can be approved or rejected");
        }
        String creator = booking.getCreatedBy();
        int level = booking.getCurrentApprovalLevel();
        if (!approvalAuthorizationService.isAuthorizedApproverAtLevel(approverUsername, creator, "BOOKING", level)) {
            throw new BusinessException(
                    "You are not a designated approver for this booking at Level " + level,
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }
        return booking;
    }

    private Booking findBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        Long callerCompanyId = currentUserService.requireCompanyId();
        Long ownerCompanyId = booking.getSender().getCompany() != null
                ? booking.getSender().getCompany().getId() : null;
        if (ownerCompanyId == null || !ownerCompanyId.equals(callerCompanyId)) {
            // 404, not 403 — don't confirm to a caller from another company that this id exists.
            throw new ResourceNotFoundException("Booking", id);
        }
        return booking;
    }

    private void apply(Booking booking, BookingRequest r, LocalDate bookingDate) {
        Long callerCompanyId = currentUserService.requireCompanyId();
        Party sender = resolveCompanySender();
        Party receiver = partyRepository.findById(r.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver party", r.receiverId()));
        // A party with no company is a shared/global address-book entry; one with a company
        // must be the caller's own — otherwise this would let a booking pull in another
        // company's private address-book record just by guessing its id.
        if (receiver.getCompany() != null && !receiver.getCompany().getId().equals(callerCompanyId)) {
            throw new ResourceNotFoundException("Receiver party", r.receiverId());
        }
        CourierWay courierWay = courierWayRepository.findById(r.courierWayId())
                .orElseThrow(() -> new ResourceNotFoundException("Courier way", r.courierWayId()));
        if (!courierWay.isActive()) {
            throw new BusinessException("Courier way '" + courierWay.getName() + "' is not active");
        }
        PackageType packageType = null;
        if (r.packageTypeId() != null) {
            packageType = packageTypeRepository.findById(r.packageTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Package type", r.packageTypeId()));
        }
        Unit unit = null;
        if (r.unitId() != null) {
            unit = unitRepository.findById(r.unitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit", r.unitId()));
            if (unit.getCompany() == null || !unit.getCompany().getId().equals(callerCompanyId)) {
                throw new ResourceNotFoundException("Unit", r.unitId());
            }
        }
        booking.setBookingDate(bookingDate);
        booking.setSender(sender);
        booking.setReceiver(receiver);
        booking.setCourierWay(courierWay);
        booking.setPackageType(packageType);
        booking.setUnit(unit);
        booking.setItemDescription(r.itemDescription());
        booking.setWeightKg(r.weightKg());
        booking.setNoOfPackages(r.noOfPackages());
        booking.setCourierMode(r.courierMode());
        booking.setSpecialInstructions(r.specialInstructions());
        booking.setCompanyPoNo(r.companyPoNo());
    }

    private Party resolveCompanySender() {
        Long companyId = currentUserService.requireCompanyId();
        CompanySettings settings = companySettingsRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new BusinessException("Company settings not configured. Please set up company details in Admin → Company Setup."));
        if (settings.getLinkedParty() == null) {
            throw new BusinessException("Company party not linked. Please save Company Setup to generate the sender party.");
        }
        return settings.getLinkedParty();
    }

    private String resolveCompanyCode() {
        Long companyId = currentUserService.requireCompanyId();
        return companySettingsRepository.findByCompanyId(companyId)
                .map(s -> s.getCompany() != null ? s.getCompany().getCompanyCode() : null)
                .orElse(null);
    }

    private Specification<Booking> buildSpec(String bookingNumber, LocalDate fromDate, LocalDate toDate,
                                             BookingStatus status, Long senderId, Long receiverId,
                                             CourierMode mode, String receiverName, String receiverCompanyName) {
        Long callerCompanyId = currentUserService.requireCompanyId();
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("sender").get("company").get("id"), callerCompanyId));
            if (StringUtils.hasText(bookingNumber)) {
                predicates.add(cb.like(cb.lower(root.get("bookingNumber")),
                        "%" + bookingNumber.toLowerCase() + "%"));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bookingDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bookingDate"), toDate));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (senderId != null) {
                predicates.add(cb.equal(root.get("sender").get("id"), senderId));
            }
            if (receiverId != null) {
                predicates.add(cb.equal(root.get("receiver").get("id"), receiverId));
            }
            if (mode != null) {
                predicates.add(cb.equal(root.get("courierMode"), mode));
            }
            if (StringUtils.hasText(receiverName)) {
                predicates.add(cb.like(cb.lower(root.get("receiver").get("partyName")),
                        "%" + receiverName.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(receiverCompanyName)) {
                predicates.add(cb.like(cb.lower(root.get("receiver").get("companyName")),
                        "%" + receiverCompanyName.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public boolean isCreatorOf(Long id, String username) {
        return bookingRepository.findById(id)
                .map(b -> username != null && username.equals(b.getCreatedBy()))
                .orElse(false);
    }
}
