package com.finsight.visitor.internal.client.dto;

import com.finsight.visitor.CongestionLevel;

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