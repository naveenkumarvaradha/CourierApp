package com.courierapp.repository;

import com.courierapp.entity.PasswordPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicy, Long> {
}
