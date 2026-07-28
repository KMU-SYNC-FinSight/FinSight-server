package com.finsight.upload.service;

import com.finsight.ai.client.VideoAnalysisClient;
import com.finsight.ai.dto.VideoAnalysisResult;
import com.finsight.score.service.ScoreService;
import com.finsight.upload.domain.DataUpload;
import com.finsight.upload.repository.DataUploadRepository;
import com.finsight.visitor.domain.VisitorMetric;
import com.finsight.visitor.repository.VisitorMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoAnalysisProcessor {

    private final DataUploadRepository dataUploadRepository;
    private final VisitorMetricRepository visitorMetricRepository;
    private final VideoAnalysisClient videoAnalysisClient;
    private final ScoreService scoreService;  // 필드 추가

    @Async("analysisExecutor")
    @Transactional
    public void process(Long uploadId) {
        DataUpload upload = dataUploadRepository.findById(uploadId)
                .orElse(null);
        if (upload == null) {
            log.warn("분석 대상 업로드 없음: uploadId={}", uploadId);
            return;
        }

        try {
            // 1. PROCESSING 전환
            upload.markProcessing();
            dataUploadRepository.saveAndFlush(upload);

            // 2. AI 분석 (Mock, 지연 포함)
            VideoAnalysisResult result =
                    videoAnalysisClient.analyze(upload.getStoredFilePath());

            // 3. visitor_metrics 저장
            VisitorMetric metric = VisitorMetric.create(
                    upload.getStoreId(),
                    upload.getId(),
                    LocalDate.now(),
                    result.averageOccupancy(),
                    result.peakOccupancy(),
                    result.trackedObjectCount(),
                    result.averageDwellSeconds(),
                    result.congestionLevel()
            );
            visitorMetricRepository.save(metric);

            // 4. COMPLETED 전환
            upload.markCompleted();
            dataUploadRepository.save(upload);

            // 5. 운영 점수 재계산
            scoreService.recalculate(upload.getStoreId());

            log.info("영상 분석 완료: uploadId={}", uploadId);

        } catch (Exception e) {
            log.error("영상 분석 실패: uploadId={}", uploadId, e);
            upload.markFailed("영상 분석 중 오류가 발생했습니다.");
            dataUploadRepository.save(upload);
        }
    }
}