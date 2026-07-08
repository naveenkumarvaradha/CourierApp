package com.courierapp.repository;

import com.courierapp.entity.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {

    List<ReportSchedule> findByCompanyIdOrderByScheduleNameAsc(Long companyId);

    @Query("SELECT s FROM ReportSchedule s WHERE s.enabled = true AND (s.nextRunAt IS NULL OR s.nextRunAt <= :now)")
    List<ReportSchedule> findDueSchedules(@Param("now") Instant now);
}
