package com.finsight.ai.dto;

// POST /analyze 응답: {"jobId":"uuid", "status":"PENDING"}
public record AnalyzeSubmitResponse(
        String jobId,
        String status
) {
}