package com.finsight.auth.controller;

import com.finsight.auth.dto.KakaoLoginRequest;
import com.finsight.auth.dto.LoginRequest;
import com.finsight.auth.dto.LoginResponse;
import com.finsight.auth.dto.SignupRequest;
import com.finsight.auth.dto.SignupResponse;
import com.finsight.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Auth",
        description = "회원가입 및 로그인 API"
)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "일반 이메일 회원가입",
            description = "이메일과 비밀번호를 이용해 소상공인 계정을 생성합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "일반 이메일 로그인",
            description = "이메일과 비밀번호로 로그인하고 JWT Access Token을 발급합니다."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "카카오 로그인",
            description = "카카오 인가 코드로 로그인하고 JWT Access Token을 발급합니다. 신규 카카오 회원은 자동으로 가입됩니다."
    )
    @PostMapping("/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request
    ) {
        LoginResponse response = authService.kakaoLogin(request);
        return ResponseEntity.ok(response);
    }
}