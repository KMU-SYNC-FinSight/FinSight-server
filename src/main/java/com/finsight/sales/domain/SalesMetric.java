package com.finsight.sales.domain;

import com.finsight.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "sales_metrics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sales_store_date",
                        columnNames = {"store_id", "metric_date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesMetric extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "data_upload_id")
    private Long dataUploadId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "sales_amount", nullable = false)
    private Long salesAmount;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    public static SalesMetric create(
            Long storeId,
            Long dataUploadId,
            LocalDate metricDate,
            Long salesAmount,
            Integer transactionCount
    ) {
        SalesMetric metric = new SalesMetric();
        metric.storeId = storeId;
        metric.dataUploadId = dataUploadId;
        metric.metricDate = metricDate;
        metric.salesAmount = salesAmount;
        metric.transactionCount = transactionCount;
        return metric;
    }

    // 덮어쓰기(upsert)에 사용: 같은 날짜 데이터를 새 값으로 갱신
    public void update(
            Long dataUploadId,
            Long salesAmount,
            Integer transactionCount
    ) {
        this.dataUploadId = dataUploadId;
        this.salesAmount = salesAmount;
        this.transactionCount = transactionCount;
    }
}