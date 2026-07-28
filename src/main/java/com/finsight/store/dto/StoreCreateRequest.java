package com.finsight.store.dto;

import com.finsight.store.domain.BusinessType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "매장 등록 요청")
public record StoreCreateRequest(

        @Schema(description = "매장 이름", example = "핀사이트 카페")
        @NotBlank(message = "매장 이름은 필수입니다.")
        @Size(max = 100, message = "매장 이름은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "업종", example = "CAFE")
        @NotNull(message = "업종은 필수입니다.")
        BusinessType businessType,

        @Schema(description = "주소", example = "서울특별시 성북구")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        @Schema(description = "좌석 수", example = "24")
        @PositiveOrZero(message = "좌석 수는 0 이상이어야 합니다.")
        Integer seatCount,

        @Schema(description = "개업일", example = "2024-03-01")
        LocalDate openedAt
) {
}