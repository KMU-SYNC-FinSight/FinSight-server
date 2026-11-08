package com.finsight.score.internal.repository;

import com.finsight.score.internal.domain.OperationalScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperationalScoreRepository
        extends JpaRepository<OperationalScore, Long> {

    Optional<OperationalScore> findByStoreId(Long storeId);
}