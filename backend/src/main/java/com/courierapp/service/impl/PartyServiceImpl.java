package com.courierapp.service.impl;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.master.PartyRequest;
import com.courierapp.dto.master.PartyResponse;
import com.courierapp.entity.Party;
import com.courierapp.enums.PartyStatus;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.PartyMapper;
import com.courierapp.repository.PartyRepository;
import com.courierapp.service.ApprovalAuthorizationService;
import com.courierapp.service.AuditLogService;
import com.courierapp.service.PartyService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@Transactional
public class PartyServiceImpl implements PartyService {

    private static final String PARTY_PREFIX = "PTY";
    private static final String MODULE = "MASTER";

    private final PartyRepository partyRepository;
    private final PartyMapper partyMapper;
    private final ApprovalAuthorizationService approvalAuthorizationService;
    private final AuditLogService auditLogService;

    public PartyServiceImpl(PartyRepository partyRepository,
                            PartyMapper partyMapper,
                            ApprovalAuthorizationService approvalAuthorizationService,
                            AuditLogService auditLogService) {
        this.partyRepository = partyRepository;
        this.partyMapper = partyMapper;
        this.approvalAuthorizationService = approvalAuthorizationService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PartyResponse> list(String name, String city, String pincode,
                                            Boolean active, Pageable pageable) {
        log.debug("Listing parties: name={}, city={}, pincode={}, active={}", name, city, pincode, active);
        Specification<Party> spec = buildSpec(name, city, pincode, active);
        Page<Party> page = partyRepository.findAll(spec, pageable);
        log.debug("Found {} parties matching filters", page.getTotalElements());
        return PageResponse.from(page, partyMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartyResponse> listAllActive() {
        Specification<Party> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("partyStatus"), PartyStatus.ACTIVE),
                cb.notEqual(root.get("partyCode"), "COMPANY001")
        );
        List<Party> parties = partyRepository.findAll(spec, Sort.by("partyName"));
        log.debug("Returning {} active parties for dropdown (company party excluded)", parties.size());
        return parties.stream().map(partyMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PartyResponse get(Long id) {
        log.debug("Fetching party id={}", id);
        return partyMapper.toResponse(findParty(id));
    }

    @Override
    public PartyResponse create(PartyRequest request) {
        log.info("Creating party: name={}, type={}", request.partyName(), request.partyType());
        Party party = new Party();
        apply(party, request);
        party.setPartyCode(generatePartyCode());
        party.setPartyStatus(PartyStatus.PENDING_APPROVAL);
        party.setActive(false);
        Party saved = partyRepository.save(party);
        log.info("Party created: code={}, id={}, status=PENDING_APPROVAL", saved.getPartyCode(), saved.getId());
        auditLogService.log("PARTY", "CREATE", saved.getId(), saved.getPartyCode() + " " + saved.getPartyName(),
                saved.getCreatedBy(), "Type=" + saved.getPartyType() + ", status=PENDING_APPROVAL");
        return partyMapper.toResponse(saved);
    }

    @Override
    public PartyResponse update(Long id, PartyRequest request) {
        log.info("Updating party id={}", id);
        Party party = findParty(id);
        apply(party, request);
        Party saved = partyRepository.save(party);
        log.info("Party updated: code={}, id={}", saved.getPartyCode(), saved.getId());
        auditLogService.log("PARTY", "UPDATE", saved.getId(), saved.getPartyCode() + " " + saved.getPartyName(),
                saved.getUpdatedBy(), "Party details updated");
        return partyMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting party id={}", id);
        Party party = findParty(id);
        if (party.getPartyStatus() == PartyStatus.ACTIVE) {
            throw new BusinessException("Cannot delete an ACTIVE party. Deactivate it first.");
        }
        String name = party.getPartyCode() + " " + party.getPartyName();
        String creator = party.getCreatedBy();
        partyRepository.delete(party);
        log.info("Party deleted: code={}, id={}", party.getPartyCode(), id);
        auditLogService.log("PARTY", "DELETE", id, name, creator, null);
    }

    @Override
    public PartyResponse approve(Long id, String approverUsername) {
        log.info("Approving party id={} by user='{}'", id, approverUsername);
        Party party = requirePendingApproval(id, approverUsername);
        party.setPartyStatus(PartyStatus.ACTIVE);
        party.setActive(true);
        Party saved = partyRepository.save(party);
        log.info("Party APPROVED: code={}, id={}, approver='{}'", saved.getPartyCode(), saved.getId(), approverUsername);
        auditLogService.log("PARTY", "APPROVE", saved.getId(), saved.getPartyCode() + " " + saved.getPartyName(),
                approverUsername, "Party activated");
        return partyMapper.toResponse(saved);
    }

    @Override
    public PartyResponse reject(Long id, String approverUsername, String remarks) {
        log.info("Rejecting party id={} by user='{}', remarks='{}'", id, approverUsername, remarks);
        Party party = requirePendingApproval(id, approverUsername);
        party.setPartyStatus(PartyStatus.REJECTED);
        party.setActive(false);
        Party saved = partyRepository.save(party);
        log.info("Party REJECTED: code={}, id={}, approver='{}'", saved.getPartyCode(), saved.getId(), approverUsername);
        auditLogService.log("PARTY", "REJECT", saved.getId(), saved.getPartyCode() + " " + saved.getPartyName(),
                approverUsername, "Remarks: " + remarks);
        return partyMapper.toResponse(saved);
    }

    private Party requirePendingApproval(Long id, String approverUsername) {
        Party party = findParty(id);
        if (party.getPartyStatus() != PartyStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only parties in PENDING_APPROVAL state can be approved or rejected");
        }
        String creator = party.getCreatedBy();
        if (!approvalAuthorizationService.isAuthorizedApprover(approverUsername, creator, MODULE)) {
            throw new BusinessException(
                    "You are not a designated approver for master data",
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }
        return party;
    }

    private Party findParty(Long id) {
        return partyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Party", id));
    }

    private void apply(Party party, PartyRequest r) {
        party.setPartyName(r.partyName());
        party.setAddressLine1(r.addressLine1());
        party.setAddressLine2(r.addressLine2());
        party.setCity(r.city());
        party.setState(r.state());
        party.setPincode(r.pincode());
        party.setCountry(r.country());
        party.setPhone(r.phone());
        party.setEmail(r.email());
        party.setGstin(r.gstin());
        party.setPartyType(r.partyType());
        party.setActive(r.active());
    }

    private String generatePartyCode() {
        long count = partyRepository.countByPartyCodeStartingWith(PARTY_PREFIX);
        String code;
        do {
            count++;
            code = String.format("%s%06d", PARTY_PREFIX, count);
        } while (partyRepository.existsByPartyCode(code));
        return code;
    }

    private Specification<Party> buildSpec(String name, String city, String pincode, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Exclude internal company parties; they are managed via Company Setup
            predicates.add(cb.notLike(root.get("partyCode"), "COMPANY%"));
            if (StringUtils.hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("partyName")),
                        "%" + name.toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(city)) {
                predicates.add(cb.like(cb.lower(root.get("city")),
                        "%" + city.toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(pincode)) {
                predicates.add(cb.like(root.get("pincode"), pincode + "%"));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
