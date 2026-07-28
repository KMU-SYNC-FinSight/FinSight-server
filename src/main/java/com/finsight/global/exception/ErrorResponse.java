package com.finsight.global.exception;

import com.finsight.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "API 오류 응답")
public record ErrorResponse(

        @Schema(example = "DUPLICATE_EMAIL")
        String code,

        @Schema(example = "이미 사용 중인 이메일입니다.")
        String message,

        Map<String, String> errors,

        LocalDateTime timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                LocalDateTime.now()
        );
    }

    public static ErrorResponse validation(
            ErrorCode errorCode,
            Map<String, String> errors
    ) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                errors,
                LocalDateTime.now()
        );
    }
}