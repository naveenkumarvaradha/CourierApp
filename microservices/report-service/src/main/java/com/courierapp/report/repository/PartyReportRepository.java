package com.courierapp.report.repository;

import com.courierapp.report.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PartyReportRepository extends JpaRepository<Party, Long> {

    @Query("SELECT p FROM Party p WHERE p.createdAt >= :from AND p.createdAt <= :to ORDER BY p.createdAt ASC")
    List<Party> findByCreatedAtRange(@Param("from") Instant from, @Param("to") Instant to);
}
