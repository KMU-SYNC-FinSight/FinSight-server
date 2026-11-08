# Config & Auth — 설정 바인딩 & 인증/인가 컨벤션

> 이 문서는 `application.yml`·환경변수 관리, `@Value` 바인딩, 파일 저장소 전환, JWT 인증 흐름, 매장 소유권 인가를 다룬다.
> 설정값 추가, 인증·인가가 필요한 API 작업 시 참조한다. 모듈 경계 규칙은 `architecture.md`를 따른다.

---

## 1. 설정 파일과 프로필

- 설정은 `src/main/resources/application.yml` 한 파일에 둔다. 프로필은 쓰지 않는다(`application-{profile}.yml`, `spring.profiles.active`, `@Profile` 없음).
- 로컬과 배포의 차이는 환경변수 값으로만 낸다. yml에 값을 직접 적은 항목(`ddl-auto: update`, `format_sql: true`, `logging.level`)은 모든 환경에 똑같이 적용된다.
- 테스트 프로필·테스트 설정 파일·인증 스텁도 없다. `FinSightApplicationTests`(`@SpringBootTest`)는 실제 설정으로 컨텍스트를 띄우므로 2절 환경변수와 DB가 필요하다. `ModularityTests`는 컨텍스트 없이 돈다.

---

## 2. 환경변수 관리

환경마다 다르거나 비밀인 값은 yml에서 `${ENV_VAR}`로 받고, 루트 `.env.example`의 해당 주석 영역(`# DB`, `# JWT`, `# Kakao login` …)에도 같은 이름을 추가한다.

```yaml
kakao:
  client-id: ${KAKAO_CLIENT_ID}
  client-secret: ${KAKAO_CLIENT_SECRET:}   # 유일한 기본값(빈 문자열). 비어 있으면 토큰 요청에서 뺀다
```

- `.env.example`에서 비밀값은 비워 둔다. 공개돼도 되는 값(로컬 `docker-compose.yml`과 같은 DB 접속 정보, 로컬 redirect URI)만 예시로 채운다.
- 필요 조건에 해당하는 변수가 정의되지 않으면 placeholder를 풀지 못해 시작에 실패한다.
- 애플리케이션은 `.env`를 직접 읽지 않는다(`spring.config.import`·dotenv 라이브러리 없음). 실행 환경(IDE 실행 설정, 셸, 배포 환경)이 환경변수로 넣는다. `.env`는 `.gitignore`에 있다.

