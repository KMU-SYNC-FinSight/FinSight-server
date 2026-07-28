package com.finsight.upload.domain;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "data_uploads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DataUpload extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_type", nullable = false, length = 20)
    private UploadType uploadType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "stored_file_path", length = 500)
    private String storedFilePath;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static DataUpload createVideoUpload(
            Long storeId,
            String originalFileName,
            String storedFilePath
    ) {
        DataUpload upload = new DataUpload();
        upload.storeId = storeId;
        upload.uploadType = UploadType.STORE_VIDEO;
        upload.processingStatus = ProcessingStatus.UPLOADED;
        upload.originalFileName = originalFileName;
        upload.storedFilePath = storedFilePath;
        upload.errorMessage = null;
        upload.completedAt = null;
        return upload;
    }

    public static DataUpload createCsvUpload(
            Long storeId,
            UploadType uploadType,
            String originalFileName,
            String storedFilePath
    ) {
        DataUpload upload = new DataUpload();
        upload.storeId = storeId;
        upload.uploadType = uploadType;
        upload.processingStatus = ProcessingStatus.COMPLETED;
        upload.originalFileName = originalFileName;
        upload.storedFilePath = storedFilePath;
        upload.errorMessage = null;
        upload.completedAt = java.time.LocalDateTime.now();
        return upload;
    }

    // 다음 챕터(AI 연동)에서 상태 전이에 사용
    public void markProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
    }

    public void markCompleted() {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}