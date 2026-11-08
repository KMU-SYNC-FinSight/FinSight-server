/**
 * 모든 모듈이 함께 쓰는 공용 모듈 (설정, 예외, 보안, 파일 저장소, 공통 엔티티).
 *
 * <p>OPEN 모듈이라 하위 패키지까지 모두 다른 모듈에 공개한다.
 * 반대로 global은 어떤 도메인 모듈도 참조하면 안 된다 (참조하면 순환 의존으로 검증에 실패한다).
 */
@ApplicationModule(displayName = "Global", type = ApplicationModule.Type.OPEN)
package com.finsight.global;

import org.springframework.modulith.ApplicationModule;
