package com.finsight.visitor.internal.service;

import com.finsight.upload.UploadTracker;
import com.finsight.upload.VideoUploadedEvent;
import com.finsight.visitor.internal.client.VideoAnalysisClient;
import com.finsight.visitor.internal.client.dto.VideoAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAnalysisService {

    private final UploadTracker uploadTracker;
    private final VideoAnalysisClient videoAnalysisClient;
    private final VisitorMetricService visitorMetricService;

    /**
     * 영상 업로드가 커밋된 뒤 AI 분석을 시작한다.
     *
     * <p>AI 분석은 최대 몇 분 걸리므로 트랜잭션 없이 analysisExecutor에서 돌리고,
     * 상태 변경과 결과 저장은 각각 짧은 트랜잭션으로 바로 커밋한다.
     * 리스너 전체를 한 트랜잭션으로 감싸는 {@code @ApplicationModuleListener}를 쓰지 않는 이유다.
     */
    @Async("analysisExecutor")
    @TransactionalEventListener
    public void on(VideoUploadedEvent event) {
        Long uploadId = event.uploadId();

        try {
            // 1. PROCESSING → 즉시 커밋 (별도 트랜잭션)
            uploadTracker.markProcessing(uploadId);

            // 2. AI 분석 (트랜잭션 밖에서 오래 걸림)
            VideoAnalysisResult result =
                    videoAnalysisClient.analyze(event.storedFilePath());

            // 3. 결과 저장 + COMPLETED → 한 트랜잭션으로 커밋, 커밋 후 운영 점수 재계산
            visitorMetricService.saveAnalysisResult(event, result);

            log.info("영상 분석 완료: uploadId={}", uploadId);

        } catch (Exception e) {
            log.error("영상 분석 실패: uploadId={}", uploadId, e);
            uploadTracker.markFailed(uploadId, "영상 분석 중 오류가 발생했습니다.");
        }
    }
}
