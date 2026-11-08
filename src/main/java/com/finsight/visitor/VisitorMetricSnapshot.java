package com.finsight.visitor;

import java.time.LocalDate;

/**
 * 영상 1건의 분석 결과로 만든 방문객 지표.
 */
public record VisitorMetricSnapshot(
        Long id,
        Long uploadId,
        LocalDate metricDate,
        Double averageOccupancy,
        Integer peakOccupancy,
        Integer trackedObjectCount,
        Double averageDwellSeconds,
        CongestionLevel congestionLevel
) {
}
