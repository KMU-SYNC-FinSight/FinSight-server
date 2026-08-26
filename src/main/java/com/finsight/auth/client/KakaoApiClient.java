package com.finsight.auth.client;

import com.finsight.auth.dto.KakaoTokenResponse;
import com.finsight.auth.dto.KakaoUserInfoResponse;
import com.finsight.global.exception.BusinessException;
import com.finsight.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoApiClient {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoApiClient(
            RestClient restClient,
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.client-secret}") String clientSecret,
            @Value("${kakao.redirect-uri}") String redirectUri
    ) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public String getAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        if (StringUtils.hasText(clientSecret)) {
            body.add("client_secret", clientSecret);
        }
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new BusinessException(ErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
            }
            return response.accessToken();
        } catch (HttpClientErrorException e) {
            log.warn("카카오 인가 코드 검증 실패: status={}", e.getStatusCode());
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        } catch (RestClientException e) {
            log.warn("카카오 토큰 요청 실패", e);
            throw new BusinessException(ErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
        }
    }

    public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri(USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null || response.id() == null) {
                throw new BusinessException(ErrorCode.KAKAO_USER_INFO_REQUEST_FAILED);
            }
            return response;
        } catch (RestClientException e) {
            log.warn("카카오 사용자 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.KAKAO_USER_INFO_REQUEST_FAILED);
        }
    }
}
