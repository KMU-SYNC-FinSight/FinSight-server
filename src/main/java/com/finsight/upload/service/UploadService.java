package com.finsight.upload.service;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.global.storage.FileStorage;
import com.finsight.store.domain.Store;
import com.finsight.store.repository.StoreRepository;
import com.finsight.upload.domain.DataUpload;
import com.finsight.upload.dto.UploadStatusResponse;
import com.finsight.upload.dto.VideoUploadResponse;
import com.finsight.upload.repository.DataUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

// 지금은 파일 저장과 DB 기록이 한 트랜잭션 안에 있어 고아 파일이 생길 수 있음 (트랜잭션은 DB 작업만 롤백 해주기 때문에)
@Service
@RequiredArgsConstructor
public class UploadService {

    private static final List<String> ALLOWED_VIDEO_EXTENSIONS =
            List.of("mp4", "mov", "avi", "mkv");

    private final StoreRepository storeRepository;
    private final DataUploadRepository dataUploadRepository;
    private final FileStorage fileStorage;
    private final VideoAnalysisProcessor videoAnalysisProcessor;

    @Value("${file.upload.max-video-size}")
    private long maxVideoSize;

    @Transactional
    public VideoUploadResponse uploadVideo(
            Long userId,
            Long storeId,
            MultipartFile file
    ) {
        // 1. 매장 소유권 확인 (남의 매장/없는 매장이면 STORE_NOT_FOUND)
        Store store = storeRepository.findByIdAndUserId(storeId, userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 2. 파일 검증
        validateVideoFile(file);

        // 3. 저장
        String storedPath = fileStorage.store(file, "videos");

        // 4. 업로드 이력 기록 (상태: UPLOADED)
        DataUpload upload = DataUpload.createVideoUpload(
                store.getId(),
                file.getOriginalFilename(),
                storedPath
        );
        DataUpload saved = dataUploadRepository.save(upload);

        // 비동기 분석 트리거 (여기서 기다리지 않음)
        videoAnalysisProcessor.process(saved.getId());

        return VideoUploadResponse.from(saved);
    }

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }

        if (file.getSize() > maxVideoSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename
                .substring(filename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public UploadStatusResponse getUploadStatus(Long userId, Long uploadId) {
        DataUpload upload = dataUploadRepository.findById(uploadId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.UPLOAD_NOT_FOUND));

        // 소유권 확인: 이 업로드가 속한 매장이 내 것인지
        storeRepository.findByIdAndUserId(upload.getStoreId(), userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.UPLOAD_NOT_FOUND));

        return UploadStatusResponse.from(upload);
    }
}