package com.finsight.ai.client;

import com.finsight.ai.dto.VideoAnalysisResult;
import com.finsight.visitor.domain.CongestionLevel;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class MockVideoAnalysisClient implements VideoAnalysisClient {

    @Override
    public VideoAnalysisResult analyze(String storedFilePath) {
        // 분석 중(PROCESSING) 상태를 프론트가 볼 수 있도록 짧은 지연
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 문서 예시 고정값
        return new VideoAnalysisResult(
                "YOLO",
                "yolo11n",
                4.7,
                11,
                38,
                52.4,
                CongestionLevel.NORMAL
        );
    }
}