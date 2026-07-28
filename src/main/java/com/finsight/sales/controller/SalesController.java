package com.finsight.sales.controller;

import com.finsight.global.security.CustomUserPrincipal;
import com.finsight.sales.dto.SalesUploadResponse;
import com.finsight.sales.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Sales", description = "매출 데이터 API")
@RestController
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @Operation(
            summary = "매출 CSV 업로드",
            description = "매출 CSV를 업로드해 파싱·저장합니다. "
                    + "같은 날짜 데이터는 최신 값으로 덮어씁니다. 본인 매장만 가능합니다."
    )
    @PostMapping(
            value = "/api/stores/{storeId}/sales",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<SalesUploadResponse> uploadSalesCsv(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "매장 ID", example = "1")
            @PathVariable Long storeId,
            @RequestParam("file") MultipartFile file
    ) {
        SalesUploadResponse response =
                salesService.uploadSalesCsv(principal.getUserId(), storeId, file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}