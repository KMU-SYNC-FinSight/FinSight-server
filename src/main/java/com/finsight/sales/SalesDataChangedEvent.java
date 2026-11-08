package com.finsight.sales;

/**
 * 매장의 매출 데이터가 추가·수정된 뒤 발행되는 이벤트.
 * 트랜잭션이 커밋되면 score 모듈이 받아 운영 점수를 다시 계산한다.
 */
public record SalesDataChangedEvent(
        Long storeId
) {
}
