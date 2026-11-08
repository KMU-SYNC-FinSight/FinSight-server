package com.finsight.user.internal.service;

import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import com.finsight.user.internal.domain.User;
import com.finsight.user.internal.dto.UserMeResponse;
import com.finsight.user.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserMeResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserMeResponse.from(user);
    }
}