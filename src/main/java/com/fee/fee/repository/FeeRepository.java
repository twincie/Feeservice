package com.fee.fee.repository;

import com.fee.fee.domain.Fee;
import com.fee.fee.domain.Tenant;
import com.fee.fee.enumeration.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {
    List<Fee> findByTenant(Tenant tenant);
    List<Fee> findByTenantAndFeeType(Tenant tenant, FeeType feeType);
    List<Fee> findByTenantAndIsActive(Tenant tenant, Boolean isActive);
    Optional<Fee> findByIdAndTenant(Long id, Tenant tenant);
    boolean existsByTenantAndName(Tenant tenant, String name);
    boolean existsByTenantAndNameAndIdNot(Tenant tenant, String name, Long id);
}
