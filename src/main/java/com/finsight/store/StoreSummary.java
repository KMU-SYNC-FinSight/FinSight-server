package com.finsight.store;

/**
 * 다른 모듈에 넘겨주는 매장 요약 정보.
 */
public record StoreSummary(
        Long storeId,
        String name
) {
}
