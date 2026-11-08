package com.finsight.score.internal.service;

import com.finsight.sales.DailySales;
import com.finsight.sales.SalesMetricQuery;
import com.finsight.score.ScoreQuery;
import com.finsight.score.ScoreSnapshot;
import com.finsight.score.internal.domain.OperationalScore;
import com.finsight.score.internal.repository.OperationalScoreRepository;
import com.finsight.score.internal.service.ScoreCalculator.ScoreResult;
import com.finsight.visitor.VisitorMetricQuery;
import com.finsight.visitor.VisitorMetricSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService implements ScoreQuery {

    private final SalesMetricQuery salesMetricQuery;
    private final VisitorMetricQuery visitorMetricQuery;
    private final OperationalScoreRepository operationalScoreRepository;
    private final ScoreCalculator scoreCalculator;

    // 매출·방문객 데이터가 바뀐 뒤 이벤트 리스너가 호출한다.
    // 업로드와 다른 트랜잭션에서 돌기 때문에, 실패해도 업로드는 이미 커밋돼 있다.
    // 예외는 삼키지 않고 던져서 이벤트 발행 기록이 미완료로 남게 한다.
    @Transactional
    public void recalculate(Long storeId) {
        List<DailySales> sales = salesMetricQuery.findAllByStoreId(storeId);
        List<VisitorMetricSnapshot> visitors =
                visitorMetricQuery.findAllByStoreId(storeId);

        // 데이터가 아예 없으면 점수 계산 스킵
        if (sales.isEmpty() && visitors.isEmpty()) {
            return;
        }

        ScoreResult r = scoreCalculator.calculate(sales, visitors);

        Optional<OperationalScore> existing =
                operationalScoreRepository.findByStoreId(storeId);

        if (existing.isPresent()) {
            existing.get().update(
                    r.total(), r.grade(), r.riskLevel(),
                    r.visitorStability(), r.salesTrend(),
                    r.salesEfficiency(), r.businessContinuity(),
                    r.volatility());
        } else {
            operationalScoreRepository.save(OperationalScore.create(
                    storeId, r.total(), r.grade(), r.riskLevel(),
                    r.visitorStability(), r.salesTrend(),
                    r.salesEfficiency(), r.businessContinuity(),
                    r.volatility()));
        }

        log.info("운영 점수 재계산: storeId={}, score={}, grade={}",
                storeId, r.total(), r.grade());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ScoreSnapshot> findByStoreId(Long storeId) {
        return operationalScoreRepository.findByStoreId(storeId)
                .map(ScoreService::toSnapshot);
    }

    private static ScoreSnapshot toSnapshot(OperationalScore score) {
        return new ScoreSnapshot(
                score.getStoreId(),
                score.getScore(),
                score.getGrade(),
                score.getRiskLevel(),
                score.getVisitorStabilityScore(),
                score.getSalesTrendScore(),
                score.getSalesEfficiencyScore(),
                score.getBusinessContinuityScore(),
                score.getVolatilityScore()
        );
    }
}
