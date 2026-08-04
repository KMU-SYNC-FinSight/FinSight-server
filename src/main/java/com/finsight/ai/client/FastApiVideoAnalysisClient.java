package com.finsight.ai.client;

import com.finsight.ai.dto.AnalyzePollResponse;
import com.finsight.ai.dto.AnalyzeSubmitResponse;
import com.finsight.ai.dto.VideoAnalysisResult;
import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;

@Slf4j
@Primary
@Component
public class FastApiVideoAnalysisClient implements VideoAnalysisClient {

    private final RestClient restClient;
    private final String aiServerUrl;

    // 폴링 설정
    private static final long POLL_INTERVAL_MS = 3000;   // 3초마다 폴링
    private static final int MAX_POLL_ATTEMPTS = 200;    // 최대 200회 (약 10분)

    public FastApiVideoAnalysisClient(
            RestClient restClient,
            @Value("${ai.server.url}") String aiServerUrl
    ) {
        this.restClient = restClient;
        this.aiServerUrl = aiServerUrl;
    }

    @Override
    public VideoAnalysisResult analyze(String storedFilePath) {
        // 1. 영상 파일을 multipart로 제출 → jobId 받기
        String jobId = submit(storedFilePath);
        log.info("AI 분석 제출 완료: jobId={}", jobId);

        // 2. jobId로 폴링 → DONE 될 때까지
        AnalyzePollResponse result = poll(jobId);
        log.info("AI 분석 완료: jobId={}", jobId);

        // 3. 우리 DTO로 변환해서 반환
        return new VideoAnalysisResult(
                result.modelName(),
                result.modelVersion(),
                result.averageOccupancy(),
                result.peakOccupancy(),
                result.trackedObjectCount(),
                result.averageDwellSeconds(),
                result.congestionLevel()
        );
    }

    // ── 제출 ──
    private String submit(String storedFilePath) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(Path.of(storedFilePath)));

        AnalyzeSubmitResponse response = restClient.post()
                .uri(aiServerUrl + "/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(AnalyzeSubmitResponse.class);

        if (response == null || response.jobId() == null) {
            throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
        }
        return response.jobId();
    }

    // ── 폴링 ──
    private AnalyzePollResponse poll(String jobId) {
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            AnalyzePollResponse response = restClient.get()
                    .uri(aiServerUrl + "/analyze/" + jobId)
                    .retrieve()
                    .body(AnalyzePollResponse.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
            }

            String status = response.status();

            if ("DONE".equals(status)) {
                return response;                    // 완료 → 결과 반환
            }
            if ("FAILED".equals(status)) {
                throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
            }
            // PENDING / PROCESSING → 계속 폴링
            sleep(POLL_INTERVAL_MS);
        }
        // 최대 시도 초과 → 타임아웃
        throw new BusinessException(ErrorCode.AI_ANALYSIS_TIMEOUT);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }
}