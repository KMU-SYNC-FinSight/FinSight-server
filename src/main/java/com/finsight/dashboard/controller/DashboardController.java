package com.finsight.dashboard.controller;

import com.finsight.dashboard.dto.DashboardResponse;
import com.finsight.dashboard.service.DashboardService;
import com.finsight.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "운영 대시보드 API")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "매장 운영 대시보드 조회",
            description = "매장의 운영 점수, 방문객·매출 지표를 종합 조회합니다."
    )
    @GetMapping("/api/stores/{storeId}/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "매장 ID", example = "1")
            @PathVariable Long storeId
    ) {
        DashboardResponse response =
                dashboardService.getDashboard(principal.getUserId(), storeId);
        return ResponseEntity.ok(response);
    }
}