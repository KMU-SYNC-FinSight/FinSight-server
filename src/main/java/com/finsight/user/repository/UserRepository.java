package com.finsight.user.repository;

import com.finsight.user.domain.LoginProvider;
import com.finsight.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(
            LoginProvider provider,
            String providerId
    );
}