/**
 * 회원·인증 모듈. 이메일·카카오 회원가입과 로그인, 토큰 발급, 내 정보 조회를 맡는다.
 *
 * <p>다른 모듈에 공개하는 API는 없다. 인증 주체 조회는 global의 {@code PrincipalLoader}를 구현해서 제공한다.
 */
@ApplicationModule(displayName = "회원·인증", allowedDependencies = "global")
package com.finsight.user;

import org.springframework.modulith.ApplicationModule;
