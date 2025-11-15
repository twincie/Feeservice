package com.fee.fee.repository;

import com.fee.fee.domain.TransactionChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionChannelRepository extends JpaRepository<TransactionChannel, Long> {
    Optional<TransactionChannel> findByCode(String code);
    boolean existsByCode(String code);
}