| 변수 | yml 키 | 읽는 곳 | 필요 조건 |
| --- | --- | --- | --- |
| `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | `spring.datasource.*` | Spring Boot (포트 3306은 URL에 고정) | 항상 |
| `JWT_SECRET`, `JWT_EXPIRATION` | `jwt.secret`, `jwt.access-token-expiration` | `JwtTokenProvider` (만료는 밀리초) | 항상 |
| `FILE_STORAGE_TYPE` | `file.storage.type` | `FileStorage` 구현 선택 (4절) | 항상 |
| `FILE_UPLOAD_DIR` | `file.upload.local-dir` | `LocalFileStorage` | `local`일 때 |
| `AWS_S3_BUCKET`, `AWS_REGION` | `aws.s3.*` | `S3FileStorage` | `s3`일 때 |
| `AI_SERVER_URL` | `ai.server.url` | `FastApiVideoAnalysisClient` (조건 없는 `@Component`) | 항상 |
| `KAKAO_CLIENT_ID`, `KAKAO_REDIRECT_URI` | `kakao.*` | `KakaoApiClient` (redirect URI는 프론트가 인가 요청에 쓴 값과 같아야 한다) | 항상 |
| `KAKAO_CLIENT_SECRET` | `kakao.client-secret` | `KakaoApiClient` | 카카오 앱에서 Client Secret을 켰을 때 |

---

## 3. 설정 바인딩

- `@Value`로 주입한다. `@ConfigurationProperties`·`@ConfigurationPropertiesScan`은 쓰지 않는다.
- 생성자 파라미터에 `@Value`를 붙여 `final` 필드에 담는다. 필드 주입은 `UploadService.maxVideoSize`(`file.upload.max-video-size`) 한 곳뿐이다.
- `@Value`에는 yml 키를 쓴다. 코드에서 환경변수 이름(`${JWT_SECRET}`)을 직접 참조하지 않는다.

```java
// global.security — JwtTokenProvider.java
public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-expiration}") long accessTokenExpiration
) { ... }
```

- yml에 값을 직접 적은 `file.upload.max-video-size: 209715200`(200MB)는 `spring.servlet.multipart.max-file-size: 200MB`와 같은 값이다. 한쪽을 바꾸면 다른 쪽도 맞춘다.

---

## 4. 파일 저장소 전환

파일 저장은 `global.storage.FileStorage`(`store(file, directory)`, `downloadToTemp(storedFilePath)`)로만 한다. 구현은 `file.storage.type`으로 고르고, `@ConditionalOnProperty`로 하나만 빈이 된다.

| `FILE_STORAGE_TYPE` | 구현 | `store()` 반환값 | `downloadToTemp()` |
| --- | --- | --- | --- |
| `local` | `LocalFileStorage` | `{FILE_UPLOAD_DIR}/{directory}/{UUID}_{원본명}` 절대 경로 | 저장 경로 그대로 |
| `s3` | `S3FileStorage` | `{directory}/{UUID}_{원본명}` S3 key | `ai-analysis-*` 임시 파일로 내려받음 |

- 호출하는 쪽은 구현을 모른다. `directory`는 용도별 문자열이다(`UploadService` → `"videos"`, `SalesService` → `"sales"`).
- `LocalFileStorage`의 `matchIfMissing = true`는 효과가 없다. yml이 키를 항상 정의하므로 값이 비었거나 `local`/`s3`가 아니면 `FileStorage` 빈이 없어 시작에 실패한다.
- AWS 자격 증명은 yml·`.env.example`에 두지 않는다. `S3FileStorage`가 SDK `DefaultCredentialsProvider`(환경변수, `~/.aws` 프로필, IAM 역할 등 기본 체인)로 찾는다.
- `store()` 반환값은 `data_uploads.stored_file_path`에 저장된다. 저장소 종류를 바꾸면 기존 행의 경로는 새 구현에서 읽을 수 없다.

---

## 5. 인증 모델

JWT Access Token 하나로 인증한다. Refresh Token과 로그아웃(토큰 폐기)은 없다. 세션은 `STATELESS`, CSRF는 끈다.

### 5-1. 토큰 claim

`JwtTokenProvider`가 `JWT_SECRET`으로 HMAC 서명한다(키 길이에 따라 jjwt가 HS256 이상을 고른다).

| Claim | 값 | 검증 때 사용 |
| --- | --- | --- |
| `sub` | userId (숫자 문자열) | O — `getUserId()`가 반환 |
| `role` / `provider` | `UserRole.name()` / `LoginProvider.name()` | X |
| `iat` / `exp` | 발급 시각 / 발급 + `JWT_EXPIRATION` ms | `exp`만 확인 |

> 권한 판단에는 토큰의 `role`이 아니라 요청마다 `PrincipalLoader`가 불러온 DB 값(캐시)을 쓴다.

### 5-2. 로그인 경로

모두 `user` 모듈 `AuthController`(`/api/auth`) → `AuthService`가 처리한다.

| API | 처리 | 토큰 |
| --- | --- | --- |
| `POST /api/auth/signup` | 이메일 중복 확인 → BCrypt 인코딩 → `OWNER`·`ACTIVE`·`LOCAL` 사용자 저장 | 발급 안 함 |
| `POST /api/auth/login` | 이메일 조회 → `LOCAL` 계정인지 → 비밀번호 일치 → `ACTIVE`인지 | `LoginResponse` |
| `POST /api/auth/kakao` | 인가 코드 → 카카오 토큰 → 사용자 정보 → `(KAKAO, providerId)` 조회, 없으면 자동 가입 → `ACTIVE`인지 | `LoginResponse` |

- 이메일 로그인 실패(없는 이메일, 카카오 계정, 비밀번호 불일치)는 모두 `INVALID_LOGIN_CREDENTIALS`로 응답한다. 어느 조건에서 실패했는지 드러내지 않는다.
- 카카오는 프론트엔드가 인가 코드를 받아 `{ "code": ... }`로 보내고, `KakaoApiClient`(`RestClient`)가 카카오 API를 직접 호출한다. `spring-boot-starter-oauth2-client` 의존성은 있지만 `oauth2Login`·client registration은 쓰지 않는다.

### 5-3. 인증 흐름

```
HTTP Request (Authorization: Bearer <token>)
    ↓
JwtAuthenticationFilter        — 서명·만료 검증 → sub에서 userId (@Component 아님, SecurityConfig가 생성)
    ↓
