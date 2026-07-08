package com.courierapp.admin.repository;

import com.courierapp.admin.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCompanyCodeIgnoreCase(String code);
    List<Company> findByActiveTrueOrderByNameAsc();
}
