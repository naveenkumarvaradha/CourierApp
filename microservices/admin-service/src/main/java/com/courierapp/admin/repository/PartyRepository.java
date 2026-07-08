package com.courierapp.admin.repository;

import com.courierapp.admin.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {
    Optional<Party> findByPartyCode(String partyCode);
}
