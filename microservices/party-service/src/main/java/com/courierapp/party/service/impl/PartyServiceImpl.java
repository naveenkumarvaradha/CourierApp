package com.courierapp.party.service.impl;

import com.courierapp.party.dto.PageResponse;
import com.courierapp.party.dto.master.PartyRequest;
import com.courierapp.party.dto.master.PartyResponse;
import com.courierapp.party.entity.Party;
import com.courierapp.party.enums.PartyStatus;
import com.courierapp.party.exception.BusinessException;
import com.courierapp.party.exception.ResourceNotFoundException;
import com.courierapp.party.repository.PartyRepository;
import com.courierapp.party.service.ApprovalAuthorizationService;
import com.courierapp.party.service.AuditLogService;
import com.courierapp.party.service.PartyService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class PartyServiceImpl implements PartyService {

    private static final String PARTY_PREFIX = "PTY";
    private static final String MODULE = "MASTER";

    private final PartyRepository partyRepository;
    private final ApprovalAuthorizationService approvalAuthorizationService;
    private final AuditLogService auditLogService;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    public PartyServiceImpl(PartyRepository partyRepository,
                            ApprovalAuthorizationService approvalAuthorizationService,
                            AuditLogService auditLogService,
                            KafkaTemplate<String, Map<String, Object>> kafkaTemplate) {
        this.partyRepository = partyRepository;
        this.approvalAuthorizationService = approvalAuthorizationService;
        this.auditLogService = auditLogService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PartyResponse> list(String name, String city, String pincode,
                                            Boolean active, Pageable pageable) {
        Specification<Party> spec = buildSpec(name, city, pincode, active);
        Page<Party> page = partyRepository.findAll(spec, pageable);
        return PageResponse.from(page, p -> {
            PartyResponse base = toResponse(p);
            if (p.getPartyStatus() == PartyStatus.PENDING_APPROVAL) {
                List<String> approvers = approvalAuthorizationService
                        .resolveApproversAtLevel(p.getCreatedBy(), MODULE, p.getCurrentApprovalLevel());
                return new PartyResponse(base.id(), base.partyCode(), base.partyName(),
                        base.addressLine1(), base.addressLine2(), base.city(), base.state(),
                        base.pincode(), base.country(), base.phone(), base.email(), base.gstin(),
                        base.partyType(), base.active(), base.partyStatus(), base.companyName(),
                        base.currentApprovalLevel(), base.createdAt(), base.createdBy(),
                        base.updatedAt(), base.updatedBy(), approvers);
            }
            return base;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartyResponse> listAllActive() {
        Specification<Party> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("partyStatus"), PartyStatus.ACTIVE),
                cb.notLike(root.get("partyCode"), "COMPANY%")
        );
        return partyRepository.findAll(spec, Sort.by("partyName")).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PartyResponse get(Long id) {
        return toResponse(findParty(id));
    }

    @Override
    public PartyResponse create(PartyRequest request) {
        Party party = new Party();
        apply(party, request);
        party.setPartyCode(generatePartyCode());
        party.setPartyStatus(PartyStatus.PENDING_APPROVAL);
        party.setActive(false);
        party.setCurrentApprovalLevel(1);
        Party saved = partyRepository.save(party);
        auditLogService.log("PARTY", "CREATE", saved.getId(), saved.getPartyCode() + " " + saved.getPartyName(),
                saved.getCreatedBy(), "Type=" + saved.getPartyType() + ", status=PENDING_APPROVAL");
        publishEvent("party.events", Map.of(
                "eventType", "PARTY_CREATED",
                "partyId", saved.getId(),
                "partyCode", saved.getPartyCode(),
                "partyName", saved.getPartyName(),
                "createdBy", orEmpty(saved.getCreatedBy())));
        return toResponse(saved);
    }

    @Override
    public PartyResponse update(Long id, PartyRequest request) {
        Party party = findParty(id);
        apply(party, request);
        if (party.getPartyStatus() != PartyStatus.PENDING_APPROVAL) {
            party.setPartyStatus(request.active() ? PartyStatus.ACTIVE : PartyStatus.INACTIVE);
        }
        Party saved = partyRepository.save(party);
        auditLogService.log("PARTY", "UPDATE", saved.getId(), saved.getPartyCode() + " " + saved.getPartyName(),
                saved.getUpdatedBy(), "Party details updated");
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Party party = findParty(id);
        if (party.getPartyStatus() == PartyStatus.ACTIVE) {
            throw new BusinessException("Cannot delete an ACTIVE party. Deactivate it first.");
        }
        String name = party.getPartyCode() + " " + party.getPartyName();
        String creator = party.getCreatedBy();
        partyRepository.delete(party);
        auditLogService.log("PARTY", "DELETE", id, name, creator, null);
    }

    @Override
    public PartyResponse approve(Long id, String approverUsername) {
        Party party = requirePendingApproval(id, approverUsername);
        String creator = party.getCreatedBy();
        int currentLevel = party.getCurrentApprovalLevel();
        int maxLevel = approvalAuthorizationService.getMaxLevel(creator, MODULE);

        if (currentLevel < maxLevel) {
            int nextLevel = currentLevel + 1;
            party.setCurrentApprovalLevel(nextLevel);
            Party saved = partyRepository.save(party);
            auditLogService.log("PARTY", "APPROVE", saved.getId(), saved.getPartyCode() + " " + saved.getPartyName(),
                    approverUsername, "Level " + currentLevel + " approved — escalated to Level " + nextLevel);
            return toResponse(saved);
        } else {
            party.setPartyStatus(PartyStatus.ACTIVE);
            party.setActive(true);
            Party saved = partyRepository.save(party);
            auditLogService.log("PARTY", "APPROVE", saved.getId(), saved.getPartyCode() + " " + saved.getPartyName(),
                    approverUsername, "Final approval (Level " + currentLevel + ") — Party activated");
            publishEvent("party.events", Map.of(
                    "eventType", "PARTY_APPROVED",
                    "partyId", saved.getId(),
                    "partyCode", saved.getPartyCode(),
                    "createdBy", orEmpty(creator),
                    "approverUsername", approverUsername));
            return toResponse(saved);
        }
    }

    @Override
    public PartyResponse reject(Long id, String approverUsername, String remarks) {
        Party party = requirePendingApproval(id, approverUsername);
        party.setPartyStatus(PartyStatus.REJECTED);
        party.setActive(false);
        Party saved = partyRepository.save(party);
        auditLogService.log("PARTY", "REJECT", saved.getId(), saved.getPartyCode() + " " + saved.getPartyName(),
                approverUsername, "Remarks: " + remarks);
        publishEvent("party.events", Map.of(
                "eventType", "PARTY_REJECTED",
                "partyId", saved.getId(),
                "partyCode", saved.getPartyCode(),
                "createdBy", orEmpty(party.getCreatedBy()),
                "approverUsername", approverUsername));
        return toResponse(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Party requirePendingApproval(Long id, String approverUsername) {
        Party party = findParty(id);
        if (party.getPartyStatus() != PartyStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only parties in PENDING_APPROVAL state can be approved or rejected");
        }
        String creator = party.getCreatedBy();
        int level = party.getCurrentApprovalLevel();
        if (!approvalAuthorizationService.isAuthorizedApproverAtLevel(approverUsername, creator, MODULE, level)) {
            throw new BusinessException(
                    "You are not a designated approver for master data at Level " + level,
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }
        return party;
    }

    private Party findParty(Long id) {
        return partyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Party", id));
    }

    private void apply(Party party, PartyRequest r) {
        party.setPartyName(upper(r.partyName()));
        party.setAddressLine1(upper(r.addressLine1()));
        party.setAddressLine2(upper(r.addressLine2()));
        party.setCity(upper(r.city()));
        party.setState(upper(r.state()));
        party.setPincode(upper(r.pincode()));
        party.setCountry(upper(r.country()));
        party.setPhone(upper(r.phone()));
        party.setEmail(r.email());
        party.setGstin(upper(r.gstin()));
        party.setPartyType(r.partyType());
        party.setActive(r.active());
        party.setCompanyName(r.companyName() != null && !r.companyName().isBlank()
                ? r.companyName().toUpperCase(Locale.ROOT) : null);
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase(Locale.ROOT);
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

    private PartyResponse toResponse(Party p) {
        return new PartyResponse(
                p.getId(), p.getPartyCode(), p.getPartyName(),
                p.getAddressLine1(), p.getAddressLine2(), p.getCity(), p.getState(),
                p.getPincode(), p.getCountry(), p.getPhone(), p.getEmail(), p.getGstin(),
                p.getPartyType(), p.isActive(), p.getPartyStatus(), p.getCompanyName(),
                p.getCurrentApprovalLevel(), p.getCreatedAt(), p.getCreatedBy(),
                p.getUpdatedAt(), p.getUpdatedBy(), List.of()
        );
    }

    private Specification<Party> buildSpec(String name, String city, String pincode, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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

    private void publishEvent(String topic, Map<String, Object> event) {
        if (!kafkaEnabled) return;
        try {
            kafkaTemplate.send(topic, (String) event.get("eventType"), event);
        } catch (Exception e) {
            log.warn("Failed to publish Kafka event to {}: {}", topic, e.getMessage());
        }
    }

    private String orEmpty(String s) { return s != null ? s : ""; }
}
