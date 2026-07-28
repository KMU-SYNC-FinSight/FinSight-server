package com.finsight.user.controller;

import com.finsight.global.security.CustomUserPrincipal;
import com.finsight.user.dto.UserMeResponse;
import com.finsight.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 정보 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "내 정보 조회",
            description = "JWT 토큰으로 인증된 현재 사용자의 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        UserMeResponse response =
                userService.getMyInfo(principal.getUserId());

        return ResponseEntity.ok(response);
    }
}