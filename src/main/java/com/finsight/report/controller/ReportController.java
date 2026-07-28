package com.finsight.report.controller;

import com.finsight.global.security.CustomUserPrincipal;
import com.finsight.report.dto.ReportResponse;
import com.finsight.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report", description = "금융기관용 리포트 API")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "금융기관용 리포트 조회",
            description = "운영 안정성 보조지표 리포트를 조회합니다. "
                    + "신용등급이나 대출 판단이 아닌 참고용 지표입니다."
    )
    @GetMapping("/api/stores/{storeId}/report")
    public ResponseEntity<ReportResponse> getReport(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "매장 ID", example = "1")
            @PathVariable Long storeId
    ) {
        ReportResponse response =
                reportService.getReport(principal.getUserId(), storeId);
        return ResponseEntity.ok(response);
    }
}