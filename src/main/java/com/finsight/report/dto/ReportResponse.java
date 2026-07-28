package com.finsight.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "금융기관용 리포트 응답")
public record ReportResponse(

        @Schema(description = "매장 ID", example = "1")
        Long storeId,

        @Schema(description = "운영 안정성 점수", example = "78.4")
        Double operationScore,

        @Schema(description = "등급", example = "STABLE")
        String grade,

        @Schema(description = "위험도", example = "LOW")
        String riskLevel,

        @Schema(description = "요약", example = "최근 방문 수요와 매출 흐름이 안정적으로 유지되고 있습니다.")
        String summary,

        @Schema(description = "근거 목록")
        List<String> evidence,

        @Schema(description = "이용 안내")
        String usageNotice
) {
}