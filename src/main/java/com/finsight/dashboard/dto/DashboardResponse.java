package com.finsight.dashboard.dto;

import com.finsight.visitor.domain.CongestionLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매장 운영 대시보드 응답")
public record DashboardResponse(

        @Schema(description = "매장 ID", example = "1")
        Long storeId,

        @Schema(description = "매장 이름", example = "핀사이트 카페")
        String storeName,

        @Schema(description = "운영 안정성 점수", example = "78.4")
        Double operationScore,

        @Schema(description = "등급", example = "STABLE")
        String grade,

        @Schema(description = "위험도", example = "LOW")
        String riskLevel,

        @Schema(description = "방문객 지표")
        VisitorMetrics visitorMetrics,

        @Schema(description = "매출 지표")
        SalesMetrics salesMetrics
) {

    @Schema(description = "방문객 지표")
    public record VisitorMetrics(
            Double averageOccupancy,
            Integer peakOccupancy,
            Integer trackedObjectCount,
            Double averageDwellSeconds,
            CongestionLevel congestionLevel
    ) {}

    @Schema(description = "매출 지표")
    public record SalesMetrics(
            Long salesAmount,
            Integer transactionCount,
            Double salesPerTrackedObject
    ) {}
}