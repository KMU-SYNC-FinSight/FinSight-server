package com.finsight.global.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    // 권한 이름 (예: OWNER). global이 user 모듈의 UserRole을 알지 않도록 문자열로 받는다.
    private final String role;

    private CustomUserPrincipal(
            Long userId,
            String email,
            String password,
            String role
    ) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public static CustomUserPrincipal of(
            Long userId,
            String email,
            String password,
            String role
    ) {
        return new CustomUserPrincipal(userId, email, password, role);
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}