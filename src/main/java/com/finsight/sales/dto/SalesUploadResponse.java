package com.finsight.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매출 CSV 업로드 응답")
public record SalesUploadResponse(

        @Schema(description = "업로드 ID", example = "2")
        Long uploadId,

        @Schema(description = "처리된 전체 행 수", example = "3")
        int totalRows,

        @Schema(description = "새로 저장된 행 수", example = "2")
        int insertedRows,

        @Schema(description = "덮어쓴 행 수", example = "1")
        int updatedRows,

        @Schema(description = "건너뛴(오류) 행 수", example = "0")
        int skippedRows
) {
}