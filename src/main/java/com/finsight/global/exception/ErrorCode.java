package com.finsight.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    EMPTY_FILE(
            HttpStatus.BAD_REQUEST,
            "EMPTY_FILE",
            "업로드된 파일이 비어 있습니다."
    ),

    FILE_TOO_LARGE(
            HttpStatus.BAD_REQUEST,
            "FILE_TOO_LARGE",
            "허용된 파일 크기를 초과했습니다."
    ),

    UNSUPPORTED_FILE_TYPE(
            HttpStatus.BAD_REQUEST,
            "UNSUPPORTED_FILE_TYPE",
            "지원하지 않는 파일 형식입니다."
    ),

    FILE_STORAGE_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "FILE_STORAGE_ERROR",
            "파일 저장 중 오류가 발생했습니다."
    ),

    DUPLICATE_EMAIL(
            HttpStatus.CONFLICT,
            "DUPLICATE_EMAIL",
            "이미 사용 중인 이메일입니다."
    ),

    INVALID_LOGIN_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "INVALID_LOGIN_CREDENTIALS",
            "이메일 또는 비밀번호가 올바르지 않습니다."
    ),

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER_NOT_FOUND",
            "사용자를 찾을 수 없습니다."
    ),

    VALIDATION_ERROR(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "입력값이 올바르지 않습니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다."
    ),

    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
        "UNAUTHORIZED",
                "인증이 필요합니다."
    ),

    INVALID_TOKEN(
            HttpStatus.UNAUTHORIZED,
        "INVALID_TOKEN",
                "유효하지 않은 토큰입니다."
    ),

    EXPIRED_TOKEN(
            HttpStatus.UNAUTHORIZED,
        "EXPIRED_TOKEN",
                "만료된 토큰입니다."
    ),

    INACTIVE_ACCOUNT(
            HttpStatus.FORBIDDEN,
        "INACTIVE_ACCOUNT",
                "이용할 수 없는 계정입니다."
    ),

    STORE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "STORE_NOT_FOUND",
                    "매장을 찾을 수 없습니다."
    ),

    UPLOAD_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "UPLOAD_NOT_FOUND",
            "업로드 내역을 찾을 수 없습니다."
    ),

    CSV_INVALID_HEADER(
            HttpStatus.BAD_REQUEST,
            "CSV_INVALID_HEADER",
            "CSV 헤더 형식이 올바르지 않습니다. (date,salesAmount,transactionCount)"
    ),

    CSV_NO_VALID_ROWS(
            HttpStatus.BAD_REQUEST,
            "CSV_NO_VALID_ROWS",
            "처리할 수 있는 유효한 데이터 행이 없습니다."
    ),

    CSV_PARSE_ERROR(
            HttpStatus.BAD_REQUEST,
            "CSV_PARSE_ERROR",
            "CSV 파일을 읽는 중 오류가 발생했습니다."
    ),
    SCORE_NOT_READY(
            HttpStatus.NOT_FOUND,
            "SCORE_NOT_READY",
            "아직 운영 점수가 산출되지 않았습니다. 데이터를 먼저 업로드해 주세요."
    ),
    AI_ANALYSIS_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "AI_ANALYSIS_FAILED",
            "영상 분석 중 오류가 발생했습니다."
    ),

    AI_ANALYSIS_TIMEOUT(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "AI_ANALYSIS_TIMEOUT",
            "영상 분석 시간이 초과되었습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}