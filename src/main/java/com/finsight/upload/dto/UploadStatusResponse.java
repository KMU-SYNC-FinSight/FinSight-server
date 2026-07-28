package com.finsight.upload.dto;

import com.finsight.upload.domain.DataUpload;
import com.finsight.upload.domain.ProcessingStatus;
import com.finsight.upload.domain.UploadType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업로드 상태 조회 응답")
public record UploadStatusResponse(

        @Schema(description = "업로드 ID", example = "1")
        Long uploadId,

        @Schema(description = "업로드 유형", example = "STORE_VIDEO")
        UploadType uploadType,

        @Schema(description = "처리 상태", example = "COMPLETED")
        ProcessingStatus processingStatus,

        @Schema(description = "원본 파일명", example = "store-video.mp4")
        String originalFileName,

        @Schema(description = "실패 시 오류 메시지", example = "null", nullable = true)
        String errorMessage
) {

    public static UploadStatusResponse from(DataUpload upload) {
        return new UploadStatusResponse(
                upload.getId(),
                upload.getUploadType(),
                upload.getProcessingStatus(),
                upload.getOriginalFileName(),
                upload.getErrorMessage()
        );
    }
}