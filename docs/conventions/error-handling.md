# Error Handling — 예외 처리 & Swagger 명세

> ErrorCode/BusinessException 체계, GlobalExceptionHandler와 오류 응답 형식, 인증·외부 API·이벤트 리스너의 오류 처리, 컨트롤러 기반 Swagger 명세를 다룬다.
> 새 에러 코드 추가, 예외 처리 로직, API 문서 작업 시 참조한다. 모듈 배치는 `architecture.md`.

---

## 1. 구성 요소

예외 처리 클래스는 모두 `global`에 있다. `global`은 OPEN 모듈이라 모든 도메인 모듈이 그대로 import한다.

| 클래스 | 패키지 | 역할 |
| --- | --- | --- |
| `ErrorCode` | `global.exception` | 전체 에러 코드 enum 하나. `HttpStatus status`·`String code`·`String message` |
| `BusinessException` | `global.exception` | `ErrorCode`를 담아 던지는 `RuntimeException` |
| `ErrorResponse` | `global.exception` | 오류 응답 본문 record |
| `GlobalExceptionHandler` | `global.exception` | `@RestControllerAdvice`. 컨트롤러 밖으로 나온 예외를 `ErrorResponse`로 바꾼다 |
| `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint` | `global.security` | 필터 단계 인증 오류를 `ErrorResponse`로 응답한다 (7절) |

- 도메인 전용 에러 코드도 `global.exception.ErrorCode`에 둔다. 도메인별 enum을 따로 만들지 않는다.
- 성공 응답은 래퍼 없이 DTO를 `ResponseEntity`에 담아 돌려준다. 오류일 때만 `ErrorResponse`를 쓴다.

---

## 2. ErrorCode 정의 규칙

```java
// global/exception/ErrorCode.java (@Getter @RequiredArgsConstructor enum)
    STORE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "STORE_NOT_FOUND",
            "매장을 찾을 수 없습니다."
    ),
```

### 2-1. 이름과 code

- 상수 이름은 영어 대문자 스네이크 케이스로, 무엇이 어떻게 됐는지를 쓴다. 리소스 없음은 `{대상}_NOT_FOUND`(`STORE_NOT_FOUND`), 처리 실패는 `{대상}_{결과}`(`CSV_PARSE_ERROR`, `AI_ANALYSIS_TIMEOUT`), 입력·계정 상태는 `{상태}_{대상}`(`EMPTY_FILE`, `DUPLICATE_EMAIL`, `INACTIVE_ACCOUNT`).
- 한 기능·외부 연동에 속한 코드는 같은 접두사로 묶는다: `CSV_`, `AI_ANALYSIS_`, `KAKAO_`.
- `code` 문자열은 상수 이름과 똑같이 쓴다. 응답의 `code` 필드로 그대로 나간다.

### 2-2. 메시지

- 한국어 합니다체 문장으로 쓰고 마침표로 끝낸다. 응답 `message`로 사용자에게 그대로 보이므로 예외 메시지·파일 경로 같은 내부 정보를 넣지 않는다.
- 사용자가 할 일이 있으면 덧붙인다. 예: `SCORE_NOT_READY`의 "데이터를 먼저 업로드해 주세요."
- 메시지는 고정 문자열이다. `BusinessException`이 포맷 인자를 받지 않으므로 ID·파일명 같은 값을 끼워 넣지 않는다.

### 2-3. HTTP 상태 선택 기준

Spring `HttpStatus` 상수를 그대로 쓴다.

