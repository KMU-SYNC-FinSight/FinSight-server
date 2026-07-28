package com.finsight.upload.controller;

import com.finsight.global.security.CustomUserPrincipal;
import com.finsight.upload.dto.UploadStatusResponse;
import com.finsight.upload.dto.VideoUploadResponse;
import com.finsight.upload.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Upload", description = "파일 업로드 API")
@RestController
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @Operation(
            summary = "매장 영상 업로드",
            description = "매장 내부 영상을 업로드합니다. 본인 소유 매장만 가능합니다."
    )
    @PostMapping(
            value = "/api/stores/{storeId}/videos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "매장 ID", example = "1")
            @PathVariable Long storeId,
            @RequestParam("file") MultipartFile file
    ) {
        VideoUploadResponse response =
                uploadService.uploadVideo(principal.getUserId(), storeId, file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "업로드 상태 조회",
            description = "업로드 처리 상태를 조회합니다. 본인 매장의 업로드만 조회할 수 있습니다."
    )
    @GetMapping("/api/uploads/{uploadId}")
    public ResponseEntity<UploadStatusResponse> getUploadStatus(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "업로드 ID", example = "1")
            @PathVariable Long uploadId
    ) {
        UploadStatusResponse response =
                uploadService.getUploadStatus(principal.getUserId(), uploadId);

        return ResponseEntity.ok(response);
    }
}