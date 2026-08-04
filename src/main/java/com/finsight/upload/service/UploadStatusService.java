package com.finsight.upload.service;

import com.finsight.ai.dto.VideoAnalysisResult;
import com.finsight.upload.domain.DataUpload;
import com.finsight.upload.repository.DataUploadRepository;
import com.finsight.visitor.domain.VisitorMetric;
import com.finsight.visitor.repository.VisitorMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UploadStatusService {

    private final DataUploadRepository dataUploadRepository;
    private final VisitorMetricRepository visitorMetricRepository;

    // PROCESSING으로 변경
    @Transactional
    public void markProcessing(Long uploadId) {
        DataUpload upload = dataUploadRepository.findById(uploadId)
                .orElseThrow();
        upload.markProcessing();
        // @Transactional 메서드가 끝나면 자동 커밋됨
    }

    // 결과 저장 + COMPLETED
    @Transactional
    public void saveResultAndComplete(Long uploadId, VideoAnalysisResult result) {
        DataUpload upload = dataUploadRepository.findById(uploadId)
                .orElseThrow();

        VisitorMetric metric = VisitorMetric.create(
                upload.getStoreId(),
                upload.getId(),
                upload.getRecordedAt().toLocalDate(),
                result.averageOccupancy(),
                result.peakOccupancy(),
                result.trackedObjectCount(),
                result.averageDwellSeconds(),
                result.congestionLevel()
        );
        visitorMetricRepository.save(metric);

        upload.markCompleted();
    }

    // FAILED로 변경
    @Transactional
    public void markFailed(Long uploadId, String message) {
        DataUpload upload = dataUploadRepository.findById(uploadId)
                .orElseThrow();
        upload.markFailed(message);
    }
}