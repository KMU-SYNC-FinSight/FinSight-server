package com.finsight.visitor;

/**
 * 매장의 방문객 지표가 새로 저장된 뒤 발행되는 이벤트.
 * 트랜잭션이 커밋되면 score 모듈이 받아 운영 점수를 다시 계산한다.
 */
public record VisitorDataChangedEvent(
        Long storeId
) {
}
