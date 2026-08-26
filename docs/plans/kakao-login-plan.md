# 카카오 소셜 로그인 구현 계획

> 관련 이슈: #3

## 1. 목표

인가 코드(Authorization Code) 방식의 카카오 로그인을 백엔드에 구현한다. 프론트엔드가 카카오 인가 코드를 발급받아 백엔드로 전달하면, 백엔드가 카카오 토큰 발급 → 사용자 정보 조회 → 회원 조회/가입 → JWT 발급까지 처리한다.

기존 이메일 로그인(`AuthController`, `AuthService`)과 동일한 흐름(JWT 발급, `LoginResponse`)에 합류시키고, `User` 엔티티의 `createKakaoUser`, `LoginProvider.KAKAO`는 이미 존재하므로 이를 그대로 활용한다.

## 2. 전체 흐름

```
[프론트엔드]
  카카오 로그인 버튼 → 카카오 인가 코드 획득 (redirect)
        │
        ▼
POST /api/auth/kakao { code }
        │
        ▼
[백엔드: AuthService.kakaoLogin]
  1) KakaoApiClient.getAccessToken(code)      → 카카오 access token
  2) KakaoApiClient.getUserInfo(accessToken)  → 카카오 회원 정보 (id, email, nickname)
  3) UserRepository.findByProviderAndProviderId(KAKAO, kakaoId)
       - 있으면: 기존 회원 로그인
       - 없으면: User.createKakaoUser(...)로 신규 가입
  4) JwtTokenProvider.createAccessToken(user.id, user.role, user.provider)
        │
        ▼
LoginResponse { accessToken, ... } 반환
```

## 3. 설정

- 환경 변수 추가 (`.env`, `application.yml`): `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI`
- 실제 키 값은 커밋하지 않고 로컬 `.env` / 배포 환경 변수(GitHub Actions Secrets 등)에만 저장한다.

```yaml
kakao:
  client-id: ${KAKAO_CLIENT_ID}
  client-secret: ${KAKAO_CLIENT_SECRET}
  redirect-uri: ${KAKAO_REDIRECT_URI}
```

## 4. 패키지/파일 구성 (`com.finsight.auth`)

| 파일 | 역할 |
| --- | --- |
| `client/KakaoApiClient.java` | 카카오 토큰 발급(`kauth.kakao.com/oauth/token`), 사용자 정보 조회(`kapi.kakao.com/v2/user/me`) HTTP 호출 |
| `dto/KakaoTokenResponse.java` | 카카오 토큰 응답 DTO (`access_token` 등) |
| `dto/KakaoUserInfoResponse.java` | 카카오 사용자 정보 응답 DTO (`id`, `kakao_account.email`, `properties.nickname`) |
| `dto/KakaoLoginRequest.java` | 프론트엔드 → 백엔드 요청 DTO (`code`) |
| `controller/AuthController.java` | `POST /api/auth/kakao` 엔드포인트 추가 |
| `service/AuthService.java` | `kakaoLogin(KakaoLoginRequest)` 메서드 추가 |

`config/KakaoProperties.java` (또는 `@ConfigurationProperties`)로 client-id/secret/redirect-uri 바인딩.

## 5. 세부 작업

1. **설정 바인딩**: `kakao.*` 프로퍼티를 읽는 설정 클래스 추가
2. **KakaoApiClient**: `RestClient`(Spring 6) 기반으로 구현
   - `POST https://kauth.kakao.com/oauth/token` (`grant_type=authorization_code`, `client_id`, `client_secret`, `redirect_uri`, `code`)
   - `GET https://kapi.kakao.com/v2/user/me` (`Authorization: Bearer {access_token}`)
   - 실패 시 `BusinessException(ErrorCode.KAKAO_AUTH_FAILED)` 등으로 변환
3. **UserRepository**: `findByProviderAndProviderId(LoginProvider provider, String providerId)` 추가
4. **AuthService.kakaoLogin**
   - 카카오 액세스 토큰 발급 → 사용자 정보 조회
   - `providerId`(카카오 `id`)로 기존 회원 조회, 없으면 `User.createKakaoUser(email, nickname, providerId)`로 신규 가입
   - `UserStatus.ACTIVE` 검증 (탈퇴 회원 재로그인 방지)
   - JWT 발급 후 `LoginResponse.of(user, accessToken)` 반환
5. **AuthController**: `POST /api/auth/kakao` 추가, Swagger 문서화(`@Operation`)
6. **ErrorCode 추가**
   - `KAKAO_AUTH_FAILED` (400/401): 인가 코드 검증 실패
   - `KAKAO_TOKEN_REQUEST_FAILED` (500): 카카오 토큰 요청 실패
   - `KAKAO_USER_INFO_REQUEST_FAILED` (500): 카카오 사용자 정보 조회 실패
7. **SecurityConfig**: 기존 `/api/auth/**` public 매핑에 자동 포함되므로 별도 변경 불필요 (확인만)
8. **테스트**
   - `KakaoApiClient` mocking 후 `AuthService.kakaoLogin` 단위 테스트 (신규 가입/기존 로그인/실패 케이스)
   - 이메일 계정으로 가입된 이메일에 카카오 로그인 시도 시 처리 방침 결정 (이슈에서 별도 정책 확인 필요)

## 6. 확인이 필요한 정책 사항

- 카카오 계정 이메일이 기존 LOCAL 계정 이메일과 동일한 경우 처리 방침 (자동 연동 vs 별도 계정 vs 에러)
- 카카오 동의 항목 중 이메일 필수 동의 여부 (선택 동의 시 `email`이 null일 수 있음)
- 리프레시 토큰 발급 여부 (현재 이메일 로그인은 Access Token만 발급 — 동일하게 유지할지 결정)

## 7. 작업 순서

1. 설정/DTO/`KakaoApiClient` 구현
2. `UserRepository` 조회 메서드 추가
3. `AuthService.kakaoLogin` + `ErrorCode` 추가
4. `AuthController` 엔드포인트 추가 + Swagger
5. 테스트 작성
6. 로컬 환경에서 카카오 개발자 콘솔 리다이렉트 URI로 실제 인가 코드 발급받아 통합 테스트
