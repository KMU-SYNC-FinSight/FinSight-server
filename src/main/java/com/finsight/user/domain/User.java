package com.finsight.user.domain;

import com.finsight.global.entity.BaseTimeEntity;
import com.finsight.user.domain.LoginProvider;
import com.finsight.user.domain.UserRole;
import com.finsight.user.domain.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, unique = true)
    private String email;

    /*
     * 일반 이메일 회원만 사용한다.
     * 카카오 회원은 비밀번호가 없으므로 null을 허용한다.
     */
    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoginProvider provider;

    /*
     * 카카오가 발급하는 사용자 고유 ID.
     * 일반 이메일 회원은 null이다.
     */
    @Column(name = "provider_id", length = 100)
    private String providerId;

    public static User createLocalUser(
            String email,
            String encodedPassword,
            String name
    ) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.name = name;
        user.role = UserRole.OWNER;
        user.status = UserStatus.ACTIVE;
        user.provider = LoginProvider.LOCAL;
        user.providerId = null;
        return user;
    }

    public static User createKakaoUser(
            String email,
            String name,
            String providerId
    ) {
        User user = new User();
        user.email = email;
        user.password = null;
        user.name = name;
        user.role = UserRole.OWNER;
        user.status = UserStatus.ACTIVE;
        user.provider = LoginProvider.KAKAO;
        user.providerId = providerId;
        return user;
    }

    public void updateProfile(String name, String phone) {
        this.name = name;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }
}