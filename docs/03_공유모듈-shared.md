# 03. 공유 모듈 (shared)

모든 도메인이 쓰는 common 패키지(소스 2.2 패키지 트리의 common). 규칙 자체는 02_공통-규칙-global.md에 있고, 이 문서는 그 규칙을 구현하는 부품과 테스트를 다룬다. 04~09 도메인보다 먼저 구현한다.

| 항목 | 내용 |
|---|---|
| 범위 | common 패키지: 보안 설정(JWT 검증), 오류 형식과 공통 예외 처리, Idempotency-Key 필터와 idempotency_key 테이블 · 삭제 스케줄러, 요청 로깅 AOP, 비동기 설정, TimeUtils, BaseTimeEntity, 스웨거 공통 설정, 좌표 변환, 테스트 픽스처 |
| 의존 | 없음 |
| 진행 | 순차 |

## 구조

```text
common
├─ security        SecurityFilterChain, JWT 검증(OAuth2 Resource Server), 인증 주체(주체 종류 · id · familyId · role)
├─ error           ErrorResponse, 공통 예외(HTTP 상태와 code를 가진 BusinessException), @RestControllerAdvice
├─ idempotency     Idempotency-Key 필터, idempotency_key 엔티티 · 저장소, 만료 삭제 @Scheduled 작업
├─ logging         요청 로깅 AOP
├─ async           @EnableAsync, 이벤트용 스레드 풀
├─ time            TimeUtils
├─ geo             좌표 { lat, lng } ↔ JTS Point 변환
├─ swagger         springdoc 공통 설정(dev만)
└─ persistence     BaseTimeEntity
src/test: 공통 테스트 픽스처(Testcontainers PostGIS 기반 클래스, 테스트 토큰)
```

## 테이블

idempotency_key (소스 2.4: 「키 · 요청 해시 · 응답 본문을 저장하고 24시간 뒤 스케줄러가 삭제」)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| (키) | 확인 필요 | 확인 필요 | Idempotency-Key 헤더 값 |
| (요청 해시) | 확인 필요 | | 같은 키로 다른 내용인지 판단 |
| (응답 본문) | 확인 필요(JSONB 가정) | | 처음 응답을 그대로 돌려줄 때 씀 |

컬럼 이름 · 타입 · 응답 코드 저장 여부는 소스에 없다(확인 필요 사항). createdAt은 BaseTimeEntity로 들어오고 24시간 판단에 쓴다.

## API 명세

common에는 엔드포인트가 없다. 아래 부품이 모든 도메인의 API에 걸린다.

| 부품 | 걸리는 곳 |
|---|---|
| JWT 인증 | 인증이 필요한 모든 API. 인증 없이 부르는 API는 각 도메인 API 명세의 「호출」 열에 「인증 없음」으로 적는다 |
| Idempotency-Key 필터 | 각 도메인 API 명세에서 Idempotency-Key가 「필요」인 API |
| 오류 처리 | 모든 API |
| 요청 로깅 AOP | 모든 @RestController 메서드 |

## 핵심 로직

### 오류 형식
- 모든 오류 응답 본문은 `{"error":{"code","message","retryAfterSeconds","fields"}}`. fields는 VALIDATION_FAILED일 때만 채운다.
- 도메인 예외는 HTTP 상태와 code를 가진 공통 예외로 던지고 @RestControllerAdvice가 위 형식으로 바꾼다.
- JSON 파싱 실패는 400 INVALID_REQUEST, Bean Validation 실패는 422 VALIDATION_FAILED.
- 429는 retryAfterSeconds를 채운다.
- 409 STATE_VERSION_CONFLICT 응답에는 error와 함께 최신 세션 뷰가 실린다(pickup이 채움). 실리는 필드 이름은 확인 필요.

### 인증
- Bearer JWT를 검증해 주체 종류(VEHICLE · USER), 주체 id(userId 또는 vehicleId), familyId, role을 꺼낸다. 컨트롤러는 이 인증 주체만 받는다.
- 토큰이 없거나 만료 · 폐기면 401 UNAUTHORIZED, role이 맞지 않으면 403 FORBIDDEN.
- 토큰 발급 · 갱신 · 폐기는 auth(05_인증-auth.md)가 한다. common은 검증만 한다.

