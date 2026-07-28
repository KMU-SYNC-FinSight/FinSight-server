package com.finsight.upload.dto;

import com.finsight.upload.domain.DataUpload;
import com.finsight.upload.domain.ProcessingStatus;
import com.finsight.upload.domain.UploadType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "영상 업로드 응답")
public record VideoUploadResponse(

        @Schema(description = "업로드 ID", example = "1")
        Long uploadId,

        @Schema(description = "업로드 유형", example = "STORE_VIDEO")
        UploadType uploadType,

        @Schema(description = "처리 상태", example = "UPLOADED")
        ProcessingStatus processingStatus,

        @Schema(description = "원본 파일명", example = "store-video.mp4")
        String originalFileName
) {

    public static VideoUploadResponse from(DataUpload upload) {
        return new VideoUploadResponse(
                upload.getId(),
                upload.getUploadType(),
                upload.getProcessingStatus(),
                upload.getOriginalFileName()
        );
    }
}