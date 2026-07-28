package com.finsight.sales.repository;

import com.finsight.sales.domain.SalesMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalesMetricRepository extends JpaRepository<SalesMetric, Long> {

    // 덮어쓰기 판단: 이 매장에 이 날짜 데이터가 이미 있는지
    Optional<SalesMetric> findByStoreIdAndMetricDate(
            Long storeId, LocalDate metricDate);
    
    // 날짜 오름차순 — 추세 계산용
    List<SalesMetric> findByStoreIdOrderByMetricDateAsc(Long storeId);
}