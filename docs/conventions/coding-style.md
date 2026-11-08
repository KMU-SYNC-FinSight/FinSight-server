# Coding Style — 네이밍 & 클래스 작성 규칙

> 이 문서는 클래스·메서드 네이밍과 Entity/Repository/DTO/공개 API record/Service/Controller 작성 패턴, 객체 생성, Lombok, 트랜잭션 위치, 외부 클라이언트, Validation 규칙을 다룬다.
> 모듈 배치·경계·이벤트는 [`architecture.md`](architecture.md), 예외는 [`error-handling.md`](error-handling.md), 설정·인증은 [`config-and-auth.md`](config-and-auth.md), 로그는 [`logging.md`](logging.md)를 함께 본다.
> 예시는 실제 코드에서 가져왔고 주로 `store` 모듈(`com.finsight.store`)을 쓴다. 긴 부분은 `...`로 줄였다.

---

## 1. 네이밍 컨벤션

### 1-1. 클래스

위치는 `com.finsight.{모듈}` 기준이다([`architecture.md`](architecture.md) 3절).

| 종류 | 네이밍 | 위치 | 예 |
| --- | --- | --- | --- |
| Controller | `{Domain}Controller` | `internal.controller` | `StoreController`, `AuthController` |
| Service | `{Domain}Service` (구체 클래스) | `internal.service` | `StoreService`, `UploadService` |
| 서비스 도우미 | `{대상}{역할}` | `internal.service` | `SalesCsvParser`, `ScoreCalculator` |
| Entity | 도메인 명사 (접미사 없음) | `internal.domain` | `Store`, `DataUpload`, `OperationalScore` |
| Repository | `{Entity}Repository` | `internal.repository` | `StoreRepository`, `DataUploadRepository` |
| 요청 DTO | `{대상}{동작}Request` | `internal.dto` | `StoreCreateRequest`, `KakaoLoginRequest` |
| 응답 DTO | `{대상}{동작}Response` (대상 자체면 `{대상}Response`) | `internal.dto` | `UploadStatusResponse`, `StoreResponse` |
| 외부 API 클라이언트 | `{외부 시스템}{대상}Client` | `internal.client` | `KakaoApiClient`, `FastApiVideoAnalysisClient` |
| 이벤트 리스너 | `{처리}Listener` | `internal.listener` | `ScoreRecalculationListener` |
| 공개 인터페이스 | 조회는 `{대상}Query`, 그 밖은 역할 이름 | 모듈 최상위 | `StoreQuery`, `UploadTracker` |
| 공개 인터페이스 구현 | `{인터페이스}Service` | `internal.service` | `StoreQueryService`, `SalesMetricQueryService` |
| 공개 record | `{대상}Summary`, `{대상}Snapshot` | 모듈 최상위 | `StoreSummary`, `ScoreSnapshot` |
| 이벤트 | `{무엇이}{어떻게 됐다}Event` (과거형) | 모듈 최상위 | `VideoUploadedEvent`, `SalesDataChangedEvent` |

- enum은 공개 API(인터페이스·record·이벤트)에 쓰이면 모듈 최상위(`UploadType`, `Grade`), 모듈 안에서만 쓰면 `internal.domain`(`BusinessType`, `ProcessingStatus`)에 둔다.

### 1-2. 메서드

| 접두사 | 쓰임 | 예 |
| --- | --- | --- |
| `get…` | 조회. 단건은 없으면 `BusinessException`을 던진다 | `StoreQuery.getOwnedStore`, `StoreService.getMyStores` |
| `find…` | 공개 API·Repository에서 없을 수 있는 조회. `Optional`·`List`를 돌려준다 | `ScoreQuery.findByStoreId`, `SalesMetricQuery.findAllByStoreId` |
| `is…` / `exists…` | `boolean` 확인 | `StoreQuery.isOwnedBy`, `UserRepository.existsByEmail` |
| `validate…` | Service 안 `private` 검증. 실패하면 `BusinessException` | `AuthService.validateDuplicateEmail` |
| `to…` | 다른 타입으로 바꾸는 `private` 메서드 | `ScoreService.toSnapshot` |
| `mark…` | 엔티티 상태 전이 | `DataUpload.markCompleted()` |
| `on` | 이벤트 리스너. 이벤트 타입으로 오버로드한다 | `ScoreRecalculationListener.on(SalesDataChangedEvent)` |

