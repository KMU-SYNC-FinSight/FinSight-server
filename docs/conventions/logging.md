# Logging — 로깅 컨벤션

> 로거 선언, 로그 레벨 선택, 예외 로그, `application.yml`의 logging 설정을 다룬다.
> 로그를 추가하거나 예외를 잡아 바꿔 던지는 코드를 작성할 때 참조한다. 예외 변환 규칙 자체는 `error-handling.md`를 따른다.

---

## 1. 로거 선언

로깅 전용 모듈·필터는 없다. 로그가 필요한 클래스에 Lombok `@Slf4j`를 붙이고 `log`를 쓴다. `LoggerFactory`를 직접 만들지 않는다.

| 클래스 | 남기는 로그 |
| --- | --- |
| `global.exception.GlobalExceptionHandler` | 처리하지 못한 예외 (ERROR) |
| `user.internal.client.KakaoApiClient` | 카카오 토큰·사용자 정보 API 실패 (WARN) |
| `visitor.internal.client.FastApiVideoAnalysisClient` | AI 분석 제출·완료 (INFO), 임시 파일 삭제 실패 (WARN) |
| `visitor.internal.service.VideoAnalysisService` | 영상 분석 완료 (INFO), 실패 (ERROR) |
| `score.internal.service.ScoreService` | 운영 점수 재계산 결과 (INFO) |

> `S3FileStorage`의 빈 활성화 출력만 `System.out.println`으로 남아 있다. 새 로그는 `log`로 남긴다.

---

## 2. 요청 추적

- MDC, 요청 ID, access log 필터는 없다. Tomcat access log 설정(`server.tomcat.accesslog`)도 없다.
- 로그끼리는 메시지에 넣은 식별자(`uploadId`, `storeId`, `jobId`)로 이어 본다. 영상 업로드 → 분석 → 점수 재계산 흐름은 `uploadId`와 `storeId`로 따라간다.
- 비동기 로그는 스레드 이름으로 구분된다. `AsyncConfig`가 `taskExecutor`에 `event-`, `analysisExecutor`에 `analysis-` 접두사를 붙인다.

---

## 3. 로그 레벨 선택

| 레벨 | 쓰는 상황 | 예 |
| --- | --- | --- |
| `ERROR` | 처리하지 못한 예외(500 응답), 비동기 작업 실패 | `GlobalExceptionHandler`, `VideoAnalysisService` |
| `WARN` | 외부 API 실패를 `BusinessException`으로 바꿔 던질 때, 본 흐름에 영향 없는 부수 작업 실패 | `KakaoApiClient`, 임시 파일 삭제 실패 |
| `INFO` | 비동기 작업 단계의 완료·결과 | AI 분석 제출·완료, 운영 점수 재계산 |
| `DEBUG` / `TRACE` | 애플리케이션 코드에서는 쓰지 않는다 | yml의 Hibernate 로그만 (6절) |

- 요청마다 남기는 INFO 로그는 없다. 요청을 처리하는 Controller·Service에는 로그가 없고, 외부 API 클라이언트의 실패 로그만 있다.

---

## 4. 메시지 형식

- SLF4J placeholder(`{}`)를 쓴다. 문자열을 `+`로 이어 붙이지 않는다.
- 한국어로 `무엇: key={}, key={}` 형태로 쓴다. (`GlobalExceptionHandler`의 `"Unexpected server error"`만 영어다.)
- 식별자와 상태 코드만 남긴다. 토큰, 인가 코드, 파일 내용, 개인정보는 남기지 않는다.

```java
// score — ScoreService.recalculate
log.info("운영 점수 재계산: storeId={}, score={}, grade={}", storeId, r.total(), r.grade());
```

---

## 5. 예외 로그

| 예외 (`GlobalExceptionHandler`) | 응답 | 로그 |
| --- | --- | --- |
| `BusinessException` | `ErrorCode`의 상태 | 없음 |
| `MethodArgumentNotValidException` | 400 `VALIDATION_ERROR` | 없음 |
| 그 외 `Exception` | 500 `INTERNAL_SERVER_ERROR` | `log.error("Unexpected server error", exception)` |

- `BusinessException`에는 원인 예외가 실리지 않는다. 원인 예외를 바꿔 던질 때는 그 자리에서 로그를 남긴다. 예상한 실패(카카오 4xx = 잘못된 인가 코드)는 상태 코드만, 예상하지 못한 실패는 예외 객체까지 남긴다.
- 예외 객체는 placeholder 없이 마지막 인자로 넘긴다. 그래야 스택 트레이스가 찍힌다.

```java
// user — KakaoApiClient.getAccessToken
} catch (HttpClientErrorException e) {
    log.warn("카카오 인가 코드 검증 실패: status={}", e.getStatusCode());
    throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
} catch (RestClientException e) {
    log.warn("카카오 토큰 요청 실패", e);
    throw new BusinessException(ErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
}
```

- 실패를 업무 상태로 기록하고 예외를 삼킬 때는 ERROR로 남긴다. `VideoAnalysisService`가 `log.error("영상 분석 실패: uploadId={}", uploadId, e)` 후 업로드를 `FAILED`로 기록한다 (`architecture.md` 5-2절).
- 그 밖의 이벤트 리스너(`ScoreRecalculationListener` → `ScoreService.recalculate`)는 예외를 잡지 않고 던진다. 실패는 `EVENT_PUBLICATION`에 미완료로 남는다.

> `LocalFileStorage`·`S3FileStorage`는 `IOException` 등을 원인·로그 없이 `FILE_STORAGE_ERROR`(500)로 바꿔 던진다. 지금은 이 실패의 원인이 어디에도 남지 않는다.

---

## 6. 출력 설정

`logback-spring.xml`이 없어 Spring Boot 기본 설정을 쓴다. 콘솔 텍스트 출력만 하고 루트 레벨은 INFO다. 파일 출력, JSON 포맷, 비동기 appender는 없다.

```yaml
# application.yml (spring.jpa.properties.hibernate.format_sql: true 로 SQL은 여러 줄로 출력)
logging:
  level:
    org.hibernate.SQL: debug              # 실행하는 모든 SQL
    org.hibernate.orm.jdbc.bind: trace    # SQL 바인딩 파라미터 값
```

- 프로필이 없으므로(`config-and-auth.md` 1절) 이 설정은 로컬과 배포 환경에 똑같이 적용된다. 운영에서도 모든 SQL과 바인딩 값이 출력된다.
- 바인딩 값에는 이메일, BCrypt 비밀번호 해시, 카카오 사용자 ID, 업로드 파일 경로가 그대로 들어간다.