| 상태 | 언제 | 현재 코드 |
| --- | --- | --- |
| 400 `BAD_REQUEST` | 요청 값·파일 형식이 잘못됨 | `VALIDATION_ERROR`, `EMPTY_FILE`, `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `CSV_INVALID_HEADER`, `CSV_NO_VALID_ROWS`, `CSV_PARSE_ERROR` |
| 401 `UNAUTHORIZED` | 인증 실패 (토큰, 로그인 정보, 카카오 인가 코드) | `UNAUTHORIZED`, `INVALID_TOKEN`, `EXPIRED_TOKEN`, `INVALID_LOGIN_CREDENTIALS`, `KAKAO_AUTH_FAILED` |
| 403 `FORBIDDEN` | 본인 확인은 됐지만 계정을 쓸 수 없음 | `INACTIVE_ACCOUNT` |
| 404 `NOT_FOUND` | 리소스가 없거나 본인 소유가 아님 | `USER_NOT_FOUND`, `STORE_NOT_FOUND`, `UPLOAD_NOT_FOUND`, `SCORE_NOT_READY` |
| 409 `CONFLICT` | 이미 있는 값과 충돌 | `DUPLICATE_EMAIL` |
| 500 `INTERNAL_SERVER_ERROR` | 서버 내부 오류, 외부 연동 장애 | `INTERNAL_SERVER_ERROR`, `FILE_STORAGE_ERROR`, `AI_ANALYSIS_*`, `KAKAO_TOKEN_REQUEST_FAILED`, `KAKAO_USER_INFO_REQUEST_FAILED` |

- 남의 리소스에 접근하면 403이 아니라 `*_NOT_FOUND`(404)로 응답한다. 매장은 `StoreQuery.getOwnedStore()`가 `findByIdAndUserId`로 찾지 못하면 `STORE_NOT_FOUND`, 업로드는 `StoreQuery.isOwnedBy()`가 false면 `UPLOAD_NOT_FOUND`를 던진다.
- 외부 서버 장애는 500으로 둔다. 외부 서버가 사용자 입력을 거절한 경우만 4xx로 둔다 (`KAKAO_AUTH_FAILED`).

---

## 3. 새 에러 코드 추가 순서

1. 기존 코드로 표현되는지 먼저 본다. 예: 파일 검증은 sales·upload가 `EMPTY_FILE`·`UNSUPPORTED_FILE_TYPE`을 같이 쓴다.
2. `global/exception/ErrorCode.java`의 enum 끝에 기능 단위로 묶어 추가한다(`AI_ANALYSIS_*`, `KAKAO_*`가 이렇게 추가됐다). 기존 마지막 상수의 `;`는 `,`로 바꾼다. 인자 세 개는 한 줄에 하나씩 12칸 들여 쓰고, 상수 사이에 빈 줄을 하나 둔다.
3. 던지는 곳에서 `throw new BusinessException(ErrorCode.새_코드)`를 쓴다.
4. `GlobalExceptionHandler`·`JwtAuthenticationEntryPoint`는 고치지 않는다. 상태와 본문을 `ErrorCode`에서 읽는다.

---

## 4. 예외 던지기

- 업무 오류는 `new BusinessException(ErrorCode)`로 던진다. 생성자는 이것 하나이고, 하위 클래스는 만들지 않는다. 예외 메시지는 `ErrorCode.message`가 된다.
- 서비스·외부 API 클라이언트·파일 저장소(`global.storage`)에서 던진다. 컨트롤러는 서비스만 호출하고 예외를 다루지 않는다.
- 체크 예외·라이브러리 예외는 잡아서 알맞은 `ErrorCode`로 바꿔 던진다. `BusinessException`은 원인 예외(cause)를 받지 않으므로, 바꾸면 원래 스택트레이스는 남지 않는다.

```java
// 조회 실패 — store/internal/service/StoreQueryService.java
Store store = storeRepository.findByIdAndUserId(storeId, userId)
        .orElseThrow(() ->
                new BusinessException(ErrorCode.STORE_NOT_FOUND));

