package com.finsight.visitor.domain;

import com.finsight.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "visitor_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitorMetric extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "data_upload_id")
    private Long dataUploadId;

    @Column(name = "metric_date")
    private LocalDate metricDate;

    @Column(name = "average_occupancy")
    private Double averageOccupancy;

    @Column(name = "peak_occupancy")
    private Integer peakOccupancy;

    @Column(name = "tracked_object_count")
    private Integer trackedObjectCount;

    @Column(name = "average_dwell_seconds")
    private Double averageDwellSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "congestion_level", length = 20)
    private CongestionLevel congestionLevel;

    public static VisitorMetric create(
            Long storeId,
            Long dataUploadId,
            LocalDate metricDate,
            Double averageOccupancy,
            Integer peakOccupancy,
            Integer trackedObjectCount,
            Double averageDwellSeconds,
            CongestionLevel congestionLevel
    ) {
        VisitorMetric metric = new VisitorMetric();
        metric.storeId = storeId;
        metric.dataUploadId = dataUploadId;
        metric.metricDate = metricDate;
        metric.averageOccupancy = averageOccupancy;
        metric.peakOccupancy = peakOccupancy;
        metric.trackedObjectCount = trackedObjectCount;
        metric.averageDwellSeconds = averageDwellSeconds;
        metric.congestionLevel = congestionLevel;
        return metric;
    }
}