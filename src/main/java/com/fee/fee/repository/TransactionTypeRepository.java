package com.fee.fee.repository;

import com.fee.fee.domain.*;
import com.fee.fee.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Long> {
    Optional<TransactionType> findByIdAndIsActive(Long id, Boolean isActive);
    Optional<TransactionType> findByNameAndIsActive(String name, Boolean isActive);
    Optional<TransactionType> findByCodeAndIsActive(String code, Boolean isActive);
    List<TransactionType> findByIsActive(Boolean isActive);
    boolean existsByName(String name);
    boolean existsByCode(String code);
}