package com.finsight.score;

/**
 * 매장의 현재 운영 점수와 항목별 점수.
 */
public record ScoreSnapshot(
        Long storeId,
        Double score,
        Grade grade,
        RiskLevel riskLevel,
        Double visitorStabilityScore,
        Double salesTrendScore,
        Double salesEfficiencyScore,
        Double businessContinuityScore,
        Double volatilityScore
) {
}
