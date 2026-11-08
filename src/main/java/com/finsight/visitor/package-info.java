/**
 * 방문객 분석 모듈. 매장 영상을 AI 서버로 분석해 방문객 지표를 만들고 저장한다.
 *
 * <p>공개 API: {@link com.finsight.visitor.VisitorMetricQuery}, {@link com.finsight.visitor.VisitorMetricSnapshot},
 * {@link com.finsight.visitor.CongestionLevel}
 * <br>구독 이벤트: upload의 {@code VideoUploadedEvent}
 * <br>발행 이벤트: {@link com.finsight.visitor.VisitorDataChangedEvent} (score 모듈이 받아 점수를 다시 계산한다)
 */
@ApplicationModule(displayName = "방문객 분석", allowedDependencies = {"global", "upload"})
package com.finsight.visitor;

import org.springframework.modulith.ApplicationModule;
