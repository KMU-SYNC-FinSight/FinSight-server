package com.finsight.global.security;

/**
 * 토큰에서 꺼낸 사용자 ID로 인증 주체를 불러온다.
 *
 * <p>구현은 user 모듈에 있다. global이 user 모듈을 직접 참조하지 않도록 인터페이스만 둔다.
 */
public interface PrincipalLoader {

    CustomUserPrincipal loadUserById(Long userId);
}
