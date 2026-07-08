package com.courierapp.admin.repository;

import com.courierapp.admin.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<Department> findByActiveTrueOrderByNameAsc();
}
