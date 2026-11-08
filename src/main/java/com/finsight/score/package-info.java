/**
 * 운영 점수 모듈. 매출·방문객 데이터로 매장의 운영 안정성 점수를 계산하고 보관한다.
 *
 * <p>공개 API: {@link com.finsight.score.ScoreQuery}, {@link com.finsight.score.ScoreSnapshot},
 * {@link com.finsight.score.Grade}, {@link com.finsight.score.RiskLevel}
 * <br>구독 이벤트: sales의 {@code SalesDataChangedEvent}, visitor의 {@code VisitorDataChangedEvent}
 */
@ApplicationModule(displayName = "운영 점수", allowedDependencies = {"global", "sales", "visitor"})
package com.finsight.score;

import org.springframework.modulith.ApplicationModule;
