package com.finsight.user.dto;

import com.finsight.user.domain.LoginProvider;
import com.finsight.user.domain.User;
import com.finsight.user.domain.UserRole;
import com.finsight.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보 조회 응답")
public record UserMeResponse(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "owner@finsight.com")
        String email,

        @Schema(description = "이름", example = "이서준")
        String name,

        @Schema(description = "권한", example = "OWNER")
        UserRole role,

        @Schema(description = "로그인 제공자", example = "LOCAL")
        LoginProvider provider,

        @Schema(description = "계정 상태", example = "ACTIVE")
        UserStatus status
) {

    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getProvider(),
                user.getStatus()
        );
    }
}