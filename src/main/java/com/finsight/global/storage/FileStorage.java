package com.finsight.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    /**
     * 파일을 저장하고, 저장 위치를 식별하는 경로(또는 URL)를 반환한다.
     *
     * // @param file 업로드된 파일
     * // @param directory 저장 하위 디렉터리 (예: "videos")
     * // @return 저장된 파일의 경로/식별자
     */
    String store(MultipartFile file, String directory);
}