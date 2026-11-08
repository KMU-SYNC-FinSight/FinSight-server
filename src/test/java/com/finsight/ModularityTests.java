package com.finsight;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * 모듈 경계 검증. 스프링 컨텍스트나 DB 없이 바이트코드만 분석한다.
 *
 * <ul>
 *     <li>모듈 사이 순환 의존이 없는지</li>
 *     <li>다른 모듈의 {@code internal} 패키지를 참조하지 않는지 (공개 API는 모듈 최상위 패키지만)</li>
 *     <li>각 모듈의 package-info에 선언한 {@code allowedDependencies} 밖의 모듈을 참조하지 않는지</li>
 * </ul>
 */
class ModularityTests {

    private final ApplicationModules modules =
            ApplicationModules.of(FinSightApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
