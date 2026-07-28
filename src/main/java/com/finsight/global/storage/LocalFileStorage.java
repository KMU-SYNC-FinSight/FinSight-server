package com.finsight.global.storage;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "file.storage.type",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalFileStorage implements FileStorage {

    private final Path rootLocation;

    public LocalFileStorage(
            @Value("${file.upload.local-dir}") String localDir
    ) {
        this.rootLocation = Paths.get(localDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "업로드 디렉터리를 생성할 수 없습니다: " + rootLocation, e);
        }
    }

    @Override
    public String store(MultipartFile file, String directory) {
        try {
            Path targetDir = rootLocation.resolve(directory).normalize();
            Files.createDirectories(targetDir);

            String storedName = UUID.randomUUID() + "_"
                    + sanitize(file.getOriginalFilename());
            Path targetPath = targetDir.resolve(storedName).normalize();

            // 디렉터리 탈출 방지 (../ 등)
            if (!targetPath.startsWith(rootLocation)) {
                throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
            }

            file.transferTo(targetPath);

            return targetPath.toString();

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        // 경로 구분자 제거
        return filename.replaceAll("[/\\\\]", "_");
    }
}