package com.courierapp.service.impl;

import com.courierapp.dto.PageResponse;
import com.courierapp.dto.master.PartyRequest;
import com.courierapp.dto.master.PartyResponse;
import com.courierapp.entity.Party;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.PartyMapper;
import com.courierapp.repository.PartyRepository;
import com.courierapp.service.PartyService;
import jakarta.persistence.criteria.Predicate;
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

@Service
@Transactional
public class PartyServiceImpl implements PartyService {

    private static final String PARTY_PREFIX = "PTY";

    private final PartyRepository partyRepository;
    private final PartyMapper partyMapper;

    public PartyServiceImpl(PartyRepository partyRepository, PartyMapper partyMapper) {
        this.partyRepository = partyRepository;
        this.partyMapper = partyMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PartyResponse> list(String name, String city, String pincode,
                                            Boolean active, Pageable pageable) {
        Specification<Party> spec = buildSpec(name, city, pincode, active);
        Page<Party> page = partyRepository.findAll(spec, pageable);
        return PageResponse.from(page, partyMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartyResponse> listAllActive() {
        Specification<Party> spec = (root, q, cb) -> cb.isTrue(root.get("active"));
        return partyRepository.findAll(spec, Sort.by("partyName")).stream()
                .map(partyMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PartyResponse get(Long id) {
        return partyMapper.toResponse(findParty(id));
    }

    @Override
    public PartyResponse create(PartyRequest request) {
        Party party = new Party();
        apply(party, request);
        party.setPartyCode(generatePartyCode());
        return partyMapper.toResponse(partyRepository.save(party));
    }

    @Override
    public PartyResponse update(Long id, PartyRequest request) {
        Party party = findParty(id);
        apply(party, request);
        return partyMapper.toResponse(partyRepository.save(party));
    }

    @Override
    public void delete(Long id) {
        Party party = findParty(id);
        partyRepository.delete(party);
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