// 하위 예외 변환 — sales/internal/service/SalesCsvParser.java
} catch (IOException | CsvValidationException e) {
    throw new BusinessException(ErrorCode.CSV_PARSE_ERROR);
}
```

- 클래스 안에서만 쓰는 표준 예외는 밖으로 내보내지 않는다. 예: `SalesCsvParser.toRow()`의 `IllegalArgumentException`은 `parse()`가 잡아 건너뛴 행 수만 센다. 밖으로 나간 표준 예외는 500이 된다(5절).

---

## 5. GlobalExceptionHandler와 응답 형식

| 처리 예외 | 상태 | `code` | 로그 |
| --- | --- | --- | --- |
| `BusinessException` | `ErrorCode.status` | `ErrorCode.code` | 남기지 않음 |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` | 남기지 않음 |
| `Exception` (그 외 전부) | 500 | `INTERNAL_SERVER_ERROR` | `log.error("Unexpected server error", exception)` |

본문은 `ErrorResponse.of(errorCode)` 또는 `ErrorResponse.validation(errorCode, errors)`로 만든다. 직접 `new` 하지 않는다.

```json
{
  "code": "STORE_NOT_FOUND",
  "message": "매장을 찾을 수 없습니다.",
  "errors": null,
  "timestamp": "2026-11-08T14:30:00.123456"
}
```

- `code`·`message`는 `ErrorCode` 값이다. 원래 예외 메시지는 내보내지 않는다.
- `errors`는 Validation 오류일 때만 값이 있고, 그 외에는 `null`로 내려간다(`@JsonInclude` 설정 없음).
- `timestamp`는 응답을 만든 서버 시각(`LocalDateTime.now()`)이다.

> 전용 핸들러가 없는 Spring MVC 예외(잘못된 JSON 본문, 필수 `@RequestParam` 누락, 경로 변수 타입 불일치, multipart 크기 초과 등)도 `Exception` 핸들러로 가서 500 `INTERNAL_SERVER_ERROR`로 응답한다.

---

## 6. Validation 오류 응답

- 요청 본문은 `@Valid @RequestBody`와 요청 DTO(record)의 jakarta 제약으로 검증한다. 제약마다 한국어 `message`를 적는다. 예: `SignupRequest`의 `@NotBlank(message = "이메일은 필수입니다.")`.
- 핸들러가 `FieldError`를 `필드명 → 메시지` Map(`LinkedHashMap`)으로 모은다. 한 필드에 오류가 여러 개면 하나만 담는다(`putIfAbsent`).

```json
{
  "code": "VALIDATION_ERROR",
  "message": "입력값이 올바르지 않습니다.",
  "errors": {
    "email": "올바른 이메일 형식이 아닙니다.",
    "password": "비밀번호는 8자 이상 20자 이하여야 합니다."
  },
  "timestamp": "2026-11-08T14:30:00.123456"
}
```

- `@RequestParam`·`@PathVariable`·multipart 파일에는 Bean Validation을 쓰지 않는다(`@Validated` 없음). 파일은 서비스에서 직접 검증해 `BusinessException`을 던진다 (`UploadService.validateVideoFile()`, `SalesService.validateCsvFile()`).

---

## 7. 인증·인가 오류 응답

필터 단계 오류는 `DispatcherServlet`에 닿기 전이라 `GlobalExceptionHandler`가 잡지 못한다. 그래서 필터와 EntryPoint가 나눠 처리한다.

1. `JwtAuthenticationFilter`는 토큰 검증에 실패해도 예외를 던지지 않는다. request attribute `"exception"`에 `ErrorCode`를 담고 다음 필터로 넘긴다.
2. 인증이 필요한 URL이면 Spring Security가 `JwtAuthenticationEntryPoint`를 부른다. attribute의 `ErrorCode`(없으면 `UNAUTHORIZED`)로 상태를 정하고, `ErrorResponse.of()`를 `ObjectMapper`로 직접 써서 5절과 같은 JSON을 응답한다.
3. `SecurityConfig.PUBLIC_URLS`(`/api/auth/**`, Swagger 경로, `/error`)에서는 토큰이 잘못돼도 attribute만 남고 요청은 그대로 처리된다.

