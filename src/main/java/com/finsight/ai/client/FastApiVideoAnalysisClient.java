package com.finsight.ai.client;

import com.finsight.ai.dto.AnalyzePollResponse;
import com.finsight.ai.dto.AnalyzeSubmitResponse;
import com.finsight.ai.dto.VideoAnalysisResult;
import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.global.storage.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Primary
@Component
public class FastApiVideoAnalysisClient implements VideoAnalysisClient {

    private final RestClient restClient;
    private final FileStorage fileStorage;          // ← 추가
    private final String aiServerUrl;

    private static final long POLL_INTERVAL_MS = 3000;
    private static final int MAX_POLL_ATTEMPTS = 200;

    public FastApiVideoAnalysisClient(
            RestClient restClient,
            FileStorage fileStorage,                    // ← 추가
            @Value("${ai.server.url}") String aiServerUrl
    ) {
        this.restClient = restClient;
        this.fileStorage = fileStorage;
        this.aiServerUrl = aiServerUrl;
    }

    @Override
    public VideoAnalysisResult analyze(String storedFilePath) {
        Path tempFile = null;
        boolean isTemp = false;
        try {
            // S3면 임시 다운로드, 로컬이면 그냥 그 경로
            tempFile = fileStorage.downloadToTemp(storedFilePath);
            isTemp = tempFile.toString().contains("ai-analysis-");  // 임시파일 여부

            String jobId = submit(tempFile);
            log.info("AI 분석 제출 완료: jobId={}", jobId);

            AnalyzePollResponse result = poll(jobId);
            log.info("AI 분석 완료: jobId={}", jobId);

            return new VideoAnalysisResult(
                    result.modelName(),
                    result.modelVersion(),
                    result.averageOccupancy(),
                    result.peakOccupancy(),
                    result.trackedObjectCount(),
                    result.averageDwellSeconds(),
                    result.congestionLevel()
            );
        } finally {
            // 임시 파일이면 삭제 (S3에서 받아온 경우)
            if (isTemp && tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("임시 파일 삭제 실패: {}", tempFile);
                }
            }
        }
    }

    private String submit(Path filePath) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(filePath));

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
                return response;
            }
            if ("FAILED".equals(status)) {
                throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
            }
            sleep(POLL_INTERVAL_MS);
        }
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