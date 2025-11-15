package com.fee.fee.repository;

import com.fee.fee.domain.FeeSharer;
import com.fee.fee.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeSharerRepository extends JpaRepository<FeeSharer, Long> {
    List<FeeSharer> findByTenantAndTransactionTypeCodeAndChannelCode(Tenant tenant, String txType, String channel);
}