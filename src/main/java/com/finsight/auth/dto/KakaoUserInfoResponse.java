package com.finsight.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(

        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount,

        Properties properties
) {

    public String email() {
        return kakaoAccount != null ? kakaoAccount.email() : null;
    }

    public String nickname() {
        return properties != null ? properties.nickname() : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            String email
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
            String nickname
    ) {
    }
}
