package com.finsight.store.repository;

import com.finsight.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    // 내 매장 목록
    List<Store> findByUserId(Long userId);

    // 소유권까지 함께 확인하는 상세 조회
    Optional<Store> findByIdAndUserId(Long id, Long userId);
}