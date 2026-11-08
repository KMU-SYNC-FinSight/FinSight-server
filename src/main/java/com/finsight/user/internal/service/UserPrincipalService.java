package com.finsight.user.internal.service;

import com.finsight.global.config.CacheConfig;
import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.global.security.CustomUserPrincipal;
import com.finsight.global.security.PrincipalLoader;
import com.finsight.user.internal.domain.User;
import com.finsight.user.internal.domain.UserStatus;
import com.finsight.user.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JWT 인증 필터가 쓰는 인증 주체 조회 구현.
 * 요청마다 DB를 조회하지 않도록 사용자 ID 기준으로 캐시한다.
 */
@Service
@RequiredArgsConstructor
public class UserPrincipalService implements PrincipalLoader {

    private final UserRepository userRepository;

    @Override
    @Cacheable(cacheNames = CacheConfig.USER_PRINCIPAL_CACHE, key = "#userId")
    @Transactional(readOnly = true)
    public CustomUserPrincipal loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INACTIVE_ACCOUNT);
        }

        return CustomUserPrincipal.of(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole().name()
        );
    }
}
