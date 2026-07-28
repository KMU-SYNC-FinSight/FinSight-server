package com.finsight.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "일반 이메일 회원가입 요청")
public record SignupRequest(

        @Schema(
                description = "로그인에 사용할 이메일",
                example = "owner@finsight.com"
        )
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(
                description = "로그인 비밀번호",
                example = "password123"
        )
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(
                min = 8,
                max = 20,
                message = "비밀번호는 8자 이상 20자 이하여야 합니다."
        )
        String password,

        @Schema(
                description = "사용자 이름",
                example = "이서준"
        )
        @NotBlank(message = "이름은 필수입니다.")
        @Size(
                max = 50,
                message = "이름은 50자 이하여야 합니다."
        )
        String name
) {
}