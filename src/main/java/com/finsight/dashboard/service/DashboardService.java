package com.finsight.dashboard.service;

import com.finsight.dashboard.dto.DashboardResponse;
import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.sales.domain.SalesMetric;
import com.finsight.sales.repository.SalesMetricRepository;
import com.finsight.score.domain.OperationalScore;
import com.finsight.score.repository.OperationalScoreRepository;
import com.finsight.store.domain.Store;
import com.finsight.store.repository.StoreRepository;
import com.finsight.visitor.domain.VisitorMetric;
import com.finsight.visitor.repository.VisitorMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StoreRepository storeRepository;
    private final OperationalScoreRepository operationalScoreRepository;
    private final VisitorMetricRepository visitorMetricRepository;
    private final SalesMetricRepository salesMetricRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId, Long storeId) {
        // 소유권 확인
        Store store = storeRepository.findByIdAndUserId(storeId, userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 점수 (없을 수 있음)
        Optional<OperationalScore> scoreOpt =
                operationalScoreRepository.findByStoreId(storeId);

        // 방문객: 가장 최근 것 하나 (대시보드는 최신 스냅샷)
        List<VisitorMetric> visitors =
                visitorMetricRepository.findByStoreId(storeId);
        Optional<VisitorMetric> latestVisitor = visitors.stream()
                .max(Comparator.comparing(VisitorMetric::getId));

        // 매출: 전체 합계 + 최근 날짜
        List<SalesMetric> sales =
                salesMetricRepository.findByStoreIdOrderByMetricDateAsc(storeId);

        return buildResponse(store, scoreOpt, latestVisitor, sales);
    }

    private DashboardResponse buildResponse(
            Store store,
            Optional<OperationalScore> scoreOpt,
            Optional<VisitorMetric> latestVisitor,
            List<SalesMetric> sales
    ) {
        // 방문객 지표
        DashboardResponse.VisitorMetrics visitorMetrics =
                latestVisitor.map(v -> new DashboardResponse.VisitorMetrics(
                        v.getAverageOccupancy(),
                        v.getPeakOccupancy(),
                        v.getTrackedObjectCount(),
                        v.getAverageDwellSeconds(),
                        v.getCongestionLevel()
                )).orElse(null);

        // 매출 지표: 총매출/총건수 + 방문객당 매출
        Long totalSales = sales.stream()
                .mapToLong(SalesMetric::getSalesAmount).sum();
        Integer totalTx = sales.stream()
                .mapToInt(SalesMetric::getTransactionCount).sum();

        Double salesPerVisitor = null;
        if (latestVisitor.isPresent()
                && latestVisitor.get().getTrackedObjectCount() != null
                && latestVisitor.get().getTrackedObjectCount() > 0
                && totalSales > 0) {
            salesPerVisitor = round2(
                    (double) totalSales
                            / latestVisitor.get().getTrackedObjectCount());
        }

        DashboardResponse.SalesMetrics salesMetrics =
                sales.isEmpty() ? null
                        : new DashboardResponse.SalesMetrics(
                        totalSales, totalTx, salesPerVisitor);

        return new DashboardResponse(
                store.getId(),
                store.getName(),
                scoreOpt.map(OperationalScore::getScore).orElse(null),
                scoreOpt.map(s -> s.getGrade().name()).orElse(null),
                scoreOpt.map(s -> s.getRiskLevel().name()).orElse(null),
                visitorMetrics,
                salesMetrics
        );
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}