| 상황 | 발생 위치 | ErrorCode | 상태 |
| --- | --- | --- | --- |
| 토큰 없음 | EntryPoint 기본값 | `UNAUTHORIZED` | 401 |
| 만료 (`ExpiredJwtException`) | Filter | `EXPIRED_TOKEN` | 401 |
| 서명·형식 오류 (`JwtException`, `IllegalArgumentException`) | Filter | `INVALID_TOKEN` | 401 |
| 토큰의 사용자가 없음 / 비활성 계정 | `PrincipalLoader` → Filter | `USER_NOT_FOUND` / `INACTIVE_ACCOUNT` | 404 / 403 |
| 이메일·비밀번호 불일치 | `AuthService` → Handler | `INVALID_LOGIN_CREDENTIALS` | 401 |

- 필터 단계에서 인증 오류를 추가할 때도 attribute에 `ErrorCode`를 담는 방식을 따른다. 필터에서 응답을 직접 쓰지 않는다.
- 역할 기반 인가는 없다(`anyRequest().authenticated()`만 쓴다). `AccessDeniedHandler`도 등록하지 않았다. 리소스 접근 권한은 서비스의 소유권 확인으로 처리하고 404로 응답한다(2-3절).

---

## 8. 외부 API 오류 처리

외부 호출은 각 모듈 `internal/client/`의 클라이언트가 맡는다. 공용 `RestClient` 빈은 `RestClientConfig`에 있다.

| 클라이언트 | 실패 상황 | ErrorCode |
| --- | --- | --- |
| `KakaoApiClient` (user) | 토큰 요청이 4xx (`HttpClientErrorException`), 즉 인가 코드 무효 | `KAKAO_AUTH_FAILED` (401) |
| | 토큰 요청의 그 밖의 `RestClientException`, 응답에 `accessToken` 없음 | `KAKAO_TOKEN_REQUEST_FAILED` (500) |
| | 사용자 정보 조회의 `RestClientException`, 응답에 `id` 없음 | `KAKAO_USER_INFO_REQUEST_FAILED` (500) |
| `FastApiVideoAnalysisClient` (visitor) | 제출 응답에 `jobId` 없음, 폴링 응답 `null`, `status`가 `FAILED`, 대기 중 인터럽트 | `AI_ANALYSIS_FAILED` (500) |
| | 폴링 `MAX_POLL_ATTEMPTS`(3초 간격 200회) 초과 | `AI_ANALYSIS_TIMEOUT` (500) |

- 요청 처리 중에 부르는 외부 API(카카오)는 실패를 클라이언트 안에서 `BusinessException`으로 바꾼다. 라이브러리 예외(`RestClientException`)를 서비스로 올리지 않는다.
- 상대가 사용자 입력을 거절한 경우(4xx)와 연동 장애(5xx·네트워크·빈 응답)를 나눠 다른 코드로 바꾼다. 응답이 `null`이거나 꼭 필요한 값이 없으면 실패로 본다.
- 바꾸기 전에 `log.warn`으로 원인을 남긴다. `BusinessException`에는 원인이 실리지 않기 때문이다.
- `InterruptedException`을 잡으면 `Thread.currentThread().interrupt()`로 인터럽트 상태를 되돌린다.

```java
// user/internal/client/KakaoApiClient.java
} catch (HttpClientErrorException e) {
    log.warn("카카오 인가 코드 검증 실패: status={}", e.getStatusCode());
    throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
} catch (RestClientException e) {
    log.warn("카카오 토큰 요청 실패", e);
    throw new BusinessException(ErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
}
```

- AI 분석은 비동기 리스너 안에서 돌기 때문에 `AI_ANALYSIS_*` 코드는 HTTP 응답으로 나가지 않는다. 실패는 9절처럼 업로드 상태로 기록된다.

---

## 9. 이벤트 리스너에서의 예외 처리

리스너는 발행 트랜잭션이 커밋된 뒤 별도 스레드에서 돈다(`architecture.md` 5-2절). HTTP 요청 밖이라 `GlobalExceptionHandler`가 적용되지 않고, 업로드 API는 이미 성공 응답을 보낸 상태다.

