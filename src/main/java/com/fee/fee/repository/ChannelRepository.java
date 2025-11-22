package com.fee.fee.repository;

import com.fee.fee.domain.Channel;
import com.fee.fee.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
//    Set<Channel> findByIdInAndIsActive(List<Long> ids, Boolean isActive);
//    List<Channel> findByIsActive(Boolean isActive);
//    boolean existsByCode(String code);

    Optional<Channel> findByNameAndIsActive(String name, Boolean isActive);
    Optional<Channel> findByCodeAndIsActive(String code, Boolean isActive);
    List<Channel> findByIsActive(Boolean isActive);
    boolean existsByName(String name);
    boolean existsByCode(String code);
    Set<Channel> findByNameInAndIsActive(Collection<String> name, Boolean isActive);
    List<Channel> findByCode(String code);
}