package com.finsight.upload;

import java.time.LocalDateTime;

/**
 * 매장 영상 업로드가 저장된 뒤 발행되는 이벤트.
 * 트랜잭션이 커밋되면 visitor 모듈이 받아 AI 영상 분석을 시작한다.
 */
public record VideoUploadedEvent(
        Long uploadId,
        Long storeId,
        String storedFilePath,
        LocalDateTime recordedAt
) {
}