| 리스너 | 방식 | 실패 처리 |
| --- | --- | --- |
| `ScoreRecalculationListener` (score) | `@ApplicationModuleListener` | 예외를 삼키지 않는다. `ScoreService.recalculate()`가 던지면 `EVENT_PUBLICATION`이 미완료로 남는다 |
| `VideoAnalysisService.on()` (visitor) | `@Async("analysisExecutor") @TransactionalEventListener` | 모든 예외를 잡아 `log.error("영상 분석 실패: uploadId={}", uploadId, e)`로 남기고 `uploadTracker.markFailed()`로 업로드를 `FAILED`로 기록한다 |

- 기본은 삼키지 않는다. 삼키면 발행 기록이 완료로 표시돼 실패를 추적할 수 없다.
- 잡는 경우는 실패를 업무 상태로 남길 때뿐이다. 이때 `log.error`에 예외 객체를 넘겨 스택트레이스를 남긴다.
- `markFailed()`에 넘긴 메시지는 업로드 상태 조회(`GET /api/uploads/{uploadId}`)의 `errorMessage`로 사용자에게 보인다. 내부 정보를 넣지 않는다.

---

## 10. Swagger(OpenAPI) 명세

### 10-1. 전역 설정

- `OpenApiConfig`가 `OpenAPI` 빈으로 문서 정보(`FinSight API`, `v1.0.0`)와 `bearerAuth`(HTTP bearer, JWT) 보안 스킴을 등록하고 모든 API에 전역으로 적용한다.
- 문서 경로는 `application.yml`의 `springdoc`에서 정한다(`/v3/api-docs`, `/swagger-ui.html`). 두 경로는 `SecurityConfig.PUBLIC_URLS`에 있다.

### 10-2. 컨트롤러 어노테이션

Swagger 어노테이션은 컨트롤러 클래스와 핸들러 메서드에 직접 붙인다. 문서용 인터페이스를 따로 두지 않는다.

| 위치 | 어노테이션 | 규칙 |
| --- | --- | --- |
| 컨트롤러 클래스 | `@Tag(name, description)` | `name`은 영문 PascalCase 한 단어(`Store`, `Auth`), `description`은 "… API" |
| 핸들러 메서드 | `@Operation(summary, description)` | 모든 핸들러에 붙인다. `summary`는 명사형("매장 등록"), `description`은 합니다체 문장으로 동작과 접근 조건("본인 소유 매장만 …")을 쓴다 |
| `@PathVariable`, 파일이 아닌 `@RequestParam` | `@Parameter(description, example)` | 예: `@Parameter(description = "매장 ID", example = "1")` |
| 요청·응답 DTO | `@Schema(description, example)` | record 타입과 각 필드에 붙인다 |

```java
// store/internal/controller/StoreController.java (클래스에 @Tag(name = "Store", description = "매장 관리 API"))
@Operation(
        summary = "매장 상세 조회",
        description = "매장 ID로 상세 정보를 조회합니다. 본인 소유 매장만 조회할 수 있습니다."
)
@GetMapping("/{storeId}")
public ResponseEntity<StoreResponse> getStore(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @Parameter(description = "매장 ID", example = "1")
        @PathVariable Long storeId
) { ... }
```

### 10-3. 오류 응답 문서화

- 컨트롤러에 `@ApiResponse`/`@ApiResponses`를 쓰지 않는다. 엔드포인트별 오류 코드는 Swagger에 나오지 않는다.
- `GlobalExceptionHandler` 메서드에 `@ResponseStatus`가 없어 springdoc이 전역 오류 응답도 붙이지 않는다. `ErrorResponse`의 `@Schema`는 지금 문서에 나타나지 않는다.
- 오류 응답 문서화를 도입하려면 방식을 먼저 정한다. 일부 컨트롤러에만 따로 붙이지 않는다.
