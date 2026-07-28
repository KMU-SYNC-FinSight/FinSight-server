package com.finsight.auth.dto;

import com.finsight.user.domain.User;
import com.finsight.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record SignupResponse(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(
                description = "가입 이메일",
                example = "owner@finsight.com"
        )
        String email,

        @Schema(description = "사용자 이름", example = "이서준")
        String name,

        @Schema(description = "사용자 권한", example = "OWNER")
        UserRole role
) {

    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
}