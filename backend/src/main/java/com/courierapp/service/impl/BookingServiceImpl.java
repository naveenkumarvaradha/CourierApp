package com.courierapp.service.impl;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.booking.ApprovalDecisionRequest;
import com.courierapp.dto.booking.BookingRequest;
import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.dto.booking.StatusUpdateRequest;
import com.courierapp.entity.Booking;
import com.courierapp.entity.Party;
import com.courierapp.enums.BookingStatus;
import com.courierapp.enums.CourierMode;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.BookingMapper;
import com.courierapp.repository.BookingRepository;
import com.courierapp.repository.PartyRepository;
import com.courierapp.service.ApprovalAuthorizationService;
import com.courierapp.service.BookingNumberGenerator;
import com.courierapp.service.BookingService;
import com.courierapp.service.StickerPdfService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final BookingNumberGenerator bookingNumberGenerator;
    private final BookingMapper bookingMapper;
    private final StickerPdfService stickerPdfService;
    private final ApprovalAuthorizationService approvalAuthorizationService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              PartyRepository partyRepository,
                              BookingNumberGenerator bookingNumberGenerator,
                              BookingMapper bookingMapper,
                              StickerPdfService stickerPdfService,
                              ApprovalAuthorizationService approvalAuthorizationService) {
        this.bookingRepository = bookingRepository;
        this.partyRepository = partyRepository;
        this.bookingNumberGenerator = bookingNumberGenerator;
        this.bookingMapper = bookingMapper;
        this.stickerPdfService = stickerPdfService;
        this.approvalAuthorizationService = approvalAuthorizationService;
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
        Booking booking = new Booking();
        apply(booking, request);
        booking.setBookingNumber(bookingNumberGenerator.next(request.bookingDate()));
        booking.setStatus(BookingStatus.BOOKED);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public BookingResponse update(Long id, BookingRequest request) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.BOOKED
                && booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only bookings in BOOKED or PENDING_APPROVAL state can be edited");
        }
        apply(booking, request);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public void delete(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessException("Only bookings in BOOKED state can be deleted");
        }
        bookingRepository.delete(booking);
    }

    @Override
    public BookingResponse submitForApproval(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessException("Only BOOKED bookings can be submitted for approval");
        }
        booking.setStatus(BookingStatus.PENDING_APPROVAL);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public BookingResponse approve(Long id, ApprovalDecisionRequest request, String approverUsername) {
        Booking booking = requirePendingApproval(id, approverUsername);
        booking.setStatus(BookingStatus.APPROVED);
        booking.setApproverUsername(approverUsername);
        booking.setApprovalTimestamp(Instant.now());
        booking.setApprovalRemarks(request != null ? request.remarks() : null);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public BookingResponse reject(Long id, ApprovalDecisionRequest request, String approverUsername) {
        Booking booking = requirePendingApproval(id, approverUsername);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setApproverUsername(approverUsername);
        booking.setApprovalTimestamp(Instant.now());
        booking.setApprovalRemarks(request != null ? request.remarks() : null);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public BookingResponse changeStatus(Long id, StatusUpdateRequest request) {
        Booking booking = findBooking(id);
        BookingStatus target = request.status();
        Set<BookingStatus> allowed = TRANSITIONS.getOrDefault(booking.getStatus(), Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException("Cannot transition from " + booking.getStatus()
                    + " to " + target);
        }
        booking.setStatus(target);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStickerPdf(Long id) {
        return stickerPdfService.generate(findBooking(id));
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

    private void apply(Booking booking, BookingRequest r) {
        Party sender = partyRepository.findById(r.senderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender party", r.senderId()));
        Party receiver = partyRepository.findById(r.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver party", r.receiverId()));
        booking.setBookingDate(r.bookingDate());
        booking.setSender(sender);
        booking.setReceiver(receiver);
        booking.setItemDescription(r.itemDescription());
        booking.setWeightKg(r.weightKg());
        booking.setNoOfPackages(r.noOfPackages());
        booking.setCourierMode(r.courierMode());
        booking.setDeclaredValue(r.declaredValue());
        booking.setFreightCharges(r.freightCharges());
        booking.setTotalCharges(r.totalCharges());
        booking.setPaymentMode(r.paymentMode());
        booking.setSpecialInstructions(r.specialInstructions());
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
