package com.finsight.auth.dto;

import com.finsight.user.domain.User;
import com.finsight.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(

        @Schema(description = "발급된 Access Token")
        String accessToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 이름", example = "이서준")
        String name,

        @Schema(description = "사용자 권한", example = "OWNER")
        UserRole role
) {

    public static LoginResponse of(User user, String accessToken) {
        return new LoginResponse(
                accessToken,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getRole()
        );
    }
}