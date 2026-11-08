package com.finsight.upload.internal.service;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.upload.UploadTracker;
import com.finsight.upload.UploadType;
import com.finsight.upload.internal.domain.DataUpload;
import com.finsight.upload.internal.repository.DataUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UploadTrackingService implements UploadTracker {

    private final DataUploadRepository dataUploadRepository;

    @Override
    @Transactional
    public Long recordCompletedCsvUpload(
            Long storeId,
            UploadType uploadType,
            String originalFileName,
            String storedFilePath
    ) {
        DataUpload upload = DataUpload.createCsvUpload(
                storeId,
                uploadType,
                originalFileName,
                storedFilePath
        );
        return dataUploadRepository.save(upload).getId();
    }

    // PROCESSING으로 변경
    @Override
    @Transactional
    public void markProcessing(Long uploadId) {
        findUpload(uploadId).markProcessing();
    }

    // COMPLETED로 변경
    @Override
    @Transactional
    public void markCompleted(Long uploadId) {
        findUpload(uploadId).markCompleted();
    }

    // FAILED로 변경
    @Override
    @Transactional
    public void markFailed(Long uploadId, String errorMessage) {
        findUpload(uploadId).markFailed(errorMessage);
    }

    private DataUpload findUpload(Long uploadId) {
        return dataUploadRepository.findById(uploadId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.UPLOAD_NOT_FOUND));
    }
}
