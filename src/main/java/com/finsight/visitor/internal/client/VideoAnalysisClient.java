package com.finsight.visitor.internal.client;

import com.finsight.visitor.internal.client.dto.VideoAnalysisResult;

public interface VideoAnalysisClient {

    /**
     * 저장된 영상 경로를 받아 분석 결과를 반환한다.
     * 지금은 Mock, 나중에 FastAPI 호출 구현으로 교체.
     */
    VideoAnalysisResult analyze(String storedFilePath);
}