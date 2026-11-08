package com.finsight.visitor.internal.client.dto;

import com.finsight.visitor.CongestionLevel;

// GET /analyze/{jobId} 응답
// PROCESSING이면 결과 필드들은 null, DONE이면 다 채워짐
public record AnalyzePollResponse(
        String status,              // PENDING / PROCESSING / DONE / FAILED
        String modelName,
        String modelVersion,
        Double averageOccupancy,
        Integer peakOccupancy,
        Integer trackedObjectCount,
        Double averageDwellSeconds,
        CongestionLevel congestionLevel
) {
}