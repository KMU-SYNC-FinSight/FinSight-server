package com.finsight.store.dto;

import com.finsight.store.domain.BusinessType;
import com.finsight.store.domain.Store;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "매장 응답")
public record StoreResponse(

        @Schema(description = "매장 ID", example = "1")
        Long storeId,

        @Schema(description = "매장 이름", example = "핀사이트 카페")
        String name,

        @Schema(description = "업종", example = "CAFE")
        BusinessType businessType,

        @Schema(description = "주소", example = "서울특별시 성북구")
        String address,

        @Schema(description = "좌석 수", example = "24")
        Integer seatCount,

        @Schema(description = "개업일", example = "2024-03-01")
        LocalDate openedAt
) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getBusinessType(),
                store.getAddress(),
                store.getSeatCount(),
                store.getOpenedAt()
        );
    }
}