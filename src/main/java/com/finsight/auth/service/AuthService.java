package com.finsight.auth.service;

import com.finsight.auth.dto.LoginRequest;
import com.finsight.auth.dto.LoginResponse;
import com.finsight.auth.dto.SignupRequest;
import com.finsight.auth.dto.SignupResponse;
import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.global.security.JwtTokenProvider;
import com.finsight.user.domain.LoginProvider;
import com.finsight.user.domain.User;
import com.finsight.user.domain.UserStatus;
import com.finsight.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateDuplicateEmail(request.email());

        String encodedPassword =
                passwordEncoder.encode(request.password());

        User user = User.createLocalUser(
                request.email(),
                encodedPassword,
                request.name()
        );

        User savedUser = userRepository.save(user);

        return SignupResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        // 카카오 등 LOCAL이 아닌 계정은 이메일 로그인 불가
        if (user.getProvider() != LoginProvider.LOCAL) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INACTIVE_ACCOUNT);
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getRole(),
                user.getProvider()
        );

        return LoginResponse.of(user, accessToken);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_EMAIL
            );
        }
    }
}