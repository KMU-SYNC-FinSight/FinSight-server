package com.finsight.score.domain;

import com.finsight.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "operational_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperationalScore extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Grade grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    // 항목별 점수 (문서 11번 5개 항목)
    @Column(name = "visitor_stability_score")
    private Double visitorStabilityScore;

    @Column(name = "sales_trend_score")
    private Double salesTrendScore;

    @Column(name = "sales_efficiency_score")
    private Double salesEfficiencyScore;

    @Column(name = "business_continuity_score")
    private Double businessContinuityScore;

    @Column(name = "volatility_score")
    private Double volatilityScore;

    public static OperationalScore create(
            Long storeId,
            double score,
            Grade grade,
            RiskLevel riskLevel,
            double visitorStabilityScore,
            double salesTrendScore,
            double salesEfficiencyScore,
            double businessContinuityScore,
            double volatilityScore
    ) {
        OperationalScore s = new OperationalScore();
        s.storeId = storeId;
        s.score = score;
        s.grade = grade;
        s.riskLevel = riskLevel;
        s.visitorStabilityScore = visitorStabilityScore;
        s.salesTrendScore = salesTrendScore;
        s.salesEfficiencyScore = salesEfficiencyScore;
        s.businessContinuityScore = businessContinuityScore;
        s.volatilityScore = volatilityScore;
        return s;
    }

    public void update(
            double score,
            Grade grade,
            RiskLevel riskLevel,
            double visitorStabilityScore,
            double salesTrendScore,
            double salesEfficiencyScore,
            double businessContinuityScore,
            double volatilityScore
    ) {
        this.score = score;
        this.grade = grade;
        this.riskLevel = riskLevel;
        this.visitorStabilityScore = visitorStabilityScore;
        this.salesTrendScore = salesTrendScore;
        this.salesEfficiencyScore = salesEfficiencyScore;
        this.businessContinuityScore = businessContinuityScore;
        this.volatilityScore = volatilityScore;
    }
}