### Idempotency-Key
- 대상 API는 헤더가 필수다.
- 처음 요청: 유스케이스를 실행하고 키 · 요청 해시 · 응답을 저장한다.
- 같은 키 · 같은 요청 해시: 유스케이스를 다시 실행하지 않고 저장한 응답을 그대로 돌려준다.
- 같은 키 · 다른 요청 해시: 409 IDEMPOTENCY_KEY_REUSED.
- 저장한 지 24시간이 지난 행은 @Scheduled 작업이 지운다. 서버 1대 전제(2대 이상이면 ShedLock, 소스 8장).

### 시간 · 좌표
- JVM 기본 시간대와 DB 연결 시간대는 UTC로 고정해 변환이 두 번 일어나지 않게 한다.
- TimeUtils: UTC Instant → `+09:00` 문자열, `+09:00` 요청 시각 → UTC Instant, 한국 날짜 기준 오늘(LocalDate).
- 좌표 변환: API `{lat, lng}` ↔ SRID 4326 JTS Point. schedule · place · pickup이 같이 쓴다.

### 로깅
- 02_공통-규칙-global.md 「요청 로깅」 규칙대로 남긴다.

### 시그니처

```java
// common.time
String TimeUtils.toKst(Instant utc);                 // "2026-11-03T18:12:00+09:00"
Instant TimeUtils.toUtc(OffsetDateTime requested);
LocalDate TimeUtils.todayKst(Clock clock);

// common.geo
Point GeoPoints.toPoint(double lat, double lng);     // SRID 4326
LatLng GeoPoints.toLatLng(Point point);

// common.security
record AuthPrincipal(PrincipalType type, long id, Long familyId, Role role) {}   // type: VEHICLE · USER

// common.error
class BusinessException extends RuntimeException { HttpStatus status(); String code(); Integer retryAfterSeconds(); }
```

## 테스트 케이스
- [ ] [기반] BaseTimeEntity를 상속한 엔티티를 저장하면 createdAt과 updatedAt이 채워진다
- [ ] [기반] BaseTimeEntity를 상속한 엔티티를 수정해 저장하면 updatedAt만 바뀌고 createdAt은 그대로다
- [ ] [기반] TimeUtils.toKst는 UTC 2026-11-03T09:12:00Z를 2026-11-03T18:12:00+09:00으로 바꾼다
- [ ] [기반] TimeUtils.toUtc는 2026-11-03T18:12:00+09:00을 UTC 2026-11-03T09:12:00Z로 바꾼다
- [ ] [기반] TimeUtils.todayKst는 UTC 2026-11-05T15:30:00Z 시점에 한국 날짜 2026-11-06을 돌려준다
- [ ] [기반] 서버가 뜨면 JVM 기본 시간대와 DB 연결 시간대가 UTC다
- [ ] [기반] GeoPoints.toPoint는 lat 37.501234 · lng 127.039876을 SRID 4326 Point로 바꾸고 toLatLng로 같은 값을 돌려준다
- [ ] [기반] 공통 테스트 기반 클래스는 Testcontainers postgis/postgis 컨테이너에서 geography(Point,4326) 컬럼을 만들 수 있다
- [ ] [기반] 공통 테스트 픽스처로 VEHICLE 주체와 USER 주체(familyId · role 지정)의 테스트용 access 토큰을 만들 수 있다
- [ ] [기반] 비동기 설정은 @EnableAsync와 이벤트 리스너용 스레드 풀 빈을 등록한다
- [ ] [web] JSON 형식이 틀린 본문을 보내면 400 INVALID_REQUEST를 돌려준다
- [ ] [web] Bean Validation에 실패하면 422 VALIDATION_FAILED와 fields[].field · fields[].reason을 돌려준다
- [ ] [web] 모든 오류 응답 본문은 error.code · error.message · error.retryAfterSeconds 키를 담는다
- [ ] [web] 공통 예외에 retryAfterSeconds 30을 담아 던지면 429 응답의 error.retryAfterSeconds가 30이다
- [ ] [web] Authorization 헤더 없이 인증이 필요한 API를 부르면 401 UNAUTHORIZED를 돌려준다
- [ ] [web] 만료된 access 토큰으로 부르면 401 UNAUTHORIZED를 돌려준다
- [ ] [web] 서명이 틀린 토큰으로 부르면 401 UNAUTHORIZED를 돌려준다
- [ ] [web] 컨트롤러는 토큰의 주체 종류 · 주체 id · familyId · role을 AuthPrincipal로 받는다
- [ ] [web] 허용되지 않은 role로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [persistence] idempotency_key에 키 · 요청 해시 · 응답 본문을 저장하고 키로 다시 읽는다
- [ ] [web] Idempotency-Key 대상 API에 같은 키 · 같은 본문을 두 번 보내면 두 번째는 첫 응답과 같은 상태 코드 · 본문을 돌려준다
- [ ] [web] Idempotency-Key 대상 API에 같은 키로 두 번째 요청이 오면 유스케이스는 한 번만 실행된다
- [ ] [web] 같은 Idempotency-Key로 다른 본문을 보내면 409 IDEMPOTENCY_KEY_REUSED를 돌려준다
- [ ] [web] Idempotency-Key 대상 API를 헤더 없이 부르면 거절된다
- [ ] [web] Idempotency-Key 대상이 아닌 API는 헤더 없이도 처리된다
- [ ] [서비스] 만료 키 삭제 작업은 저장한 지 24시간이 지난 idempotency_key 행만 지운다
- [ ] [web] 요청 로깅 AOP는 컨트롤러 요청마다 requestId · 호출 주체 · HTTP 메서드 · 경로 · 응답 코드 · 걸린 시간을 한 줄로 남긴다
- [ ] [web] 요청 로깅 AOP는 예외로 끝난 요청의 로그에 오류 코드를 남긴다
- [ ] [web] 요청 로깅 AOP는 요청 본문의 전화번호 · 위치 · 토큰 · 연결 코드 값을 로그에 그대로 남기지 않는다
- [ ] [web] 요청 로깅 AOP는 SSE 스트림 요청을 연결 때와 종료 때만 남긴다
- [ ] [web] 요청 로깅 AOP는 GET /v1/vehicle-link-codes/{codeId}/status 폴링을 응답 status가 직전과 다를 때만 남긴다
- [ ] [web] dev 프로필에서는 springdoc API 문서가 열린다
- [ ] [web] prod 프로필에서는 springdoc API 문서가 꺼져 있다

