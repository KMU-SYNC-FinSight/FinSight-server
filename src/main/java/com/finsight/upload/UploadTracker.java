package com.finsight.upload;

/**
 * 업로드 이력을 기록하고 처리 상태를 바꾸는 공개 API.
 * 매출 CSV 업로드(sales)와 영상 분석(visitor)이 쓴다.
 *
 * <p>모든 메서드는 트랜잭션 안에서 실행된다. 호출한 쪽에 트랜잭션이 없으면 메서드가 끝날 때 바로 커밋되고,
 * 있으면 그 트랜잭션에 함께 묶인다.
 */
public interface UploadTracker {

    /**
     * 저장과 처리가 함께 끝난 CSV 업로드를 COMPLETED 상태로 기록하고 업로드 ID를 돌려준다.
     */
    Long recordCompletedCsvUpload(
            Long storeId,
            UploadType uploadType,
            String originalFileName,
            String storedFilePath
    );

    void markProcessing(Long uploadId);

    void markCompleted(Long uploadId);

    void markFailed(Long uploadId, String errorMessage);
}
