package com.finsight.sales;

import java.util.List;

/**
 * 다른 모듈이 매출 데이터를 읽을 때 쓰는 공개 API.
 */
public interface SalesMetricQuery {

    /**
     * 매장의 일자별 매출을 날짜 오름차순으로 돌려준다.
     */
    List<DailySales> findAllByStoreId(Long storeId);
}
