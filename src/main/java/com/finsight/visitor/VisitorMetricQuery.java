package com.finsight.visitor;

import java.util.List;

/**
 * 다른 모듈이 방문객 지표를 읽을 때 쓰는 공개 API.
 */
public interface VisitorMetricQuery {

    /**
     * 매장의 방문객 지표를 저장된 순서(오래된 것부터)로 돌려준다.
     */
    List<VisitorMetricSnapshot> findAllByStoreId(Long storeId);
}
