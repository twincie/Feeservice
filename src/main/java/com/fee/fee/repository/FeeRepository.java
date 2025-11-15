package com.fee.fee.repository;

import com.fee.fee.domain.Fee;
import com.fee.fee.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {
    List<Fee> findByTenantAndTransactionTypeCodeAndChannelCodeAndFeeLevelAndClientIdAndProductCodeAndIsProcessorFee(
            Tenant tenant, String txType, String channel, Fee.FeeLevel level, String clientId, String productCode, boolean isProcessor);

    List<Fee> findByTenantAndTransactionTypeCodeAndChannelCodeAndFeeLevelAndProductCodeAndIsProcessorFee(
            Tenant tenant, String txType, String channel, Fee.FeeLevel level, String productCode, boolean isProcessor);

    List<Fee> findByTenantAndTransactionTypeCodeAndChannelCodeAndFeeLevelAndIsProcessorFee(
            Tenant tenant, String txType, String channel, Fee.FeeLevel level, boolean isProcessor);

    List<Fee> findByTenantAndTransactionTypeCodeAndChannelCodeAndIsProcessorFee(
            Tenant tenant, String txType, String channel, boolean isProcessor);
}
