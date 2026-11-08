/**
 * 리포트 모듈. 금융기관용 운영 안정성 보조지표 리포트를 조회한다.
 *
 * <p>자기 데이터는 없고, 매장과 운영 점수의 공개 조회 API만 읽는다.
 */
@ApplicationModule(displayName = "리포트", allowedDependencies = {"global", "store", "score"})
package com.finsight.report;

import org.springframework.modulith.ApplicationModule;