- Controller 핸들러와 그것이 부르는 Service 메서드는 이름이 같다(`StoreController.createStore` → `StoreService.createStore`).

---

## 2. 클래스 작성 규칙

### 2-1. JPA Entity

- `internal.domain`에 두고 `global.entity.BaseTimeEntity`(`createdAt`, `updatedAt`)를 상속한다.
- `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. PK는 `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id`.
- 테이블명은 복수형 snake_case(`stores`, `data_uploads`), 여러 단어 필드는 `@Column(name = "seat_count")`로 컬럼명을 적는다. enum 필드는 `@Enumerated(EnumType.STRING)` + `@Column(length = …)`.
- 스키마는 `ddl-auto: update`가 엔티티에서 만든다. `nullable`, `length`, 유니크 제약(`@UniqueConstraint(name = "uk_…")`)을 엔티티에 적는다. `update`는 새 테이블·컬럼만 추가하고 기존 컬럼의 타입·길이 변경은 반영하지 않는다.
- 다른 모듈 데이터는 연관관계 매핑 없이 ID 값(`Long userId`, `Long storeId`)으로만 든다([`architecture.md`](architecture.md) 3절).
- 생성은 정적 팩토리 `create…(...)`(2-7절). 상태 변경은 의도가 드러나는 메서드(`markFailed(...)`, `withdraw()`), 같은 키로 덮어쓰는 갱신은 `update(...)`로 한다.

```java
// com.finsight.store.internal.domain
@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseTimeEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;   // user 모듈의 users.id. 연관관계 없이 값만 든다

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 30)
    private BusinessType businessType;
    // id, name, address, seatCount, openedAt 생략

    public static Store create(Long userId, String name, BusinessType businessType, ...) {
        Store store = new Store();
        store.userId = userId;
        ...
        return store;
    }
}
```

### 2-2. Repository

- `internal.repository`에 `JpaRepository<{Entity}, Long>`를 상속한 인터페이스로 두고, 구현 클래스는 만들지 않는다. 모듈 밖에서는 쓰지 않는다.
- 조회는 Spring Data 메서드 이름 쿼리로 쓴다. 단건은 `Optional<T>`, 여럿은 `List<T>`, 존재 확인은 `boolean existsBy…`.
- 소유권·정렬 조건은 메서드 이름에 넣는다(`StoreRepository.findByIdAndUserId`, `SalesMetricRepository.findByStoreIdOrderByMetricDateAsc`).

### 2-3. DTO (Request / Response)

- API 요청·응답 DTO는 `internal.dto`에 `record`로 선언한다. 클래스에 `@Schema(description)`, 컴포넌트마다 `@Schema(description, example)`를 단다(springdoc).
- Request DTO는 Service에 그대로 넘긴다. Request에 변환 메서드를 두지 않고, Service가 값을 꺼내 엔티티의 `create(...)`에 넘긴다.
- Service는 Response DTO를 돌려주고, 엔티티는 Service 밖으로 나가지 않는다. 변환 팩토리(`from`/`of`)는 2-7절. 여러 출처를 조합하는 Response(`DashboardResponse`, `ReportResponse`, `SalesUploadResponse`)는 팩토리 없이 Service가 표준 생성자로 만든다.
- 엔티티 ID는 `id`가 아니라 `{대상}Id`(`storeId`, `userId`, `uploadId`)로 내보낸다. enum 필드는 enum 타입 그대로 둔다.
- 한 Response 안에서만 쓰는 하위 DTO는 파일을 나누지 않고 중첩 `record`로 둔다(`DashboardResponse.VisitorMetrics`, `KakaoUserInfoResponse.KakaoAccount`).

```java
// com.finsight.store.internal.dto
@Schema(description = "매장 응답")
public record StoreResponse(

        @Schema(description = "매장 ID", example = "1")
        Long storeId,

        @Schema(description = "업종", example = "CAFE")
        BusinessType businessType
        // name, address, seatCount, openedAt 생략
) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(store.getId(), store.getBusinessType(), ...);
    }
}
```

### 2-4. 공개 API record · 이벤트 record

- 다른 모듈에 넘기는 값은 모듈 최상위의 `record`다. 공개 record는 `internal` 타입을 참조하지 않으므로 `from(엔티티)`를 두지 않는다. 구현 Service가 변환하고, 필드가 많으면 `private static toSnapshot(엔티티)`로 뺀다(`ScoreService`, `VisitorMetricService`).
- 이벤트 record는 발행 모듈 최상위에 두고 ID와 값만 담는다. 발행·구독 규칙은 [`architecture.md`](architecture.md) 5-2절.
- 공개 인터페이스·record·이벤트에는 Javadoc을 단다. 이벤트는 언제 발행되고 누가 받는지 적는다(`SalesDataChangedEvent`: 매출 데이터가 바뀐 뒤 발행, 커밋되면 score 모듈이 점수를 다시 계산).

### 2-5. Service

- `@Service` + `@RequiredArgsConstructor` 구체 클래스다. 인터페이스는 공개 API를 구현할 때만 둔다.
- Controller가 부르는 Service는 Request DTO·경로 값과, 로그인이 필요한 API면 `Long userId`를 받아 Response DTO를 돌려준다.
- 매장에 딸린 데이터를 다루면 먼저 `StoreQuery`(`getOwnedStore`, `isOwnedBy`)로 소유권을 확인한다. 다른 모듈은 공개 인터페이스만 주입받는다([`architecture.md`](architecture.md) 5-1절).
- 비즈니스 규칙 검증은 `private validate…`에서, 없는 대상은 `orElseThrow(() -> new BusinessException(ErrorCode.…_NOT_FOUND))`로 처리한다([`error-handling.md`](error-handling.md)).
- 상태 없는 파싱·계산은 `@Component` 도우미로 떼고, 그 반환 묶음은 도우미 안 중첩 `record`로 둔다(`SalesCsvParser.ParseResult`, `ScoreCalculator.ScoreResult`).
- 공개 인터페이스 구현은 Controller가 있는 모듈이면 Controller용 Service와 분리한다(`StoreService` / `StoreQueryService`, `UploadService` / `UploadTrackingService`). Controller가 없는 모듈은 모듈의 Service가 직접 구현한다(`ScoreService implements ScoreQuery`). 구현 메서드에는 `@Override`를 붙인다.

```java
// com.finsight.store.internal.service
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional
    public StoreResponse createStore(Long userId, StoreCreateRequest request) {
        Store store = Store.create(userId, request.name(), request.businessType(), ...);
        Store savedStore = storeRepository.save(store);
        return StoreResponse.from(savedStore);
    }
}
```

### 2-6. Controller

- `@Tag(name, description)` + `@RestController` + `@RequiredArgsConstructor`. 같은 모듈의 Service 하나만 주입받고, 비즈니스 로직과 try-catch를 두지 않는다.
- 경로는 `/api/`로 시작한다. 모든 핸들러가 모듈 소유 경로를 공유하면 클래스에 `@RequestMapping("/api/stores")`를 둔다. 다른 모듈 리소스 아래 경로(`/api/stores/{storeId}/sales`)가 끼면 클래스 매핑 없이 메서드에 전체 경로를 적는다(`SalesController`, `UploadController`).
- 핸들러마다 `@Operation(summary, description)`, `@PathVariable`에는 `@Parameter(description, example)`를 단다.
- 로그인 사용자는 `@AuthenticationPrincipal CustomUserPrincipal principal`로 받아 `principal.getUserId()`만 넘긴다([`config-and-auth.md`](config-and-auth.md)).
- 반환은 `ResponseEntity<{X}Response>`. 새 리소스를 만드는 POST는 `ResponseEntity.status(HttpStatus.CREATED).body(…)`, 나머지는 `ResponseEntity.ok(…)`.
- 요청 바디는 `@Valid @RequestBody`, 파일은 `consumes = MediaType.MULTIPART_FORM_DATA_VALUE` + `@RequestParam("file") MultipartFile`로 받는다.

```java
// com.finsight.store.internal.controller
@Tag(name = "Store", description = "매장 관리 API")
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "매장 등록", description = "로그인한 사용자의 매장을 등록합니다. ...")
    @PostMapping
    public ResponseEntity<StoreResponse> createStore(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody StoreCreateRequest request
    ) {
        StoreResponse response = storeService.createStore(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### 2-7. 객체 생성 — 정적 팩토리와 생성자

| 대상 | 생성 방법 | 예 |
| --- | --- | --- |
| Entity | `protected` 기본 생성자 + `static create(...)`. 생성 경로가 여럿이면 `create{종류}(...)` | `Store.create(...)`, `User.createKakaoUser(...)` |
| 엔티티를 옮기는 Response | 엔티티 하나는 `from`, 엔티티와 다른 값을 함께 받으면 `of` | `StoreResponse.from(store)`, `LoginResponse.of(user, accessToken)` |
| 그 밖의 record | 표준 생성자 `new` | `new StoreSummary(...)`, `new SalesDataChangedEvent(storeId)` |
| record가 아닌 값 객체 | `private` 생성자 + `static of(...)` | `CustomUserPrincipal.of(userId, email, password, role)` |
| 예외 | `new` | `throw new BusinessException(ErrorCode.STORE_NOT_FOUND)` |
| 스프링 빈 | 직접 만들지 않는다. 생성자 주입(2-8절) | |

- 엔티티에는 public 생성자를 두지 않는다. 팩토리 안에서만 `new`로 만들고 필드를 채우므로 생성 경로가 팩토리로 모인다.

### 2-8. Lombok

`build.gradle`에 `compileOnly` + `annotationProcessor`로 들어 있다. `lombok.config`는 없다.

| 어노테이션 | 용도 |
| --- | --- |
| `@RequiredArgsConstructor` | 스프링 빈의 생성자 주입. `final` 필드를 갖는 enum(`ErrorCode`) |
| `@Getter` | 엔티티·`BaseTimeEntity`·`BusinessException`·`ErrorCode`의 접근자. 클래스 단위로 붙인다 |
| `@NoArgsConstructor(access = AccessLevel.PROTECTED)` | JPA 엔티티의 기본 생성자 |
| `@Slf4j` | 로거([`logging.md`](logging.md)) |

- `@Setter`·`@Data`·`@Builder`·`@AllArgsConstructor`는 쓰지 않는다. 상태 변경은 엔티티 메서드로 한다. `record`에는 Lombok을 붙이지 않고, `@Autowired`도 쓰지 않는다.
- 생성자를 직접 쓰는 경우는 `@Value("${…}")`로 설정값을 받을 때다(`KakaoApiClient`, `FastApiVideoAnalysisClient`, `JwtTokenProvider`, `LocalFileStorage`). `lombok.config`가 없어 필드의 `@Value`가 Lombok 생성자 파라미터로 복사되지 않는다. 설정 키는 [`config-and-auth.md`](config-and-auth.md).

### 2-9. 트랜잭션 위치

- `@Transactional`은 `internal.service`의 Service 메서드에만 붙인다. Controller·Repository·클라이언트·리스너 클래스에는 붙이지 않는다.
- 쓰기 메서드는 `@Transactional`, 조회 메서드는 `@Transactional(readOnly = true)`. 조회만 하는 공개 API 구현은 클래스에 `@Transactional(readOnly = true)`를 붙인다(`StoreQueryService`, `SalesMetricQueryService`).
- 전파는 기본값(REQUIRED)만 쓴다. 트랜잭션 안에서 공개 API를 부르면 호출한 쪽 트랜잭션에 묶인다(`SalesService.uploadSalesCsv` → `UploadTracker.recordCompletedCsvUpload`).
- 이벤트는 쓰기 트랜잭션 안에서 `ApplicationEventPublisher.publishEvent()`로 발행한다([`architecture.md`](architecture.md) 5-2절).
- 오래 걸리는 외부 호출은 트랜잭션 밖에서 하고 저장만 짧은 트랜잭션으로 나눈다. 나눌 때는 다른 빈의 `@Transactional` 메서드를 부른다. 같은 클래스 안 호출은 프록시를 거치지 않아 트랜잭션이 걸리지 않는다.

```java
// com.finsight.visitor.internal.service.VideoAnalysisService (try-catch 생략)
@Async("analysisExecutor")
@TransactionalEventListener
public void on(VideoUploadedEvent event) {
    uploadTracker.markProcessing(uploadId);                       // 짧은 트랜잭션, 바로 커밋
    VideoAnalysisResult result =
            videoAnalysisClient.analyze(event.storedFilePath());   // 트랜잭션 밖
    visitorMetricService.saveAnalysisResult(event, result);       // 저장 + COMPLETED 한 트랜잭션
}
```

### 2-10. 외부 클라이언트

- 모듈의 `internal.client`에 `@Component`로 둔다. 외부 응답은 `record`로 받고, 필드명이 snake_case면 `@JsonProperty("access_token")`으로 맞춘다(`KakaoTokenResponse`).
- HTTP 호출은 `global.config.RestClientConfig`가 등록한 `RestClient` 빈을 생성자로 주입받아 쓴다. 클라이언트마다 따로 만들지 않는다.
- 고정된 외부 엔드포인트는 `private static final` 상수(`KakaoApiClient.TOKEN_URI`), 환경마다 다른 값은 `@Value`(`${ai.server.url}`)로 받는다.
- 구현체가 하나면 인터페이스 없이 클래스로 둔다(`KakaoApiClient`). 구현을 바꿔 끼워야 할 때만 인터페이스를 둔다(`VideoAnalysisClient` ← `FastApiVideoAnalysisClient`, `MockVideoAnalysisClient`).
- 응답이 `null`이거나 필수 값이 비면 `BusinessException(ErrorCode.…)`을 던진다(`KakaoApiClient.getUserInfo`의 `response.id() == null` 확인). 외부 오류 처리는 [`error-handling.md`](error-handling.md).

---

## 3. Validation

- `spring-boot-starter-validation`(Jakarta Bean Validation)으로 형식을 검증한다. Request record 컴포넌트에 `@Schema` 다음 줄로 직접 붙이고, Controller에서 `@Valid @RequestBody`로 트리거한다.
- 메시지는 한글로 `{항목}은(는) 필수입니다.`, `{항목}은(는) N자 이하여야 합니다.` 형태로 쓴다.
- 엔티티에 그대로 저장하는 문자열은 `@Size(max)`를 `@Column(length)`와 같은 값으로 붙인다(`StoreCreateRequest.name` ↔ `Store.name`, 100).
- 실패하면 `MethodArgumentNotValidException` → `GlobalExceptionHandler`가 `ErrorCode.VALIDATION_ERROR`(400)와 필드별 메시지로 응답한다([`error-handling.md`](error-handling.md)).
- 파일(`MultipartFile`)과 `@RequestParam` 값에는 Bean Validation을 걸지 않고 Service의 `validate…`에서 검사한다(`UploadService.validateVideoFile`, `SalesService.validateCsvFile`).
- 형식을 넘는 규칙(이메일 중복, 매장 소유권)은 Service에서 `BusinessException`으로 처리한다.

```java
// com.finsight.store.internal.dto
@Schema(description = "매장 등록 요청")
public record StoreCreateRequest(

        @Schema(description = "매장 이름", example = "핀사이트 카페")
        @NotBlank(message = "매장 이름은 필수입니다.")
        @Size(max = 100, message = "매장 이름은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "업종", example = "CAFE")
        @NotNull(message = "업종은 필수입니다.")
        BusinessType businessType
        // address, seatCount, openedAt 생략
) {
}
```
