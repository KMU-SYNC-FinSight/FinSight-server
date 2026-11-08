/**
 * 매장 모듈. 매장 등록·조회와 매장 소유권 확인을 맡는다.
 *
 * <p>공개 API: {@link com.finsight.store.StoreQuery}, {@link com.finsight.store.StoreSummary}
 */
@ApplicationModule(displayName = "매장", allowedDependencies = "global")
package com.finsight.store;

import org.springframework.modulith.ApplicationModule;
