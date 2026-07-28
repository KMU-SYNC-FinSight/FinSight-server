package com.finsight.ai.dto;

import com.finsight.visitor.domain.CongestionLevel;

public record VideoAnalysisResult(
        String modelName,
        String modelVersion,
        double averageOccupancy,
        int peakOccupancy,
        int trackedObjectCount,
        double averageDwellSeconds,
        CongestionLevel congestionLevel
) {
}