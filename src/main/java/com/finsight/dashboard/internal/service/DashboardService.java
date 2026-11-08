package com.finsight.dashboard.internal.service;

import com.finsight.dashboard.internal.dto.DashboardResponse;
import com.finsight.sales.DailySales;
import com.finsight.sales.SalesMetricQuery;
import com.finsight.score.ScoreQuery;
import com.finsight.score.ScoreSnapshot;
import com.finsight.store.StoreQuery;
import com.finsight.store.StoreSummary;
import com.finsight.visitor.VisitorMetricQuery;
import com.finsight.visitor.VisitorMetricSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StoreQuery storeQuery;
    private final ScoreQuery scoreQuery;
    private final VisitorMetricQuery visitorMetricQuery;
    private final SalesMetricQuery salesMetricQuery;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId, Long storeId) {
        // 소유권 확인
        StoreSummary store = storeQuery.getOwnedStore(userId, storeId);

        // 점수 (없을 수 있음)
        Optional<ScoreSnapshot> scoreOpt = scoreQuery.findByStoreId(storeId);

        // 방문객: 가장 최근 것 하나 (대시보드는 최신 스냅샷)
        Optional<VisitorMetricSnapshot> latestVisitor =
                visitorMetricQuery.findAllByStoreId(storeId).stream()
                        .max(Comparator.comparing(VisitorMetricSnapshot::id));

        // 매출: 전체 합계 + 최근 날짜
        List<DailySales> sales = salesMetricQuery.findAllByStoreId(storeId);

        return buildResponse(store, scoreOpt, latestVisitor, sales);
    }

    private DashboardResponse buildResponse(
            StoreSummary store,
            Optional<ScoreSnapshot> scoreOpt,
            Optional<VisitorMetricSnapshot> latestVisitor,
            List<DailySales> sales
    ) {
        // 방문객 지표
        DashboardResponse.VisitorMetrics visitorMetrics =
                latestVisitor.map(v -> new DashboardResponse.VisitorMetrics(
                        v.averageOccupancy(),
                        v.peakOccupancy(),
                        v.trackedObjectCount(),
                        v.averageDwellSeconds(),
                        v.congestionLevel()
                )).orElse(null);

        // 매출 지표: 총매출/총건수 + 방문객당 매출
        Long totalSales = sales.stream()
                .mapToLong(DailySales::salesAmount).sum();
        Integer totalTx = sales.stream()
                .mapToInt(DailySales::transactionCount).sum();

        Double salesPerVisitor = null;
        if (latestVisitor.isPresent()
                && latestVisitor.get().trackedObjectCount() != null
                && latestVisitor.get().trackedObjectCount() > 0
                && totalSales > 0) {
            salesPerVisitor = round2(
                    (double) totalSales
                            / latestVisitor.get().trackedObjectCount());
        }

        DashboardResponse.SalesMetrics salesMetrics =
                sales.isEmpty() ? null
                        : new DashboardResponse.SalesMetrics(
                        totalSales, totalTx, salesPerVisitor);

        return new DashboardResponse(
                store.storeId(),
                store.name(),
                scoreOpt.map(ScoreSnapshot::score).orElse(null),
                scoreOpt.map(s -> s.grade().name()).orElse(null),
                scoreOpt.map(s -> s.riskLevel().name()).orElse(null),
                visitorMetrics,
                salesMetrics
        );
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}