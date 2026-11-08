package com.finsight.visitor.internal.repository;

import com.finsight.visitor.internal.domain.VisitorMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitorMetricRepository
        extends JpaRepository<VisitorMetric, Long> {

    // 업로드 단위 결과 조회 (대시보드/검증용)
    Optional<VisitorMetric> findByDataUploadId(Long dataUploadId);

    List<VisitorMetric> findByStoreIdOrderByIdAsc(Long storeId);
}