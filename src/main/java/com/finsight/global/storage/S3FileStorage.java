package com.finsight.global.storage;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;   // ← 추가
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;                                          // ← 추가
import java.nio.file.Path;                                           // ← 추가
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "file.storage.type", havingValue = "s3")
public class S3FileStorage implements FileStorage {

    private final String bucket;
    private final S3Client s3Client;

    public S3FileStorage(
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region
    ) {
        this.bucket = bucket;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @PostConstruct
    void log() {
        System.out.println("[Storage] S3FileStorage 활성화 (bucket=" + bucket + ")");
    }

    @Override
    public String store(MultipartFile file, String directory) {
        String key = directory + "/" + UUID.randomUUID()
                + "_" + sanitize(file.getOriginalFilename());

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request,
                    RequestBody.fromInputStream(
                            file.getInputStream(), file.getSize()));

            return key;

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Override
    public Path downloadToTemp(String storedFilePath) {
        try {
            String suffix = storedFilePath.contains(".")
                    ? storedFilePath.substring(storedFilePath.lastIndexOf('.'))
                    : "";
            Path tempFile = Files.createTempFile("ai-analysis-", suffix);
            Files.delete(tempFile);   // ← 추가: 빈 파일 삭제 (경로만 남김)

            s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(storedFilePath)
                            .build(),
                    tempFile
            );

            return tempFile;

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        return filename.replaceAll("[/\\\\]", "_");
    }
}