# Architecture Convention

> FinSight 서버의 모듈 구조와 모듈 간 경계 규칙을 다룬다. Spring Modulith 기반 모듈러 모놀리스.

---

## 1. 기술 스택 (구조 관련)

| 항목 | 버전·용도 |
| --- | --- |
| Spring Boot | 3.5.4 |
| Java | 17 |
| Spring Modulith | 1.4.14 — 모듈 경계 검증(`verify()`), 모듈 간 이벤트 발행 기록 |

---

## 2. 모듈 구성

`com.finsight` 바로 아래 패키지 하나가 모듈 하나다.

| 모듈 | 하는 일 | 테이블 | 공개 API | 발행 이벤트 | 의존 모듈 |
| --- | --- | --- | --- | --- | --- |
| `global` | 공용 설정·예외·보안·파일 저장소 | - | 전체 공개 (OPEN) | - | 없음 |
| `user` | 회원가입, 로그인(이메일·카카오), 토큰 발급, 내 정보 | `users` | 없음 (`PrincipalLoader` 구현) | - | global |
| `store` | 매장 등록·조회, 소유권 확인 | `stores` | `StoreQuery`, `StoreSummary` | - | global |
| `upload` | 파일 저장, 업로드 이력·처리 상태 | `data_uploads` | `UploadTracker`, `UploadType` | `VideoUploadedEvent` | global, store |
| `sales` | 매출 CSV 파싱·저장 | `sales_metrics` | `SalesMetricQuery`, `DailySales` | `SalesDataChangedEvent` | global, store, upload |
| `visitor` | 영상 AI 분석, 방문객 지표 | `visitor_metrics` | `VisitorMetricQuery`, `VisitorMetricSnapshot`, `CongestionLevel` | `VisitorDataChangedEvent` | global, upload |
| `score` | 운영 점수 계산 | `operational_scores` | `ScoreQuery`, `ScoreSnapshot`, `Grade`, `RiskLevel` | - | global, sales, visitor |
| `dashboard` | 소상공인용 대시보드 조회 | - | 없음 | - | global, store, score, sales, visitor |
| `report` | 금융기관용 리포트 조회 | - | 없음 | - | global, store, score |

의존 방향은 한쪽으로만 흐르고 순환이 없다. 아래는 "이 모듈을 누가 쓰는지"다 (global은 모든 모듈이 쓰므로 생략).

```
store   ← upload, sales, dashboard, report
upload  ← sales, visitor
sales   ← score, dashboard
visitor ← score, dashboard
score   ← dashboard, report
user    ← (없음)
```

---

## 3. 패키지 구조

```
com.finsight.{모듈}
├── package-info.java     # @ApplicationModule(displayName, allowedDependencies)
├── {공개 API}             # 다른 모듈이 쓰는 인터페이스·record·enum·이벤트
└── internal/             # 모듈 내부 구현. 다른 모듈은 참조하지 않는다
    ├── controller/
    ├── service/
    ├── domain/           # JPA 엔티티, 내부 enum
    ├── repository/
    ├── dto/              # API 요청·응답 DTO
    ├── client/           # 외부 API 클라이언트 (필요한 모듈만)
    └── listener/         # 다른 모듈 이벤트 구독 (필요한 모듈만)
```

- **공개 API는 모듈 최상위 패키지에만 둔다.** 엔티티와 Repository는 항상 `internal`에 둔다.
- 공개 API는 엔티티 대신 record(`…Summary`, `…Snapshot`)를 돌려준다.
- 다른 모듈의 테이블을 직접 조회하거나 FK로 묶지 않는다. 다른 모듈 데이터는 ID 값(`store_id` 등)으로만 들고 있는다.
- `global`은 OPEN 모듈이라 하위 패키지까지 모두 공개된다. 대신 `global`은 도메인 모듈을 참조하면 안 된다.
  `global`에서 도메인 기능이 필요하면 `global`에 인터페이스를 두고 도메인 모듈이 구현한다.
  (예: `global.security.PrincipalLoader` ← `user.internal.service.UserPrincipalService`)

---

## 4. 경계 강제

### 4-1. 검증 방법

