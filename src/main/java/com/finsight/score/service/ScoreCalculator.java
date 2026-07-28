package com.finsight.score.service;

import com.finsight.sales.domain.SalesMetric;
import com.finsight.score.domain.Grade;
import com.finsight.score.domain.RiskLevel;
import com.finsight.visitor.domain.VisitorMetric;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScoreCalculator {

    // 배점 (문서 11번)
    private static final double MAX_VISITOR_STABILITY = 25.0;
    private static final double MAX_SALES_TREND = 25.0;
    private static final double MAX_SALES_EFFICIENCY = 20.0;
    private static final double MAX_BUSINESS_CONTINUITY = 15.0;
    private static final double MAX_VOLATILITY = 15.0;

    // 데이터 부족 시 중립점 비율 (만점의 60%)
    private static final double NEUTRAL_RATIO = 0.6;

    // 방문객당 매출 "만점" 기준선 (원). 조정 가능한 프로토타입 상수.
    private static final double EFFICIENCY_TARGET_PER_VISITOR = 20000.0;

    // 영업 지속성 만점 기준 일수
    private static final int CONTINUITY_TARGET_DAYS = 14;

    // 계산 결과 묶음
    public record ScoreResult(
            double total,
            Grade grade,
            RiskLevel riskLevel,
            double visitorStability,
            double salesTrend,
            double salesEfficiency,
            double businessContinuity,
            double volatility
    ) {}

    public ScoreResult calculate(
            List<SalesMetric> sales,
            List<VisitorMetric> visitors
    ) {
        double visitorStability = calcVisitorStability(visitors);
        double salesTrend = calcSalesTrend(sales);
        double salesEfficiency = calcSalesEfficiency(sales, visitors);
        double businessContinuity = calcBusinessContinuity(sales);
        double volatility = calcVolatility(sales);

        double total = round1(
                visitorStability + salesTrend + salesEfficiency
                        + businessContinuity + volatility
        );

        Grade grade = toGrade(total);
        RiskLevel riskLevel = toRiskLevel(grade);

        return new ScoreResult(
                total, grade, riskLevel,
                round1(visitorStability), round1(salesTrend),
                round1(salesEfficiency), round1(businessContinuity),
                round1(volatility)
        );
    }

    // ① 방문 수요 안정성: averageOccupancy의 변동계수 기반
    private double calcVisitorStability(List<VisitorMetric> visitors) {
        List<Double> values = visitors.stream()
                .map(VisitorMetric::getAverageOccupancy)
                .filter(v -> v != null)
                .toList();

        if (values.size() < 2) {
            return MAX_VISITOR_STABILITY * NEUTRAL_RATIO; // 데이터 부족 → 중립
        }
        double cv = coefficientOfVariation(values);
        return MAX_VISITOR_STABILITY * stabilityFromCv(cv);
    }

    // ② 매출 추세: 앞 절반 평균 vs 뒤 절반 평균
    private double calcSalesTrend(List<SalesMetric> sales) {
        if (sales.size() < 2) {
            return MAX_SALES_TREND * NEUTRAL_RATIO;
        }
        List<Double> amounts = sales.stream()
                .map(s -> (double) s.getSalesAmount())
                .toList();

        int half = amounts.size() / 2;
        double firstAvg = average(amounts.subList(0, half));
        double secondAvg = average(
                amounts.subList(amounts.size() - half, amounts.size()));

        if (firstAvg <= 0) {
            return MAX_SALES_TREND * NEUTRAL_RATIO;
        }
        double changeRatio = (secondAvg - firstAvg) / firstAvg;
        // -20% 이하 → 0, 0% → 0.6(유지), +20% 이상 → 1.0
        double normalized = clamp(0.6 + changeRatio * 2.0, 0.0, 1.0);
        return MAX_SALES_TREND * normalized;
    }

    // ③ 방문객 대비 매출 효율: 총매출 / 총방문객
    private double calcSalesEfficiency(
            List<SalesMetric> sales, List<VisitorMetric> visitors) {

        double totalSales = sales.stream()
                .mapToDouble(SalesMetric::getSalesAmount).sum();

        int totalVisitors = visitors.stream()
                .map(VisitorMetric::getTrackedObjectCount)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue).sum();

        if (totalVisitors <= 0 || totalSales <= 0) {
            return MAX_SALES_EFFICIENCY * NEUTRAL_RATIO;
        }
        double perVisitor = totalSales / totalVisitors;
        double normalized =
                clamp(perVisitor / EFFICIENCY_TARGET_PER_VISITOR, 0.0, 1.0);
        return MAX_SALES_EFFICIENCY * normalized;
    }

    // ④ 영업 지속성: 매출 데이터가 커버하는 날짜 수
    private double calcBusinessContinuity(List<SalesMetric> sales) {
        long distinctDays = sales.stream()
                .map(SalesMetric::getMetricDate)
                .distinct().count();

        double normalized =
                clamp((double) distinctDays / CONTINUITY_TARGET_DAYS, 0.0, 1.0);
        return MAX_BUSINESS_CONTINUITY * normalized;
    }

    // ⑤ 일별 변동성: 매출 변동계수 (작을수록 높은 점수)
    private double calcVolatility(List<SalesMetric> sales) {
        if (sales.size() < 2) {
            return MAX_VOLATILITY * NEUTRAL_RATIO;
        }
        List<Double> amounts = sales.stream()
                .map(s -> (double) s.getSalesAmount())
                .toList();
        double cv = coefficientOfVariation(amounts);
        return MAX_VOLATILITY * stabilityFromCv(cv);
    }

    // --- 공통 헬퍼 ---

    // 변동계수(CV)를 안정성 점수(0~1)로: CV 0 → 1.0, CV 0.5 이상 → 0
    private double stabilityFromCv(double cv) {
        return clamp(1.0 - cv / 0.5, 0.0, 1.0);
    }

    private double coefficientOfVariation(List<Double> values) {
        double mean = average(values);
        if (mean == 0) return 0;
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        return Math.sqrt(variance) / mean;
    }

    private double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue)
                .average().orElse(0);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private Grade toGrade(double total) {
        if (total >= 85) return Grade.VERY_STABLE;
        if (total >= 70) return Grade.STABLE;
        if (total >= 55) return Grade.CAUTION;
        return Grade.RISK;
    }

    private RiskLevel toRiskLevel(Grade grade) {
        return switch (grade) {
            case VERY_STABLE, STABLE -> RiskLevel.LOW;
            case CAUTION -> RiskLevel.MEDIUM;
            case RISK -> RiskLevel.HIGH;
        };
    }
}