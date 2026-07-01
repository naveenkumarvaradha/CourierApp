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
import com.courierapp.repository.UserRepository;
import com.courierapp.service.ApprovalAuthorizationService;
import com.courierapp.service.AuditLogService;
import com.courierapp.service.BookingNumberGenerator;
import com.courierapp.service.BookingService;
import com.courierapp.service.StickerPdfService;
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
@Service
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
                              AuditLogService auditLogService) {
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
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> search(String bookingNumber, LocalDate fromDate, LocalDate toDate,
                                                BookingStatus status, Long senderId, Long receiverId,
                                                CourierMode mode, Pageable pageable) {
        Specification<Booking> spec = buildSpec(bookingNumber, fromDate, toDate, status,
                senderId, receiverId, mode);
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
        booking.setBookingNumber(bookingNumberGenerator.next(today));
        booking.setStatus(BookingStatus.BOOKED);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "CREATE", saved.getId(), saved.getBookingNumber(),
                saved.getCreatedBy(), "Receiver=" + saved.getReceiver().getPartyName()
                        + ", mode=" + saved.getCourierMode());
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
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "SUBMIT", saved.getId(), saved.getBookingNumber(),
                saved.getUpdatedBy(), "Submitted for approval");
        return bookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse approve(Long id, ApprovalDecisionRequest request, String approverUsername) {
        Booking booking = requirePendingApproval(id, approverUsername);
        booking.setStatus(BookingStatus.APPROVED);
        booking.setApproverUsername(approverUsername);
        booking.setApprovalTimestamp(Instant.now());
        booking.setApprovalRemarks(request != null ? request.remarks() : null);
        Booking saved = bookingRepository.save(booking);
        auditLogService.log("BOOKING", "APPROVE", saved.getId(), saved.getBookingNumber(),
                approverUsername, "Remarks: " + (request != null ? request.remarks() : ""));
        return bookingMapper.toResponse(saved);
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
        CompanySettings settings = companySettingsRepository.findAll().stream().findFirst().orElse(null);

        byte[] pdf = stickerPdfService.generate(booking, creator, settings);

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
        if (booking.getAwbNumber() != null && !booking.getAwbNumber().isBlank() && booking.isPrintTaken()) {
            throw new BusinessException("Cannot cancel: AWB number assigned and sticker already printed");
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
        if (!approvalAuthorizationService.isAuthorizedApprover(approverUsername, creator)) {
            throw new BusinessException(
                    "You are not a designated approver for this booking",
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }
        return booking;
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    private void apply(Booking booking, BookingRequest r, LocalDate bookingDate) {
        Party sender = resolveCompanySender();
        Party receiver = partyRepository.findById(r.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver party", r.receiverId()));
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
        booking.setBookingDate(bookingDate);
        booking.setSender(sender);
        booking.setReceiver(receiver);
        booking.setCourierWay(courierWay);
        booking.setPackageType(packageType);
        booking.setItemDescription(r.itemDescription());
        booking.setWeightKg(r.weightKg());
        booking.setNoOfPackages(r.noOfPackages());
        booking.setCourierMode(r.courierMode());
        booking.setSpecialInstructions(r.specialInstructions());
        booking.setCompanyPoNo(r.companyPoNo());
    }

    private Party resolveCompanySender() {
        CompanySettings settings = companySettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BusinessException("Company settings not configured. Please set up company details in Admin → Company Setup."));
        if (settings.getLinkedParty() == null) {
            throw new BusinessException("Company party not linked. Please save Company Setup to generate the sender party.");
        }
        return settings.getLinkedParty();
    }

    private Specification<Booking> buildSpec(String bookingNumber, LocalDate fromDate, LocalDate toDate,
                                             BookingStatus status, Long senderId, Long receiverId,
                                             CourierMode mode) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
