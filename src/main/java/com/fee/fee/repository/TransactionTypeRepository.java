package com.fee.fee.repository;

import com.fee.fee.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Long> {
    Optional<TransactionType> findByCode(String code);
    boolean existsByCode(String code);
}