PrincipalLoader.loadUserById   — UserPrincipalService: 캐시, 없으면 users 조회 + ACTIVE 확인
    ↓
SecurityContextHolder          — CustomUserPrincipal + 권한 ROLE_{role}
    ↓
SecurityConfig                 — PUBLIC_URLS는 permitAll, 나머지는 authenticated()
    ↓
Controller                     — @AuthenticationPrincipal CustomUserPrincipal
```

- 필터는 예외를 던지지 않는다. 실패하면 `ErrorCode`를 request attribute `"exception"`에 담고 체인을 계속 탄다. 인증이 필요한 경로면 `JwtAuthenticationEntryPoint`가 그 코드(없으면 `UNAUTHORIZED`)로 `ErrorResponse`를 쓴다. 공개 경로는 토큰이 잘못돼도 그대로 통과한다.

| 상황 | ErrorCode | 상태 |
| --- | --- | --- |
| 토큰 없음, `Bearer ` 접두사 아님 | `UNAUTHORIZED` | 401 |
| 만료 / 서명·형식 오류 | `EXPIRED_TOKEN` / `INVALID_TOKEN` | 401 |
| 토큰의 사용자가 없음 | `USER_NOT_FOUND` | 404 |
| `ACTIVE`가 아닌 계정 | `INACTIVE_ACCOUNT` | 403 |

### 5-4. 인증 주체 캐시

- `global`이 `user`를 참조하지 않도록 `global.security.PrincipalLoader`를 두고 `user.internal.service.UserPrincipalService`가 구현한다 (`architecture.md` 3절).
- `@Cacheable(cacheNames = CacheConfig.USER_PRINCIPAL_CACHE, key = "#userId")` — Caffeine, 쓰기 후 5분 만료, 최대 1,000개. 예외는 캐시되지 않는다.
- `CacheConfig`는 `CaffeineCacheManager`를 캐시 이름과 함께 만들므로 등록하지 않은 이름은 쓸 수 없다. 새 캐시는 `CacheConfig`에 이름 상수를 추가해 생성자에 넘긴다.
- `@CacheEvict`는 아직 없다. 사용자 상태·역할을 바꾸는 쓰기 API를 만들면 그 메서드에 같은 캐시 이름·키로 `@CacheEvict`를 단다 (`docs/plans/caffeine-cache-plan.md` 4.4). 그 전까지 변경은 최대 5분 늦게 반영된다.

### 5-5. 역할 (`UserRole`)

- `user.internal.domain.UserRole`은 `OWNER`, `FINANCIAL_ANALYST`, `ADMIN`이다. 가입 경로(`User.createLocalUser`, `User.createKakaoUser`)는 모두 `OWNER`로 만들고, 나머지 둘을 부여하는 코드는 없다.
- 역할로 인가하는 곳은 없다. `hasRole`·`hasAuthority`·`@PreAuthorize`·`@EnableMethodSecurity`를 쓰지 않는다. 역할은 토큰 claim과 응답(`SignupResponse`, `LoginResponse`, `UserMeResponse`)에만 나간다.
- `global`은 `UserRole`을 모른다. `CustomUserPrincipal`·`JwtTokenProvider`는 역할을 enum 이름 `String`으로 받는다. 권한 문자열은 `"ROLE_" + role`이므로 역할 인가를 붙인다면 `hasRole("OWNER")` 형태가 이 값과 맞는다.

---

## 6. 인가 — 매장 소유권

FinSight의 인가는 역할이 아니라 **매장 소유권**이다. 매장(`stores.user_id`)에 딸린 데이터는 Service에서 요청한 사용자가 소유자인지 확인한다. 다른 모듈은 `store`의 공개 API `StoreQuery`를 쓴다.

```java
// store — StoreQuery.java
StoreSummary getOwnedStore(Long userId, Long storeId);   // 없거나 남의 매장이면 STORE_NOT_FOUND
boolean isOwnedBy(Long storeId, Long userId);            // 인자 순서가 getOwnedStore와 반대
```

- 소유권 확인은 Service 메서드의 첫 단계에서 한다.
- 남의 매장도 403이 아니라 "없음"(404)으로 응답한다. 매장이 있는지 드러내지 않는다.
- 하위 리소스(업로드)는 리소스를 찾은 뒤 그 `storeId`로 `isOwnedBy`를 부르고, 실패하면 그 리소스의 NOT_FOUND 코드로 응답한다.
- `StoreQuery`를 쓰려면 `package-info.java`의 `allowedDependencies`에 `store`를 넣는다 (`architecture.md` 4-2절).

| 위치 | 방법 | 실패 시 |
| --- | --- | --- |
| `UploadService.uploadVideo`, `SalesService.uploadSalesCsv` | `getOwnedStore` | `STORE_NOT_FOUND` |
| `DashboardService.getDashboard`, `ReportService.getReport` | `getOwnedStore` | `STORE_NOT_FOUND` |
| `UploadService.getUploadStatus` | `isOwnedBy(upload.getStoreId(), userId)` | `UPLOAD_NOT_FOUND` |
| `StoreService.getStore` (store 내부) | `StoreRepository.findByIdAndUserId` | `STORE_NOT_FOUND` |

> 금융기관용 리포트(`ReportService`)도 소유자 본인만 조회할 수 있다. `FINANCIAL_ANALYST`가 다른 사용자 매장의 리포트를 보는 경로는 없다.

---

## 7. 사용 규칙

### 7-1. Controller

`@AuthenticationPrincipal CustomUserPrincipal principal`로 받고 `principal.getUserId()`만 Service에 넘긴다. `SecurityContextHolder`를 직접 꺼내지 않는다. Service는 `Long userId`를 받고 `CustomUserPrincipal`에 의존하지 않는다.

```java
// dashboard — DashboardController.java
@GetMapping("/api/stores/{storeId}/dashboard")
public ResponseEntity<DashboardResponse> getDashboard(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @Parameter(description = "매장 ID", example = "1")
        @PathVariable Long storeId
) {
    DashboardResponse response =
            dashboardService.getDashboard(principal.getUserId(), storeId);
    return ResponseEntity.ok(response);
}
```

- Swagger: springdoc이 `@AuthenticationPrincipal` 파라미터를 문서에서 자동으로 뺀다. `@Parameter(hidden = true)`를 붙이지 않는다.
- `OpenApiConfig`가 전역 `bearerAuth` 보안 요구사항을 걸어서 공개 API(`/api/auth/**`)도 Swagger에는 인증 필요로 표시된다. 공개 경로를 따로 비우는 설정은 없다.

### 7-2. 인증 불필요 API

`SecurityConfig.PUBLIC_URLS` 배열에 패턴을 추가한다. 그 외 경로는 모두 `authenticated()`다.

| 패턴 | 이유 |
| --- | --- |
| `/api/auth/**` | 회원가입·로그인·카카오 로그인 |
| `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, `/v3/api-docs/**` | Swagger UI·문서 (프로필이 없어 배포 환경에서도 공개) |
| `/error` | 매핑되지 않은 요청 등의 오류 응답이 401로 가려지지 않게 |

- `/api/auth/**` 아래는 전부 공개다. 인증이 필요한 API를 이 경로에 두지 않는다. 공개 API의 Controller는 principal 파라미터를 선언하지 않는다 (`AuthController`).
- CORS 허용 origin은 `SecurityConfig.corsConfigurationSource()`에 코드로 둔다(`http://localhost:*`, `https://kmu-finsight*.vercel.app`, credentials 허용).

---

## 8. 비밀값 취급

- 비밀값(`DB_PASSWORD`, `JWT_SECRET`, `KAKAO_CLIENT_SECRET`)은 환경변수로만 넣는다. yml에는 `${...}` 참조만 쓴다. AWS 자격 증명은 SDK 기본 체인에 맡긴다(4절).
- `.env`와 `*.pem`은 `.gitignore`에 있다. `.env.example`에서 채워 둔 비밀값은 `DB_PASSWORD` 하나이고, 로컬 `docker-compose.yml`용 값이므로 배포 DB에 쓰지 않는다.
- `JWT_SECRET`은 UTF-8 32바이트(256비트) 이상이어야 한다. 짧거나 비어 있으면 `Keys.hmacShaKeyFor`가 `WeakKeyException`을 던져 시작에 실패한다.
- 사용자 비밀번호는 `SecurityConfig`의 `BCryptPasswordEncoder`로 인코딩해 저장한다.
- 애플리케이션 로그에는 토큰·인가 코드·비밀값을 남기지 않는다(`KakaoApiClient`는 상태 코드만 남긴다). 단 `org.hibernate.orm.jdbc.bind: trace`는 SQL 바인딩 값을 모두 출력한다 (`logging.md` 6절).
