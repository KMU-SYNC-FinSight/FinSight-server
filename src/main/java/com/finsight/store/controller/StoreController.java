package com.finsight.store.controller;

import com.finsight.global.security.CustomUserPrincipal;
import com.finsight.store.dto.StoreCreateRequest;
import com.finsight.store.dto.StoreResponse;
import com.finsight.store.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Store", description = "매장 관리 API")
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @Operation(
            summary = "매장 등록",
            description = "로그인한 사용자의 매장을 등록합니다. 소유자는 토큰에서 추출됩니다."
    )
    @PostMapping
    public ResponseEntity<StoreResponse> createStore(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody StoreCreateRequest request
    ) {
        StoreResponse response =
                storeService.createStore(principal.getUserId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "내 매장 목록 조회",
            description = "로그인한 사용자가 등록한 매장 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<List<StoreResponse>> getMyStores(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        List<StoreResponse> response =
                storeService.getMyStores(principal.getUserId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "매장 상세 조회",
            description = "매장 ID로 상세 정보를 조회합니다. 본인 소유 매장만 조회할 수 있습니다."
    )
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStore(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "매장 ID", example = "1")
            @PathVariable Long storeId
    ) {
        StoreResponse response =
                storeService.getStore(principal.getUserId(), storeId);

        return ResponseEntity.ok(response);
    }
}