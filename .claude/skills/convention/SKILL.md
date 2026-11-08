---
name: convention
description: 이 프로젝트의 코딩 컨벤션을 적용하는 스킬. 코드를 작성하거나 리뷰할 때 /convention으로 호출하면 docs/conventions/ 문서를 읽고 컨벤션에 맞는 코드를 작성한다.
---

# Convention 적용 워크플로우

코딩 작업 전에 이 워크플로우를 따라 컨벤션을 파악하고 적용한다.

## Step 1 — 인덱스 읽기

반드시 `docs/conventions/00-index.md`를 먼저 읽는다.
인덱스에는 작업 유형별로 참조해야 할 문서가 정리되어 있다.

```
Read: docs/conventions/00-index.md
```

## Step 2 — 필요한 문서만 선택적으로 읽기

현재 작업 컨텍스트를 바탕으로 인덱스의 "언제 참조하는가" 열을 보고 필요한 문서만 읽는다.
모든 문서를 한 번에 읽지 않는다.

| 작업 유형 | 읽을 문서 |
|-----------|-----------|
| Controller / Service / Repository / Entity / DTO 작성 | `coding-style.md` |
| 새 모듈·공개 API·이벤트 추가, 모듈 간 의존 변경 | `architecture.md` |
| 새 에러 코드 추가, 예외 처리, Swagger 명세 | `error-handling.md` |
| 인증이 필요한 API, 설정값 추가 | `config-and-auth.md` |
| 로그 추가 | `logging.md` |

인덱스 하단의 "빠른 참조" 섹션도 활용한다.

## Step 3 — 컨벤션 적용 및 자가 검증

코드 작성 완료 후 아래 항목을 확인한다.

- [ ] 클래스 명명 규칙과 패키지 위치를 지켰는가 (`coding-style.md`)
- [ ] 다른 모듈은 최상위 패키지의 공개 API로만 참조했는가 — 다른 모듈의 `internal` 참조, `allowedDependencies` 밖 의존이 없는가 (`architecture.md`)
- [ ] 모듈 구조를 건드렸다면 `./gradlew test --tests com.finsight.ModularityTests`가 통과하는가 (`architecture.md` 4-1절)
- [ ] 에러는 `ErrorCode` + `BusinessException`으로 처리했는가 (`error-handling.md`)
- [ ] 인증 필요 API에서 `@AuthenticationPrincipal CustomUserPrincipal`로 사용자를 받고, 매장 데이터는 소유권을 확인했는가 (`config-and-auth.md`)
- [ ] 엔티티를 추가·변경했다면 `ddl-auto: update`가 처리하지 못하는 변경(컬럼 삭제·타입 축소·기존 데이터와 충돌하는 제약)을 PR에 적었는가
