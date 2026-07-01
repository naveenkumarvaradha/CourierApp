package com.courierapp.repository;

import com.courierapp.entity.FlexFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlexFieldValueRepository extends JpaRepository<FlexFieldValue, Long> {
    List<FlexFieldValue> findByModuleAndEntityId(String module, Long entityId);
    Optional<FlexFieldValue> findByModuleAndEntityIdAndFieldId(String module, Long entityId, Long fieldId);
}
