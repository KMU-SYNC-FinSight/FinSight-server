/**
 * 대시보드 모듈. 소상공인용 매장 운영 대시보드를 조회한다.
 *
 * <p>자기 데이터는 없고, 다른 모듈의 공개 조회 API만 읽어 화면용 응답을 조합한다.
 */
@ApplicationModule(
        displayName = "대시보드",
        allowedDependencies = {"global", "store", "score", "sales", "visitor"}
)
package com.finsight.dashboard;

import org.springframework.modulith.ApplicationModule;
