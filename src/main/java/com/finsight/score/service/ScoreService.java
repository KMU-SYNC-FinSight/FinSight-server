package com.finsight.score.service;

import com.finsight.sales.domain.SalesMetric;
import com.finsight.sales.repository.SalesMetricRepository;
import com.finsight.score.domain.OperationalScore;
import com.finsight.score.repository.OperationalScoreRepository;
import com.finsight.score.service.ScoreCalculator.ScoreResult;
import com.finsight.visitor.domain.VisitorMetric;
import com.finsight.visitor.repository.VisitorMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    private final SalesMetricRepository salesMetricRepository;
    private final VisitorMetricRepository visitorMetricRepository;
    private final OperationalScoreRepository operationalScoreRepository;
    private final ScoreCalculator scoreCalculator;

    // 데이터 업로드 후 호출됨. 실패해도 업로드 자체는 살아야 하므로 예외를 삼킨다.
    @Transactional
    public void recalculate(Long storeId) {
        try {
            List<SalesMetric> sales =
                    salesMetricRepository.findByStoreIdOrderByMetricDateAsc(storeId);
            List<VisitorMetric> visitors =
                    visitorMetricRepository.findByStoreId(storeId);

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

        } catch (Exception e) {
            // 점수 계산 실패가 업로드 흐름을 깨지 않도록 방어
            log.error("운영 점수 재계산 실패: storeId={}", storeId, e);
        }
    }
}