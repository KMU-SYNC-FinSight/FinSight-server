package com.finsight.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 로그인 요청")
public record KakaoLoginRequest(

        @Schema(description = "카카오 인가 코드", example = "a1b2c3d4e5...")
        @NotBlank(message = "인가 코드는 필수입니다.")
        String code
) {
}
