package com.courierapp.repository;

import com.courierapp.entity.FlexFieldOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlexFieldOptionRepository extends JpaRepository<FlexFieldOption, Long> {
    List<FlexFieldOption> findByFieldIdAndActiveTrueOrderBySortOrderAsc(Long fieldId);
}
