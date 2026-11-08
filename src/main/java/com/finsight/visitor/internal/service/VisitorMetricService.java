package com.finsight.visitor.internal.service;

import com.finsight.upload.UploadTracker;
import com.finsight.upload.VideoUploadedEvent;
import com.finsight.visitor.VisitorDataChangedEvent;
import com.finsight.visitor.VisitorMetricQuery;
import com.finsight.visitor.VisitorMetricSnapshot;
import com.finsight.visitor.internal.client.dto.VideoAnalysisResult;
import com.finsight.visitor.internal.domain.VisitorMetric;
import com.finsight.visitor.internal.repository.VisitorMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitorMetricService implements VisitorMetricQuery {

    private final VisitorMetricRepository visitorMetricRepository;
    private final UploadTracker uploadTracker;
    private final ApplicationEventPublisher eventPublisher;

    // 분석 결과 저장 + 업로드 COMPLETED를 한 트랜잭션으로 커밋한다
    @Transactional
    public void saveAnalysisResult(
            VideoUploadedEvent upload,
            VideoAnalysisResult result
    ) {
        VisitorMetric metric = VisitorMetric.create(
                upload.storeId(),
                upload.uploadId(),
                upload.recordedAt().toLocalDate(),
                result.averageOccupancy(),
                result.peakOccupancy(),
                result.trackedObjectCount(),
                result.averageDwellSeconds(),
                result.congestionLevel()
        );
        visitorMetricRepository.save(metric);

        uploadTracker.markCompleted(upload.uploadId());

        // 커밋 후 score 모듈이 운영 점수를 다시 계산한다
        eventPublisher.publishEvent(new VisitorDataChangedEvent(upload.storeId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorMetricSnapshot> findAllByStoreId(Long storeId) {
        return visitorMetricRepository.findByStoreIdOrderByIdAsc(storeId)
                .stream()
                .map(VisitorMetricService::toSnapshot)
                .toList();
    }

    private static VisitorMetricSnapshot toSnapshot(VisitorMetric metric) {
        return new VisitorMetricSnapshot(
                metric.getId(),
                metric.getDataUploadId(),
                metric.getMetricDate(),
                metric.getAverageOccupancy(),
                metric.getPeakOccupancy(),
                metric.getTrackedObjectCount(),
                metric.getAverageDwellSeconds(),
                metric.getCongestionLevel()
        );
    }
}
