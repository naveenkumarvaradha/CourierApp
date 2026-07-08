package com.courierapp.party.repository;

import com.courierapp.party.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long>, JpaSpecificationExecutor<Party> {
    boolean existsByPartyCode(String partyCode);
    long countByPartyCodeStartingWith(String prefix);
    Optional<Party> findByPartyCode(String partyCode);
}
