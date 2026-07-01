package com.courierapp.repository;

import com.courierapp.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long>, JpaSpecificationExecutor<Party> {
    Optional<Party> findByPartyCode(String partyCode);
    boolean existsByPartyCode(String partyCode);
    long countByPartyCodeStartingWith(String prefix);
}
