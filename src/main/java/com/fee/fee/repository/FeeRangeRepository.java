package com.fee.fee.repository;

import com.fee.fee.domain.FeeRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRangeRepository extends JpaRepository<FeeRange, Long> {
    List<FeeRange> findByFeeId(Long feeId);
    void deleteByFeeId(Long feeId);
}