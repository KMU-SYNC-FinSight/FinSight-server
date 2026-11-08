package com.finsight.score.internal.listener;

import com.finsight.sales.SalesDataChangedEvent;
import com.finsight.score.internal.service.ScoreService;
import com.finsight.visitor.VisitorDataChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 매출·방문객 데이터가 바뀌면 운영 점수를 다시 계산한다.
 *
 * <p>{@code @ApplicationModuleListener}는 발행한 트랜잭션이 커밋된 뒤 별도 스레드·새 트랜잭션에서 실행된다.
 * 실패하면 이벤트 발행 기록(event_publication)이 미완료로 남는다.
 */
@Component
@RequiredArgsConstructor
public class ScoreRecalculationListener {

    private final ScoreService scoreService;

    @ApplicationModuleListener
    public void on(SalesDataChangedEvent event) {
        scoreService.recalculate(event.storeId());
    }

    @ApplicationModuleListener
    public void on(VisitorDataChangedEvent event) {
        scoreService.recalculate(event.storeId());
    }
}
