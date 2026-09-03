# 인증 사용자 조회 캐싱 (Caffeine) 적용 계획

> 상태: 구현 완료 (4장 코드는 실제 적용된 최종 코드 기준)

## 1. 캐시가 왜 필요한가

FinSight는 JWT 기반 stateless 인증을 쓴다 (`SecurityConfig` — `SessionCreationPolicy.STATELESS`). 요청마다 세션 없이 토큰만으로 인증하기 때문에, `JwtAuthenticationFilter`가 **모든 인증이 필요한 요청마다** 아래 로직을 반복한다.

```
JwtAuthenticationFilter.doFilterInternal()
  → jwtTokenProvider.getUserId(token)                // 토큰에서 꺼냄 (DB 접근 없음)
  → customUserDetailsService.loadUserById(userId)     // UserRepository.findById() → DB 조회
```

`CustomUserDetailsService.loadUserById` (`src/main/java/com/finsight/global/security/CustomUserDetailsService.java`)는 토큰이 이미 서명 검증을 통과한 뒤에도, 단지 `CustomUserPrincipal`(id/email/password/role 4개 필드)을 만들기 위해 매번 MySQL에 왕복한다. 대시보드 하나를 열 때도 내부적으로 여러 API를 호출하는 구조라, 사용자 1명이 몇 초 안에 같은 `userId` 조회를 여러 번 반복하는 경우가 흔하다. 이 조회 결과는

- **자주 읽힌다** (요청마다),
- **거의 바뀌지 않는다** (email/password/role은 로그인 후 웬만해선 고정),
- **작다** (필드 4개짜리 객체)

세 조건을 모두 만족해서 캐싱의 전형적인 대상이다. 캐시를 넣으면 이 경로의 DB 왕복을 없애고, 인증 지연 시간과 DB 커넥션 풀 압박을 동시에 줄일 수 있다.

## 2. Redis vs Caffeine 비교

| 기준 | Redis | Caffeine |
| --- | --- | --- |
| 위치 | 별도 프로세스 (네트워크 캐시) | 애플리케이션 힙 내부 (인메모리 로컬 캐시) |
| 접근 속도 | 네트워크 왕복 필요 (ms 단위) | 메서드 호출 수준 (μs~ns 단위) |
| 인프라 | 별도 서버/컨테이너 구축·운영 필요 | 라이브러리 추가만으로 끝, 별도 인프라 없음 |
| 인스턴스 간 공유 | 가능 (여러 서버가 같은 캐시를 봄) | 불가능 (인스턴스별로 각자 캐시를 가짐) |
| 데이터 정합성 | 여러 인스턴스가 항상 같은 값을 봄 | 인스턴스마다 TTL 만료 시점까지 살짝 다를 수 있음 |
| 영속성 | 옵션에 따라 재시작 후에도 유지 가능 | 없음 (재시작하면 캐시 전부 초기화) |
| 운영 비용 | Redis 서버 배포/모니터링/장애 대응 필요 | 추가 운영 비용 없음 |
| 적합한 상황 | 여러 서버가 캐시를 공유해야 하거나, 세션·랭킹처럼 캐시 자체가 별도 저장소여야 할 때 | 단일 인스턴스, 짧은 TTL로 충분한 로컬 hot data |

## 3. 왜 Caffeine인가

FinSight의 현재 배포 형태와 요구사항을 보면 Redis가 주는 이점(여러 인스턴스 간 캐시 공유, 영속성)이 필요하지 않다.

- `docker-compose.yml`에는 MySQL 컨테이너만 있고, 로드밸런서나 다중 인스턴스 구성이 없다 — **인스턴스가 1개**라 "여러 서버가 같은 캐시를 봐야 한다"는 Redis의 핵심 강점이 무의미하다.
- 캐싱 대상(`CustomUserPrincipal`)은 **인증 요청마다 즉시 다시 만들 수 있는 파생 데이터**다. 캐시가 비어 있거나 유실돼도 DB에서 다시 읽으면 그만이라 영속성이 필요 없다.
- 인증 경로는 **지연 시간에 가장 민감한 구간**이다. 여기서 Redis를 쓰면 "DB 왕복을 없애려고 네트워크 왕복(Redis)을 새로 추가"하는 모순이 생긴다. Caffeine은 같은 프로세스 메모리에서 조회하므로 이 문제가 없다.
- Redis를 쓰려면 컨테이너 추가, 커넥션 풀 설정, 장애 시 fallback 처리 등 운영 부담이 늘어난다. 프로젝트 규모 대비 이득이 크지 않다.

