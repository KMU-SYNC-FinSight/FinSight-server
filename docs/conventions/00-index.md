# FinSight — 컨벤션 인덱스

Java 17 + Spring Boot 3.5 + Spring Modulith 기반 모듈러 모놀리스의 개발 컨벤션을 용도별 문서로 나눴다. 작업 종류에 맞는 문서만 읽으면 된다.

**핵심 전제**
- 소상공인(`OWNER`)이 매장을 등록하고 매출 CSV·매장 영상을 올리면, 매출·방문객 지표로 운영 점수를 계산해 소상공인용 대시보드와 금융기관용 리포트로 보여준다.
- 언어 Java 17, DB MySQL 8.x. 스키마는 JPA `ddl-auto: update`로 관리한다(Flyway 없음).
- 단일 Gradle 모듈. `com.finsight` 바로 아래 패키지 하나가 Spring Modulith 모듈 하나이고, 경계는 `ModularityTests`(`verify()`)로 강제한다.
- 모듈 간 동기 조회는 공개 API(`…Query`), 다른 모듈의 후속 처리는 이벤트(`@ApplicationModuleListener`, `EVENT_PUBLICATION` 기록)로 한다.
- 인가는 역할이 아니라 매장 소유권(`StoreQuery`)으로 한다.

| 문서 | 다루는 내용 | 언제 참조하는가 |
| --- | --- | --- |
| [`architecture.md`](./architecture.md) | 모듈 구성, 의존 방향, 패키지 구조(`internal`), 경계 검증, 모듈 간 통신(공개 API·이벤트·`EVENT_PUBLICATION`) | 새 모듈·공개 API·이벤트 설계, 의존성 리뷰 |
| [`coding-style.md`](./coding-style.md) | 네이밍, Entity/Repository/DTO/공개 record/Service/Controller 패턴, 정적 팩토리·Lombok, 트랜잭션 위치, 외부 클라이언트, Validation | 실제 코드 작성·리뷰 |
| [`error-handling.md`](./error-handling.md) | `ErrorCode`/`BusinessException`, `GlobalExceptionHandler`와 오류 응답, 인증·외부 API·이벤트 리스너 오류, Swagger 명세 | 에러 코드 추가, 예외 처리, API 문서화 |
| [`config-and-auth.md`](./config-and-auth.md) | `application.yml`·환경변수, `@Value` 바인딩, 파일 저장소 전환, JWT 인증 흐름, 매장 소유권 인가 | 설정값 추가, 인증·인가 작업 |
| [`logging.md`](./logging.md) | `@Slf4j`, 로그 레벨, 메시지 형식, 예외 로그, 출력 설정 | 로그 추가, 예외를 잡아 바꿔 던질 때 |
| [`git-convention.md`](./git-convention.md) | 커밋·브랜치·PR·이슈·라벨 규칙, CI 필수 체크(`verify()`) | 커밋, 브랜치, 이슈, PR |

## 빠른 참조

- "새 기능 모듈 추가" → `architecture.md` 2·3절(모듈·패키지) → `coding-style.md` 1-1절(클래스 이름·위치)
- "다른 모듈 데이터가 필요" → `architecture.md` 4-2절(공개 API·`allowedDependencies`) + `coding-style.md` 2-4절(공개 record)
- "A 모듈 변화에 B가 반응" → `architecture.md` 5-2절(이벤트)
- "오래 걸리는 외부 호출(영상 분석 등)" → `architecture.md` 5-2절 예외 규칙 + `coding-style.md` 2-9절(트랜잭션 나누기)
- "엔티티·컬럼 추가" → `coding-style.md` 2-1절(`ddl-auto`가 반영하지 않는 변경 주의)
- "객체를 어떻게 생성하지 / Lombok 어디까지" → `coding-style.md` 2-7·2-8절
- "에러 코드 추가" → `error-handling.md` 2·3절
- "외부 API 실패 처리" → `error-handling.md` 8절
- "이벤트 리스너에서 예외가 나면" → `error-handling.md` 9절
- "Swagger 명세 다는 곳" → `error-handling.md` 10절
- "설정값·환경변수 추가" → `config-and-auth.md` 2·3절
- "로그인 사용자 받기 / 매장 소유권 확인" → `config-and-auth.md` 6절·7-1절
- "인증 없이 열어야 하는 API" → `config-and-auth.md` 7-2절
- "로그를 남길지, 어떤 레벨로" → `logging.md` 3·5절
- "커밋·브랜치·PR·라벨" → `git-convention.md`

## 모듈 한눈에 보기

```
com.finsight
├── global      # 공용 설정·예외·보안·파일 저장소 (OPEN). 도메인 모듈을 참조하지 않는다
├── user        # 회원가입, 로그인(이메일·카카오), JWT 발급 — PrincipalLoader 구현
├── store       # 매장 — StoreQuery, StoreSummary
├── upload      # 업로드 이력·상태 — UploadTracker, UploadType, VideoUploadedEvent
├── sales       # 매출 CSV — SalesMetricQuery, DailySales, SalesDataChangedEvent
├── visitor     # 영상 AI 분석, 방문객 지표 — VisitorMetricQuery, VisitorMetricSnapshot, VisitorDataChangedEvent
├── score       # 운영 점수 — ScoreQuery, ScoreSnapshot, Grade, RiskLevel
├── dashboard   # 소상공인용 대시보드 조회
└── report      # 금융기관용 리포트 조회

각 모듈: {공개 API} + internal/{controller, service, domain, repository, dto, client, listener}
```
