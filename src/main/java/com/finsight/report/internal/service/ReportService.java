package com.finsight.report.internal.service;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.report.internal.dto.ReportResponse;
import com.finsight.score.Grade;
import com.finsight.score.ScoreQuery;
import com.finsight.score.ScoreSnapshot;
import com.finsight.store.StoreQuery;
import com.finsight.store.StoreSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    // 문서 12번: 반드시 포함하는 이용 안내 (신용등급/대출 판단 아님을 명시)
    private static final String USAGE_NOTICE =
            "본 결과는 대출 승인이나 신용등급을 결정하는 정보가 아닌 "
                    + "SCB 보조 운영지표입니다.";

    private final StoreQuery storeQuery;
    private final ScoreQuery scoreQuery;

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long userId, Long storeId) {
        StoreSummary store = storeQuery.getOwnedStore(userId, storeId);

        ScoreSnapshot score = scoreQuery.findByStoreId(storeId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.SCORE_NOT_READY));

        return new ReportResponse(
                store.storeId(),
                score.score(),
                score.grade().name(),
                score.riskLevel().name(),
                buildSummary(score.grade()),
                buildEvidence(score),
                USAGE_NOTICE
        );
    }

    private String buildSummary(Grade grade) {
        return switch (grade) {
            case VERY_STABLE ->
                    "방문 수요와 매출 흐름이 매우 안정적으로 유지되고 있습니다.";
            case STABLE ->
                    "최근 방문 수요와 매출 흐름이 안정적으로 유지되고 있습니다.";
            case CAUTION ->
                    "일부 지표에서 변동이 관찰되어 지속적인 관찰이 필요합니다.";
            case RISK ->
                    "방문 수요 또는 매출 흐름에서 주의가 필요한 신호가 관찰됩니다.";
        };
    }

    private List<String> buildEvidence(ScoreSnapshot s) {
        List<String> evidence = new ArrayList<>();

        // 항목별 점수를 근거 문장으로 (설명 가능성 확보)
        if (s.salesTrendScore() != null && s.salesTrendScore() >= 15) {
            evidence.add("매출 흐름이 안정적이거나 성장세를 보이고 있습니다.");
        } else if (s.salesTrendScore() != null) {
            evidence.add("매출 흐름에 다소 변동이 관찰됩니다.");
        }

        if (s.salesEfficiencyScore() != null
                && s.salesEfficiencyScore() >= 12) {
            evidence.add("방문 수요 대비 매출 효율이 양호합니다.");
        }

        if (s.volatilityScore() != null && s.volatilityScore() >= 9) {
            evidence.add("일별 매출 변동이 안정적인 범위에서 유지되고 있습니다.");
        }

        if (s.businessContinuityScore() != null
                && s.businessContinuityScore() >= 9) {
            evidence.add("영업 데이터가 꾸준히 축적되어 운영 지속성이 확인됩니다.");
        }

        evidence.add(
                "온라인 플랫폼 데이터가 부족하더라도 실제 매장 운영 상태를 "
                        + "현장 데이터로 확인할 수 있습니다.");

        return evidence;
    }
}