즉, **여러 서버가 공유해야 하는 캐시가 아니라, 각 인스턴스가 자기 자신을 위해 잠깐 들고 있으면 되는 캐시**이므로 Caffeine이 적합하다. (향후 서버를 여러 인스턴스로 스케일아웃하면, 그때는 인스턴스 간 정합성이 필요한 데이터부터 Redis로 옮기는 걸 재검토한다.)

## 4. 구현 계획

### 4.1 의존성 (`build.gradle`)

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'com.github.ben-manes.caffeine:caffeine'
}
```

### 4.2 CacheConfig

`global/config/CacheConfig.java` 신설. `@EnableCaching` + `CaffeineCacheManager` 빈 등록. 캐시 이름은 상수(`USER_PRINCIPAL_CACHE`)로 빼서 사용처(`CustomUserDetailsService`)와 문자열이 어긋나지 않게 한다.

```java
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USER_PRINCIPAL_CACHE = "userPrincipal";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(USER_PRINCIPAL_CACHE);
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(1_000)
                        .expireAfterWrite(Duration.ofMinutes(5))
        );
        return cacheManager;
    }
}
```

- `maximumSize(1_000)`: 서비스 규모상 동시 활성 사용자가 크지 않으므로 메모리 상한을 작게 잡는다.
- `expireAfterWrite(5분)`: role/status 변경이 반영되기까지 최대 5분 지연을 허용하는 대신, 반복 조회를 캐시로 흡수한다. 값이 거의 안 바뀌는 데이터라 5분 정도 지연은 감내 가능하다고 판단.

### 4.3 캐싱 적용 지점

`CustomUserDetailsService.loadUserById`에 `@Cacheable` 적용.

```java
@Cacheable(cacheNames = CacheConfig.USER_PRINCIPAL_CACHE, key = "#userId")
@Transactional(readOnly = true)
public CustomUserPrincipal loadUserById(Long userId) {
    ...
}
```

- 키는 `userId` 하나로 충분 (메서드 파라미터가 이거 하나).
- 예외(`BusinessException`)가 발생하는 경우는 캐싱하지 않음 — Spring Cache는 기본적으로 정상 반환값만 캐싱하고 예외는 캐싱하지 않으므로 별도 처리 불필요.

### 4.4 캐시 무효화

현재 코드베이스에는 사용자 정보(role/status)를 바꾸는 API가 없어서(`UserService`는 조회만 존재) 당장 캐시를 명시적으로 지울 지점이 없다. 대신 `expireAfterWrite(5분)`로 자연 만료시켜 정합성을 보장한다.

추후 프로필 수정·정지·권한 변경 같은 쓰기 API가 생기면, 해당 서비스 메서드에 `@CacheEvict(cacheNames = "userPrincipal", key = "#userId")`를 추가해 즉시 무효화한다.

### 4.5 검증

- 단위 테스트: `CustomUserDetailsService` 캐시 히트 시 `UserRepository.findById`가 두 번째 호출부터 실행되지 않는지 확인 (Mockito `verify(times(1))`).
- 수동 확인: `logging.level.org.hibernate.SQL: debug`가 이미 설정돼 있으므로, 같은 사용자로 연속 API 호출 시 두 번째 호출부터 `select ... from user` 쿼리가 로그에 안 찍히는지 확인.

## 5. 작업 순서

- [x] `build.gradle`에 `spring-boot-starter-cache`, `caffeine` 추가
- [x] `CacheConfig` 작성 (`@EnableCaching`, `CaffeineCacheManager`)
- [x] `CustomUserDetailsService.loadUserById`에 `@Cacheable` 적용
- [x] `compileJava` 통과 확인, 캐시 도입이 기존 테스트 실패와 무관함을 `git stash`로 대조 확인
- [ ] 로컬에서 동일 사용자로 연속 요청 → SQL 로그(`org.hibernate.SQL: debug`)로 두 번째 호출부터 `select ... from user` 쿼리가 안 찍히는지 확인
- [ ] 캐시 히트/미스 검증용 단위 테스트 추가 (`verify(userRepository, times(1)).findById(...)`)
- [ ] 사용자 role/status를 바꾸는 쓰기 API가 생기면 해당 지점에 `@CacheEvict(cacheNames = CacheConfig.USER_PRINCIPAL_CACHE, key = "#userId")` 추가