## 확인 필요 사항

| 항목 | 현재 가정 |
|---|---|
| common을 별도 설계서로 둠 | 소스 2.2 패키지 트리에 common이 도메인 6개와 나란히 있고 테스트할 동작(Idempotency-Key, 오류 형식, 시간 변환)이 있어 따로 둠. 6개 도메인보다 먼저 구현 |
| idempotency_key 컬럼 이름 · 타입 | 소스에는 「키 · 요청 해시 · 응답 본문」만 있음. 키 VARCHAR, 해시 CHAR(64)(SHA-256), 응답 JSONB로 가정 |
| 처음 응답의 HTTP 상태 코드 저장 | 「처음 결과를 그대로」를 지키려면 상태 코드도 저장해야 함. 컬럼 추가로 가정 |
| Idempotency-Key 범위 | 키를 전체에서 유일하게 볼지, 주체(userId · vehicleId)별로 볼지 소스에 없음. 주체별로 가정 |
| Idempotency-Key 헤더가 빠졌을 때 오류 코드 | 소스에 없음. 422 VALIDATION_FAILED(fields: Idempotency-Key)로 가정 |
| 같은 키 요청이 동시에 두 개 들어올 때 | 소스에 없음. 키 유니크 제약으로 두 번째를 막는 것으로 가정 |
| 409 STATE_VERSION_CONFLICT 본문에 세션 뷰를 싣는 필드 이름 | 소스에 없음. error와 나란히 session 필드로 가정 |
| 좌표 변환을 common에 둠 | 소스의 common 목록에는 없으나 schedule · place · pickup이 같이 써서 재사용 규칙(2.2)을 적용 |
| 좌표 소수점 6자리 초과 입력 | 반올림할지 422로 거절할지 소스에 없음. 반올림으로 가정 |
| 이벤트 스레드 풀 크기 | 소스에 없음. 설정값으로 두고 기본값은 리뷰에서 정함 |
| JWT 서명 방식 · 키 형식 | 소스에는 「JWT 키는 환경 변수」만 있음. 05_인증-auth.md에서 함께 정함 |
| SSE · LINK-02 로그 예외를 AOP가 아는 방법 | 소스는 동작만 정함. 경로 기준으로 AOP가 구분하는 것으로 가정 |
