package com.finsight.global.storage;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorage {

    String store(MultipartFile file, String directory);

    // 저장된 파일을 로컬 임시 경로로 가져온다. AI 전송용.
    Path downloadToTemp(String storedFilePath);   // ← 추가
}