package com.courierapp.repository;

import com.courierapp.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByActiveTrueOrderByNameAsc();
    Optional<Company> findByCompanyCodeIgnoreCase(String companyCode);
    boolean existsByCompanyCodeIgnoreCase(String companyCode);
    boolean existsByNameIgnoreCase(String name);
}
