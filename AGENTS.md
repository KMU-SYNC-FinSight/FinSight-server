# AGENTS.md

AI 코딩 에이전트가 이 저장소에서 작업할 때 참고하는 가이드다.

Java 17 + Spring Boot 3.5 + Spring Modulith 기반 모듈러 모놀리스(단일 Gradle 모듈). 소상공인 매장의 매출·방문객 데이터로 운영 점수를 산출해 소상공인용 대시보드와 금융기관용 리포트를 제공한다. DB는 MySQL 8.x이고, 스키마는 JPA `ddl-auto: update`로 관리한다(Flyway 없음).

## 작업 원칙

- 구현 전에 확실하지 않은 것은 반드시 사용자에게 질문한다.
- 요구사항이 여러 방식으로 해석될 수 있으면 임의로 선택하지 말고 선택지를 제시하고 물어본다.
- 컨벤션 문서에 없는 새로운 패턴을 도입해야 할 때, 어느 모듈/패키지에 둘지 애매할 때도 먼저 확인한다.
- 요청받은 범위만 수정한다. 인접 코드 개선·리팩토링은 임의로 하지 않는다.
- 테스트 코드는 지시하는 사람이 명시적으로 요청하지 않는 이상 작성하지 않는다.

## 모듈 구조

```
com.finsight
├── global      # 공용 설정·예외·보안·파일 저장소 (OPEN 모듈, 도메인 모듈을 참조하지 않음)
├── user        # 회원가입, 로그인(이메일·카카오), JWT 발급, 내 정보
├── store       # 매장 등록·조회, 소유권 확인
├── upload      # 파일 저장, 업로드 이력·처리 상태
├── sales       # 매출 CSV 파싱·저장
├── visitor     # 영상 AI 분석, 방문객 지표
├── score       # 운영 점수 계산
├── dashboard   # 소상공인용 대시보드 조회
└── report      # 금융기관용 리포트 조회
```

각 모듈의 최상위 패키지에는 다른 모듈이 쓰는 공개 API(인터페이스·record·이벤트)만 두고, 구현(controller·service·domain·repository·dto)은 `internal/` 아래에 둔다.
모듈 경계(순환 의존, 다른 모듈 `internal` 참조, `allowedDependencies` 밖 의존)는 `ModularityTests`(Spring Modulith `verify()`)로 강제한다. 모듈 구조를 건드렸으면 아래 테스트로 확인한다. DB 없이 돈다.

```
./gradlew test --tests com.finsight.ModularityTests
```

## 컨벤션 문서

작업 종류에 맞는 문서만 골라서 읽는다. 전체 인덱스와 "빠른 참조"는 `docs/conventions/00-index.md` 참고.

| 상황 | 문서 |
| --- | --- |
| 모듈 설계·의존성, 모듈 간 통신(공개 API·이벤트) | `architecture.md` |
| 코드 작성 (네이밍, DTO/Entity/Service 등) | `coding-style.md` |
| 에러 코드·예외 처리, Swagger 명세 | `error-handling.md` |
| 설정값·인증/인가 | `config-and-auth.md` |
| 로깅 | `logging.md` |
| 커밋/브랜치/이슈/PR | `git-convention.md` |

## 스킬

| 스킬 | 용도 |
| --- | --- |
| `/convention` | 컨벤션 문서를 골라 읽고 코드를 작성·리뷰한다 |
| `/git` | 커밋·브랜치·이슈·PR을 컨벤션대로 만든다 |
| `/planmd` | 구현 전에 `docs/plans/`에 계획 문서를 쓴다 (호출할 때만) |
| `/pr-check` | 현재 브랜치 PR의 리뷰 코멘트를 중요도 순으로 정리한다 |

## Git 컨벤션

자세한 내용은 `docs/conventions/git-convention.md` 참고.

- 커밋: `type: 제목` (한글, 마침표 없음). type은 `feat`/`fix`/`refactor`/`docs`/`test`/`chore`/`init`
- 작업 단위별로 커밋을 나눈다
- 커밋 메시지·PR 본문에 `Co-Authored-By: Claude ...`, `Claude-Session: ...` 등 AI 트레일러를 넣지 않는다
- 브랜치: `{type}/#{이슈번호}-{작업내용}`
- PR 제목: `[{Type}/#{이슈번호}] {설명}`, Squash Merge 기본, `main` 직접 push 금지
