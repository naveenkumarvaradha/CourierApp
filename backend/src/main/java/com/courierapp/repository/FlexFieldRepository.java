package com.courierapp.repository;

import com.courierapp.entity.FlexField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlexFieldRepository extends JpaRepository<FlexField, Long> {
    List<FlexField> findByModuleAndActiveTrueOrderBySortOrderAscFieldNameAsc(String module);
    List<FlexField> findByModuleOrderBySortOrderAscFieldNameAsc(String module);
    boolean existsByModuleAndFieldNameIgnoreCase(String module, String fieldName);
}