- `ModularityTests`(`src/test/java/com/finsight/ModularityTests.java`)가 `ApplicationModules.verify()`로 아래를 검사한다.
    - 모듈 사이 순환 의존
    - 다른 모듈의 `internal` 패키지 참조
    - `package-info.java`의 `allowedDependencies`에 없는 모듈 참조
- Java 접근 제어로는 `internal`을 막을 수 없다 (Spring이 쓰려면 클래스가 public이어야 한다). **이 테스트가 유일한 방어선**이므로 `gradle check`에서 반드시 통과해야 한다.
- DB나 스프링 컨텍스트 없이 바이트코드만 분석하므로 단독으로 빠르게 돌릴 수 있다.

```
./gradlew test --tests com.finsight.ModularityTests
```

### 4-2. 모듈 간 의존을 새로 추가할 때

1. 의존받는 모듈의 최상위 패키지에 공개 API(인터페이스·record)를 추가한다.
2. 의존하는 모듈의 `package-info.java` `allowedDependencies`에 모듈 이름을 추가한다.
3. 순환이 생기면 이벤트로 방향을 뒤집는다 (5-2절).

---

## 5. 모듈 간 통신

### 5-1. 동기 호출 — 조회, 즉시 결과가 필요한 명령

다른 모듈의 공개 인터페이스를 주입받아 호출한다. 예: 업로드할 때 `StoreQuery.getOwnedStore()`로 매장 소유권 확인.

### 5-2. 이벤트 — 다른 모듈의 후속 처리

- 이벤트 record는 **발행하는 모듈의 최상위 패키지**에 둔다. 이름은 `{무엇이}{어떻게 됐다}Event` (과거형).
- 트랜잭션 안에서 `ApplicationEventPublisher.publishEvent()`로 발행하고, 리스너는 커밋된 뒤에 실행된다.
- 리스너는 기본으로 `@ApplicationModuleListener`를 쓴다 (커밋 후, 비동기, 새 트랜잭션). 기본 실행기는 `taskExecutor`.
- **예외:** 오래 걸리는 외부 호출(영상 AI 분석)은 `@Async("analysisExecutor") @TransactionalEventListener`로 트랜잭션 없이 돌리고,
  저장이 필요한 부분만 짧은 트랜잭션으로 나눈다. `@ApplicationModuleListener`는 리스너 전체를 한 트랜잭션으로 감싸서 DB 연결을 분석 내내 잡고 있게 된다.
- 리스너가 성공해야 발행 기록이 완료로 표시된다. 예외를 삼키면 실패가 기록되지 않으므로 삼키지 않는다
  (실패를 업무적으로 처리한 경우는 예외. 예: 영상 분석 실패를 업로드 `FAILED`로 기록).
- 이벤트 처리는 결과적 일관성이다. 업로드 응답 직후에는 운영 점수가 아직 이전 값일 수 있다.

현재 이벤트 흐름:

| 이벤트 | 발행 | 구독 | 처리 |
| --- | --- | --- | --- |
| `VideoUploadedEvent` | upload | visitor | 영상 AI 분석 → 방문객 지표 저장 → 업로드 COMPLETED |
| `VisitorDataChangedEvent` | visitor | score | 운영 점수 재계산 |
| `SalesDataChangedEvent` | sales | score | 운영 점수 재계산 |

### 5-3. 이벤트 발행 기록 (EVENT_PUBLICATION)

- Spring Modulith가 이벤트 발행을 `EVENT_PUBLICATION` 테이블에 남기고, 리스너가 성공하면 `COMPLETION_DATE`를 채운다.
  실패한 건은 `COMPLETION_DATE`가 비어 있는 채로 남는다.
- 저장소는 **JDBC**를 쓴다. JPA 저장소는 `ddl-auto`가 `serialized_event`를 `VARCHAR(255)`로 만들어 파일 경로가 든 이벤트를 저장하지 못했다.
  JDBC 저장소는 Modulith가 제공하는 MySQL 스키마(`VARCHAR(4000)`)로 테이블을 만든다 (`spring.modulith.events.jdbc.schema-initialization.enabled=true`).
- 미완료 이벤트는 재시작해도 자동으로 다시 처리하지 않는다 (기본값). 완료된 기록도 지우지 않고 쌓이므로, 운영이 길어지면 정리 정책을 정한다.
