/**
 * 매출 모듈. 매출 CSV를 파싱해 일자별 매출을 저장한다 (같은 날짜는 덮어쓴다).
 *
 * <p>공개 API: {@link com.finsight.sales.SalesMetricQuery}, {@link com.finsight.sales.DailySales}
 * <br>발행 이벤트: {@link com.finsight.sales.SalesDataChangedEvent} (score 모듈이 받아 점수를 다시 계산한다)
 */
@ApplicationModule(displayName = "매출", allowedDependencies = {"global", "store", "upload"})
package com.finsight.sales;

import org.springframework.modulith.ApplicationModule;
