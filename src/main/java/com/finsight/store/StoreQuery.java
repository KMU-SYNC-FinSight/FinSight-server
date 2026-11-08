package com.finsight.store;

/**
 * 다른 모듈이 매장 정보를 읽을 때 쓰는 공개 API.
 */
public interface StoreQuery {

    /**
     * 사용자가 소유한 매장을 조회한다.
     * 매장이 없거나 다른 사용자의 매장이면 {@code STORE_NOT_FOUND}를 던진다.
     */
    StoreSummary getOwnedStore(Long userId, Long storeId);

    /**
     * 매장이 이 사용자의 소유인지 확인한다.
     */
    boolean isOwnedBy(Long storeId, Long userId);
}
