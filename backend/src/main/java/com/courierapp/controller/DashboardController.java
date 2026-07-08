package com.courierapp.controller;

import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.dto.dashboard.DashboardResponse;
import com.courierapp.dto.master.PartyResponse;
import com.courierapp.entity.Booking;
import com.courierapp.entity.Party;
import com.courierapp.entity.User;
import com.courierapp.enums.BookingStatus;
import com.courierapp.enums.PartyStatus;
import com.courierapp.mapper.BookingMapper;
import com.courierapp.mapper.PartyMapper;
import com.courierapp.repository.BookingRepository;
import com.courierapp.repository.PartyRepository;
import com.courierapp.repository.UserRepository;
import com.courierapp.service.ApprovalAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final BookingRepository bookingRepository;
    private final PartyRepository partyRepository;
    private final BookingMapper bookingMapper;
    private final PartyMapper partyMapper;
    private final ApprovalAuthorizationService approvalAuthorizationService;
    private final UserRepository userRepository;

    public DashboardController(BookingRepository bookingRepository,
                               PartyRepository partyRepository,
                               BookingMapper bookingMapper,
                               PartyMapper partyMapper,
                               ApprovalAuthorizationService approvalAuthorizationService,
                               UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.partyRepository = partyRepository;
        this.bookingMapper = bookingMapper;
        this.partyMapper = partyMapper;
        this.approvalAuthorizationService = approvalAuthorizationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Dashboard: my pending approvals + what I sent for approval")
    public DashboardResponse get(Authentication auth) {
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ADMIN_VIEW".equals(a.getAuthority()));
        boolean canApproveBookings = isAdmin || auth.getAuthorities().stream()
                .anyMatch(a -> "BOOKING_APPROVE".equals(a.getAuthority()));
        boolean canApproveParties = isAdmin || auth.getAuthorities().stream()
                .anyMatch(a -> "MASTER_APPROVE".equals(a.getAuthority()));

        // Resolve company of current user (used in multiple sections below)
        User currentUser = userRepository.findByUsername(username).orElse(null);
        Long companyId = (currentUser != null && currentUser.getCompany() != null)
                ? currentUser.getCompany().getId() : null;

        // ── Bookings pending approval ─────────────────────────────────────────
        // Only show bookings actually routed to this user (no admin bypass — admin sees only
        // what is specifically assigned to them via routing rules).
        List<BookingResponse> bookingsPendingMyApproval = List.of();
        if (canApproveBookings) {
            Specification<Booking> spec = (root, q, cb) -> cb.and(
                    cb.equal(root.get("status"), BookingStatus.PENDING_APPROVAL),
                    cb.notEqual(root.get("createdBy"), username)
            );
            bookingsPendingMyApproval = bookingRepository.findAll(spec).stream()
                    .filter(b -> approvalAuthorizationService.isDesignatedApproverAtLevel(
                            username, b.getCreatedBy(), "BOOKING", b.getCurrentApprovalLevel()))
                    .map(bookingMapper::toResponse)
                    .toList();
        }

        // ── Bookings I sent (created) that are pending approval ───────────────
        Specification<Booking> mySentSpec = (root, q, cb) -> cb.and(
                cb.equal(root.get("status"), BookingStatus.PENDING_APPROVAL),
                cb.equal(root.get("createdBy"), username)
        );
        List<BookingResponse> myBookingsPendingSent = bookingRepository.findAll(mySentSpec)
                .stream().map(b -> {
                    BookingResponse base = bookingMapper.toResponse(b);
                    List<String> approvers = approvalAuthorizationService
                            .resolveApproversAtLevel(b.getCreatedBy(), "BOOKING", b.getCurrentApprovalLevel());
                    return new BookingResponse(base.id(), base.bookingNumber(), base.bookingDate(),
                            base.sender(), base.receiver(), base.courierWay(), base.packageType(),
                            base.itemDescription(), base.weightKg(), base.noOfPackages(),
                            base.courierMode(), base.specialInstructions(), base.status(),
                            base.awbNumber(), base.approverUsername(), base.approvalTimestamp(),
                            base.approvalRemarks(), base.companyPoNo(), base.printTaken(),
                            base.cancellationRemarks(), base.currentApprovalLevel(),
                            base.createdAt(), base.createdBy(), base.updatedAt(), base.updatedBy(),
                            approvers);
                }).toList();

        // ── Parties pending approval ──────────────────────────────────────────
        // Admins see all company PENDING_APPROVAL parties; others see only what's routed to them.
        List<PartyResponse> partiesPendingMyApproval = List.of();
        if (canApproveParties) {
            if (isAdmin && companyId != null) {
                // Admin exception: see all company parties pending approval
                Specification<Party> adminPartySpec = (root, q, cb) -> {
                    jakarta.persistence.criteria.Subquery<String> sub = q.subquery(String.class);
                    jakarta.persistence.criteria.Root<com.courierapp.entity.User> userRoot =
                            sub.from(com.courierapp.entity.User.class);
                    sub.select(userRoot.get("username"))
                       .where(cb.equal(userRoot.get("company").get("id"), companyId));
                    return cb.and(
                            cb.equal(root.get("partyStatus"), PartyStatus.PENDING_APPROVAL),
                            root.get("createdBy").in(sub),
                            cb.notLike(root.get("partyCode"), "COMPANY%")
                    );
                };
                partiesPendingMyApproval = partyRepository.findAll(adminPartySpec).stream()
                        .map(p -> {
                            PartyResponse base = partyMapper.toResponse(p);
                            List<String> approvers = approvalAuthorizationService
                                    .resolveApproversAtLevel(p.getCreatedBy(), "MASTER", p.getCurrentApprovalLevel());
                            return new PartyResponse(base.id(), base.partyCode(), base.partyName(),
                                    base.addressLine1(), base.addressLine2(), base.city(), base.state(),
                                    base.pincode(), base.country(), base.phone(), base.email(), base.gstin(),
                                    base.partyType(), base.active(), base.partyStatus(), base.companyName(),
                                    base.currentApprovalLevel(), base.createdAt(), base.createdBy(),
                                    base.updatedAt(), base.updatedBy(), approvers);
                        }).toList();
            } else {
                Specification<Party> partySpec = (root, q, cb) -> cb.and(
                        cb.equal(root.get("partyStatus"), PartyStatus.PENDING_APPROVAL),
                        cb.notEqual(root.get("createdBy"), username)
                );
                partiesPendingMyApproval = partyRepository.findAll(partySpec).stream()
                        .filter(p -> !p.getPartyCode().startsWith("COMPANY"))
                        .filter(p -> approvalAuthorizationService.isDesignatedApproverAtLevel(
                                username, p.getCreatedBy(), "MASTER", p.getCurrentApprovalLevel()))
                        .map(p -> {
                            PartyResponse base = partyMapper.toResponse(p);
                            List<String> approvers = approvalAuthorizationService
                                    .resolveApproversAtLevel(p.getCreatedBy(), "MASTER", p.getCurrentApprovalLevel());
                            return new PartyResponse(base.id(), base.partyCode(), base.partyName(),
                                    base.addressLine1(), base.addressLine2(), base.city(), base.state(),
                                    base.pincode(), base.country(), base.phone(), base.email(), base.gstin(),
                                    base.partyType(), base.active(), base.partyStatus(), base.companyName(),
                                    base.currentApprovalLevel(), base.createdAt(), base.createdBy(),
                                    base.updatedAt(), base.updatedBy(), approvers);
                        }).toList();
            }
        }

        // ── Parties I sent (created) that are pending approval ────────────────
        Specification<Party> myPartiesSpec = (root, q, cb) -> cb.and(
                cb.equal(root.get("partyStatus"), PartyStatus.PENDING_APPROVAL),
                cb.equal(root.get("createdBy"), username),
                cb.notLike(root.get("partyCode"), "COMPANY%")
        );
        List<PartyResponse> myPartiesPendingSent = partyRepository.findAll(myPartiesSpec)
                .stream().map(p -> {
                    PartyResponse base = partyMapper.toResponse(p);
                    List<String> approvers = approvalAuthorizationService
                            .resolveApproversAtLevel(p.getCreatedBy(), "MASTER", p.getCurrentApprovalLevel());
                    return new PartyResponse(base.id(), base.partyCode(), base.partyName(),
                            base.addressLine1(), base.addressLine2(), base.city(), base.state(),
                            base.pincode(), base.country(), base.phone(), base.email(), base.gstin(),
                            base.partyType(), base.active(), base.partyStatus(), base.companyName(),
                            base.currentApprovalLevel(), base.createdAt(), base.createdBy(),
                            base.updatedAt(), base.updatedBy(), approvers);
                }).toList();

        // ── Approved bookings pending print ───────────────────────────────────
        // BOOKING_VIEW/APPROVE users see all company bookings pending print.
        // Other users (creators) always see their OWN approved bookings pending print.

        // Only users with BOOKING_PRINT permission can see Pending to Print
        boolean canPrint = auth.getAuthorities().stream()
                .anyMatch(a -> "BOOKING_PRINT".equals(a.getAuthority()));

        List<BookingResponse> pendingToPrint = List.of();
        if (canPrint) {
            // BOOKING_VIEW/APPROVE/ADMIN see all company bookings; others see only their own
            boolean canViewAllBookings = isAdmin || auth.getAuthorities().stream()
                    .anyMatch(a -> "BOOKING_VIEW".equals(a.getAuthority()) || "BOOKING_APPROVE".equals(a.getAuthority()));

            Specification<Booking> printSpec = (root, q, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                predicates.add(cb.equal(root.get("status"), BookingStatus.APPROVED));
                predicates.add(cb.equal(root.get("printTaken"), false));
                if (canViewAllBookings && companyId != null) {
                    jakarta.persistence.criteria.Subquery<String> sub = q.subquery(String.class);
                    jakarta.persistence.criteria.Root<com.courierapp.entity.User> userRoot =
                            sub.from(com.courierapp.entity.User.class);
                    sub.select(userRoot.get("username"))
                       .where(cb.equal(userRoot.get("company").get("id"), companyId));
                    predicates.add(root.get("createdBy").in(sub));
                } else {
                    predicates.add(cb.equal(root.get("createdBy"), username));
                }
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            pendingToPrint = bookingRepository.findAll(printSpec)
                    .stream().map(bookingMapper::toResponse).toList();
        }

        // ── All PENDING_APPROVAL bookings in this company (for BOOKING_VIEW users) ──
        // Enriched with pendingApprovers so viewers know who needs to act on each booking.
        boolean canViewBookings = isAdmin || auth.getAuthorities().stream()
                .anyMatch(a -> "BOOKING_VIEW".equals(a.getAuthority()));

        List<BookingResponse> allPendingApprovalBookings = List.of();
        if (canViewBookings && companyId != null) {
            Specification<Booking> allPendingSpec = (root, q, cb) -> {
                jakarta.persistence.criteria.Subquery<String> sub = q.subquery(String.class);
                jakarta.persistence.criteria.Root<com.courierapp.entity.User> userRoot =
                        sub.from(com.courierapp.entity.User.class);
                sub.select(userRoot.get("username"))
                   .where(cb.equal(userRoot.get("company").get("id"), companyId));
                return cb.and(
                        cb.equal(root.get("status"), BookingStatus.PENDING_APPROVAL),
                        root.get("createdBy").in(sub)
                );
            };
            allPendingApprovalBookings = bookingRepository.findAll(allPendingSpec)
                    .stream().map(b -> {
                        BookingResponse base = bookingMapper.toResponse(b);
                        List<String> approvers = approvalAuthorizationService
                                .resolveApproversAtLevel(b.getCreatedBy(), "BOOKING", b.getCurrentApprovalLevel());
                        return new BookingResponse(base.id(), base.bookingNumber(), base.bookingDate(),
                                base.sender(), base.receiver(), base.courierWay(), base.packageType(),
                                base.itemDescription(), base.weightKg(), base.noOfPackages(),
                                base.courierMode(), base.specialInstructions(), base.status(),
                                base.awbNumber(), base.approverUsername(), base.approvalTimestamp(),
                                base.approvalRemarks(), base.companyPoNo(), base.printTaken(),
                                base.cancellationRemarks(), base.currentApprovalLevel(),
                                base.createdAt(), base.createdBy(), base.updatedAt(), base.updatedBy(),
                                approvers);
                    }).toList();
        }

        // ── All PENDING_APPROVAL parties in this company (for MASTER_VIEW users) ─
        boolean canViewParties = isAdmin || auth.getAuthorities().stream()
                .anyMatch(a -> "MASTER_VIEW".equals(a.getAuthority()));

        List<PartyResponse> allPendingApprovalParties = List.of();
        if (canViewParties && companyId != null) {
            Specification<Party> allPartiesSpec = (root, q, cb) -> {
                jakarta.persistence.criteria.Subquery<String> sub = q.subquery(String.class);
                jakarta.persistence.criteria.Root<com.courierapp.entity.User> userRoot =
                        sub.from(com.courierapp.entity.User.class);
                sub.select(userRoot.get("username"))
                   .where(cb.equal(userRoot.get("company").get("id"), companyId));
                return cb.and(
                        cb.equal(root.get("partyStatus"), PartyStatus.PENDING_APPROVAL),
                        root.get("createdBy").in(sub),
                        cb.notLike(root.get("partyCode"), "COMPANY%")
                );
            };
            allPendingApprovalParties = partyRepository.findAll(allPartiesSpec)
                    .stream().map(p -> {
                        PartyResponse base = partyMapper.toResponse(p);
                        List<String> approvers = approvalAuthorizationService
                                .resolveApproversAtLevel(p.getCreatedBy(), "MASTER", p.getCurrentApprovalLevel());
                        return new PartyResponse(base.id(), base.partyCode(), base.partyName(),
                                base.addressLine1(), base.addressLine2(), base.city(), base.state(),
                                base.pincode(), base.country(), base.phone(), base.email(), base.gstin(),
                                base.partyType(), base.active(), base.partyStatus(), base.companyName(),
                                base.currentApprovalLevel(), base.createdAt(), base.createdBy(),
                                base.updatedAt(), base.updatedBy(), approvers);
                    }).toList();
        }

        return new DashboardResponse(
                bookingsPendingMyApproval,
                myBookingsPendingSent,
                partiesPendingMyApproval,
                myPartiesPendingSent,
                pendingToPrint,
                allPendingApprovalBookings,
                allPendingApprovalParties
        );
    }
}
