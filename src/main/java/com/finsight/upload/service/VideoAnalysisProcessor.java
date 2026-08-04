package com.finsight.upload.service;

import com.finsight.ai.client.VideoAnalysisClient;
import com.finsight.ai.dto.VideoAnalysisResult;
import com.finsight.score.service.ScoreService;
import com.finsight.upload.domain.DataUpload;
import com.finsight.upload.repository.DataUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoAnalysisProcessor {

    private final DataUploadRepository dataUploadRepository;
    private final UploadStatusService uploadStatusService;   // ← 추가
    private final VideoAnalysisClient videoAnalysisClient;
    private final ScoreService scoreService;

    @Async("analysisExecutor")
    public void process(Long uploadId) {
        // 존재 확인 (트랜잭션 없이 단순 조회)
        DataUpload upload = dataUploadRepository.findById(uploadId).orElse(null);
        if (upload == null) {
            log.warn("분석 대상 업로드 없음: uploadId={}", uploadId);
            return;
        }
        String storedFilePath = upload.getStoredFilePath();
        Long storeId = upload.getStoreId();

        try {
            // 1. PROCESSING → 즉시 커밋 (별도 트랜잭션)
            uploadStatusService.markProcessing(uploadId);

            // 2. AI 분석 (트랜잭션 밖에서 오래 걸림)
            VideoAnalysisResult result =
                    videoAnalysisClient.analyze(storedFilePath);

            // 3. 결과 저장 + COMPLETED → 즉시 커밋 (별도 트랜잭션)
            uploadStatusService.saveResultAndComplete(uploadId, result);

            // 4. 운영 점수 재계산
            scoreService.recalculate(storeId);

            log.info("영상 분석 완료: uploadId={}", uploadId);

        } catch (Exception e) {
            log.error("영상 분석 실패: uploadId={}", uploadId, e);
            uploadStatusService.markFailed(uploadId, "영상 분석 중 오류가 발생했습니다.");
        }
    }
}