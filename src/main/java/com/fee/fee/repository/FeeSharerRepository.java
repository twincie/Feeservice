package com.fee.fee.repository;

import com.fee.fee.domain.Fee;
import com.fee.fee.domain.FeeSharer;
import com.fee.fee.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeSharerRepository extends JpaRepository<FeeSharer, Long> {
    List<FeeSharer> findByFeeId(Long feeId);
    void deleteByFeeId(Long feeId);
    List<FeeSharer> findByFeeAndIsPrimary(Fee fee, Boolean isPrimary);
}