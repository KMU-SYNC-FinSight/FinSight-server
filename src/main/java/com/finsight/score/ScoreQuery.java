package com.finsight.score;

import java.util.Optional;

/**
 * 다른 모듈이 운영 점수를 읽을 때 쓰는 공개 API.
 */
public interface ScoreQuery {

    /**
     * 매장의 현재 운영 점수. 아직 계산된 적이 없으면 비어 있다.
     */
    Optional<ScoreSnapshot> findByStoreId(Long storeId);
}
