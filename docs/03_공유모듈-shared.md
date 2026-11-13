# 03. 공유 모듈 (shared)

모든 도메인이 쓰는 common 패키지(소스 2.2 패키지 트리의 common). 규칙 자체는 02_공통-규칙-global.md에 있고, 이 문서는 그 규칙을 구현하는 부품과 테스트를 다룬다. 04~09 도메인보다 먼저 구현한다.

출처: reference/pickup-backend-design.md(02와 같음). 구현 중 정한 것은 docs/followup/shared.md 「후속 과제」의 [팀장 결정] · [사람 결정] 메모(2026-11-07)를 따른다. 도메인 리뷰(2026-11-07 14:44) 뒤 정한 것은 같은 절의 「[사람 결정 2026-11-07 도메인 리뷰 후]」 메모를 따른다.

| 항목 | 내용 |
|---|---|
| 범위 | common 패키지: 보안 설정(JWT 검증 · role 검사 · 인증 없는 API 등록), 오류 형식과 공통 예외 처리, Idempotency-Key 필터와 idempotency_key 테이블 · 만료 삭제 작업, 요청 로깅(requestId 필터 · 인터셉터 · 로그 형식), Spring MVC 공통 설정(JSON 전용 · 검증 재정의 허용), 비동기 · 스케줄링 설정, TimeUtils · Clock 빈, BaseTimeEntity, 스웨거 공통 설정, 좌표 변환, 테스트 픽스처 |
| 의존 | 없음 |
| 진행 | 순차 |

## 구조

```text
common
├─ security        SecurityConfig(SecurityFilterChain · @EnableMethodSecurity · PublicEndpoints 빈을 모아 permitAll), PublicEndpoints · PublicEndpoint(인증 없는 API 등록), JwtDecoderConfig(HS256 검증 · exp 필수), AuthPrincipalJwtConverter(클레임 → AuthPrincipal), AuthPrincipal · PrincipalType · Role, 401 · 403 처리
├─ error           ErrorResponse, ErrorCode(02 공통 코드와 여러 모듈이 같이 쓰는 도메인 코드, TOO_MANY_REQUESTS 포함), BusinessException(정적 팩토리만), GlobalExceptionHandler(@RestControllerAdvice), ValidationFieldErrors, ErrorResponseWriter(필터 단계 오류 본문)
├─ idempotency     IdempotencyKeyFilter, @IdempotencyKeyRequired, IdempotencyKeyEntity · IdempotencyKeyRepository, IdempotencyKeyCleaner(@Scheduled 만료 삭제), CachedBodyRequest(본문 다시 읽기)
├─ logging         RequestIdFilter(MDC requestId), RequestLoggingInterceptor(HandlerInterceptor, AOP 아님), @PollingLog
├─ web             Spring MVC 공통 설정: JsonOnlyWebMvcConfig(응답은 JSON만), 검증 재정의 허용(ValidationConfigurationCustomizer)
├─ async           AsyncConfig(@EnableAsync, eventTaskExecutor), SchedulingConfig(@Scheduled 작업 실행)
├─ time            TimeUtils, TimeZoneConfig(JVM 기본 시간대 UTC), ClockConfig(Clock.systemUTC() 빈)
├─ geo             GeoPoints, LatLng: 좌표 { lat, lng } ↔ JTS Point 변환
├─ swagger         SwaggerConfig(OpenAPI 빈: bearerAuth 보안 스키마 HTTP bearer JWT · 전역 보안 요구). 문서는 dev만
└─ persistence     BaseTimeEntity
src/main/resources/schema.sql: idempotency_key 유니크 인덱스(04의 VIN 부분 유니크 인덱스도 이 파일에 더함)
src/main/resources/application.yml: logging.pattern.correlation(모든 로그 줄에 [requestId]), spring.datasource.hikari.data-source-properties.logServerErrorDetail=false(PostgreSQL 오류 상세 숨김), springdoc 끔(dev에서 켬)
src/test: 공통 테스트 픽스처(PostGIS 기반 클래스 · TestPostgisConfiguration · TestPostgis(EmbeddedPostgis: Windows 내장 서버, PostgisContainer: Testcontainers), AccessTokenFixture: 테스트 토큰)
```

테스트 케이스의 「요청 로깅 AOP」는 위 logging 패키지(RequestIdFilter + RequestLoggingInterceptor)를 말한다. 체크된 문장이라 이름을 그대로 둔다.

## 테이블

idempotency_key (소스 2.4: 「키 · 요청 해시 · 응답 본문을 저장하고 24시간 뒤 스케줄러가 삭제」. 컬럼은 구현 때 정함)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | 공통 규칙(IDENTITY) |
| principal_type | VARCHAR | NULL, 유니크 인덱스 | VEHICLE · USER. 인증 없는 요청은 null |
| principal_id | BIGINT | NULL, 유니크 인덱스 | userId 또는 vehicleId. 인증 없는 요청은 null |
| idempotency_key | VARCHAR(255) | 유니크 인덱스 | Idempotency-Key 헤더 값 |
| request_hash | char(64) | NOT NULL | SHA-256 16진수. 입력은 핵심 로직 「Idempotency-Key」 |
| response_status | INT | NULL | 처음 응답의 HTTP 상태. null = 처리 중(예약만 된 행) |
| response_body | JSONB | NULL | 처음 응답 본문. 본문 없는 응답(204)은 null |

- 유니크: schema.sql의 인덱스 uk_idempotency_key_principal_key 하나뿐이다. (principal_type, principal_id, idempotency_key)에 NULLS NOT DISTINCT로 CREATE UNIQUE INDEX IF NOT EXISTS. 인증 없는 요청(주체 null)끼리도 같은 키가 두 번 들어가지 않는다. 엔티티에는 @UniqueConstraint를 두지 않는다.
- createdAt · updatedAt은 BaseTimeEntity. 만료 삭제는 created_at으로 판단한다.
- dev: ddl-auto update는 NOT NULL 제약을 풀지 않는다. response_status가 NOT NULL로 만들어진 옛 dev DB는 지우고 다시 띄운다(남아 있으면 예약 INSERT가 실패해 500).
- prod: 공통 application.yml의 spring.sql.init.mode always가 prod에도 걸려 기동 때 schema.sql이 실행된다. ddl-auto validate라 JPA가 테이블을 만들지 않으므로 idempotency_key 테이블 DDL과 위 인덱스 SQL을 기동 전에 직접 실행한다.

## API 명세

common에는 엔드포인트가 없다. 아래 부품이 모든 도메인의 API에 걸린다.

| 부품 | 걸리는 곳 |
|---|---|
| JWT 인증 | 인증이 필요한 모든 API. 인증 없이 부르는 API는 각 도메인 API 명세의 「호출」 열에 「인증 없음」으로 적고, 그 도메인이 PublicEndpoints 빈으로 등록한다(핵심 로직 「인증 없는 API」) |
| role 검사 | 컨트롤러 메서드에 단 @PreAuthorize("hasRole('…')") |
| Idempotency-Key 필터 | @IdempotencyKeyRequired를 단 컨트롤러 메서드. 각 도메인 API 명세에서 Idempotency-Key가 「필요」인 API에 단다 |
| 오류 처리 | 모든 API(아래 공통 오류 응답) |
| JSON 전용 응답 | 모든 API |
| 요청 로깅 | 모든 @RestController 메서드. 폴링 API(LINK-02)는 @PollingLog를 단다 |
| 스웨거 문서 | dev에서만 열림. Bearer JWT 보안 스키마를 담음(핵심 로직 「스웨거」) |

공통 오류 응답(코드 정의는 02 「오류 형식과 공통 오류」)

| 상황 | HTTP · code | 비고 |
|---|---|---|
| JSON 형식이 틀림, JSON 값의 타입 · enum · 시각 형식이 틀림, 본문이 없음 | 400 INVALID_REQUEST | fields 없음 |
| 필수 요청 헤더 · 파라미터가 빠짐, 요청 파라미터 타입이 틀림 | 400 INVALID_REQUEST | |
| Bean Validation 실패(@Valid 본문, 컨트롤러 파라미터 제약, 메서드 검증) | 422 VALIDATION_FAILED | fields[].field · fields[].reason(제약 메시지) |
| Idempotency-Key 대상 API에 헤더가 없음 | 422 VALIDATION_FAILED | fields[0].field Idempotency-Key, 컨트롤러는 실행하지 않음 |
| 토큰 없음 · 만료 · 서명 틀림 · exp 없음 · 주체 클레임 틀림 | 401 UNAUTHORIZED | PublicEndpoints 경로는 토큰을 읽지 않아 401이 나지 않음 |
| role 거절(@PreAuthorize) | 403 FORBIDDEN | |
| 매핑 없는 경로(인증된 요청일 때. 토큰이 없으면 401), prod의 스웨거 경로 | 404 NOT_FOUND | 스웨거 경로는 permitAll이라 토큰 없이도 404 |
| 그 경로가 받지 않는 HTTP 메서드 | 405 METHOD_NOT_ALLOWED | |
| Accept가 application/xml뿐인 요청 | 406 NOT_ACCEPTABLE | 본문은 JSON |
| 받지 않는 Content-Type | 415 UNSUPPORTED_MEDIA_TYPE | |
| 같은 Idempotency-Key에 다른 요청 해시, 같은 키의 앞 요청이 처리 중 | 409 IDEMPOTENCY_KEY_REUSED | |
| BusinessException | 예외의 status · code · message · retryAfterSeconds | 도메인 오류는 모두 이 길 |
| 그 밖의 Spring MVC 4xx 예외(ResponseStatusException 포함) | 그 예외의 4xx 상태 | code는 HttpStatus 이름(예: CONFLICT), message는 reason, reason이 없으면 기본 문구 |
| 처리하지 않은 예외, Idempotency 조회 · 예약 단계의 DB 오류(유니크 위반 제외, DataAccessException · TransactionException) | 500 INTERNAL_SERVER_ERROR | 고정 문구. 본문에 예외 내용 · 예외 클래스 이름 · 테이블 · 컬럼 이름 없음 |
| 클라이언트가 연결을 끊어 난 예외(SSE 스트림 끊김의 AsyncRequestNotUsableException, Broken pipe 등) | 응답 본문 없음 | 정상 상황이라 ERROR가 아닌 DEBUG로만 기록. 판정은 Spring의 DisconnectedClientHelper.isClientDisconnectedException(2026-11-13 dev 로그 분석으로 추가) |

## 핵심 로직

### 오류 형식
- 모든 오류 응답 본문은 `{"error":{"code","message","retryAfterSeconds","fields","overlapScheduleIds"}}`. code · message · retryAfterSeconds 키는 늘 있고(값이 없으면 retryAfterSeconds는 null), fields는 VALIDATION_FAILED일 때만, overlapScheduleIds는 값이 있을 때만(NON_NULL) 나온다.
- 컨트롤러까지 간 요청의 예외는 GlobalExceptionHandler(@RestControllerAdvice)가, 필터 단계 오류(Security 401 · 403, IdempotencyKeyFilter 409 · 422 · 500)는 ErrorResponseWriter가 같은 본문으로 쓴다.
- BusinessException은 공개 생성자가 없고 정적 팩토리로만 만든다(팀장 결정, 파라미터 3개 규칙).

| 팩토리 | 쓰는 때 | 응답 |
|---|---|---|
| of(ErrorCode) | 02의 공통 코드(NOT_FOUND · FORBIDDEN 등), 여러 모듈이 같이 쓰는 도메인 코드(VEHICLE_ALREADY_LINKED) | 그 코드의 HTTP 상태 · code · 문구 |
| of(HttpStatus, String code, String message) | 한 모듈만 쓰는 도메인 오류 코드(문자열) | 넣은 상태 · code · message |
| tooManyRequests(int retryAfterSeconds) | 429 횟수 제한 | 429 TOO_MANY_REQUESTS, retryAfterSeconds 채움 |
| conflictWithOverlap(HttpStatus status, String code, String message, List\<Long\> overlapScheduleIds) | 겹치는 일정 id를 함께 보내는 도메인 오류(schedule SCHEDULE_OVERLAP) | 넣은 상태 · code · message, error.overlapScheduleIds 채움 |

- ErrorCode enum에는 02의 공통 코드만 둔다(TOO_MANY_REQUESTS 포함). 도메인 코드는 enum에 넣지 않는다. 단, 여러 모듈이 같은 뜻으로 쓰는 도메인 오류는 한 번만 ErrorCode에 둔다(예: VEHICLE_ALREADY_LINKED 409 「이미 보호자가 연결된 차량입니다」 — user createGuardian · auth LINK-01 · LINK-04, 사람 결정 user #73).
- 429는 retryAfterSeconds를 채운다. 도메인 코드로 429를 낼 때 retryAfterSeconds를 채우는 방법은 확인 필요.
- 500은 고정 문구를 보내고 예외 내용은 서버 로그에만 남긴다(MDC requestId가 함께 찍힘).
- 409 STATE_VERSION_CONFLICT 응답에는 error와 함께 최신 세션 뷰가 실린다(pickup이 채움). 실리는 필드 이름은 확인 필요.

422 fields[].field 이름

| 오류 위치 | field 값 |
|---|---|
| @Valid 본문 필드 | 본문 필드 이름. 파라미터 제약과 함께 메서드 검증으로 처리될 때도 같음 |
| @RequestParam · @RequestHeader | 애너테이션에 적은 이름(예: Idempotency-Key). Java 파라미터 이름이 아님 |
| 그 밖의 파라미터(@PathVariable 등) | 파라미터 이름(이름이 다른 @PathVariable은 확인 필요) |

### Spring MVC 공통 설정 (common.web)
- JsonOnlyWebMvcConfig(WebMvcConfigurer)가 응답 메시지 변환기에서 XML 변환기를 뺀다. jackson-dataformat-xml 의존성은 그대로 둔다(google-cloud-storage가 씀).
- Accept가 application/xml뿐이면 406 NOT_ACCEPTABLE(JSON 오류 본문), json이나 */*가 섞여 있으면 200 JSON. String을 돌려주는 엔드포인트는 범위 밖이다(응답 DTO는 record 규칙).
- 검증 재정의 허용: ValidationConfigurationCustomizer로 hibernate.validator.allow_parameter_constraint_override=true. 제약이 없는 doc 인터페이스를 구현한 컨트롤러 파라미터에 @Positive 등을 달아도 HV000151(500) 없이 422가 된다. 규칙 「doc에는 스웨거만, 검증은 컨트롤러」는 그대로다(사람 결정 2026-11-07).

### 인증
- Bearer JWT를 검증해 AuthPrincipal(주체 종류, 주체 id, familyId, role)을 만든다. 컨트롤러는 @AuthenticationPrincipal AuthPrincipal만 받는다.
- 서명은 HS256. 키는 설정 pickup.jwt.secret(Base64, 32바이트 이상)이고 값은 환경 변수 JWT_SECRET으로 넣는다. JwtDecoderConfig가 검증하고 AuthPrincipalJwtConverter가 클레임을 바꾼다.

| 클레임 | 값 | 없거나 틀리면 |
|---|---|---|
| sub | 주체 id(userId 또는 vehicleId) 문자열 | 숫자가 아니면 401 |
| principalType | VEHICLE · USER | 없거나 다른 값이면 401 |
| familyId | 숫자 | 없으면 401이 아니라 AuthPrincipal.familyId = null(연결 전 차량 등). 숫자가 아니면 401 |
| role | GUARDIAN · CO_GUARDIAN · DELEGATE · TEACHER · CHILD(USER 토큰만) | 없는 값이면 401. 없으면 role = null |
| iat · exp | 발급 · 만료 시각 | exp는 필수라 없으면 401(사람 결정). 만료면 401. 만료 검사는 Nimbus 기본 여유 60초 |

- 권한: authorities에 ROLE_{role}과 PRINCIPAL_{type}을 넣는다. @EnableMethodSecurity를 켜고 컨트롤러에 @PreAuthorize("hasRole('GUARDIAN')")처럼 단다(Spring 표준, 팀장 결정). VEHICLE 토큰에 role 클레임이 있으면 AuthPrincipal.role은 채워지지만 ROLE_ 권한은 주지 않는다.
- 401: 토큰이 없거나 만료 · 서명 틀림 · exp 없음 · 주체 클레임 틀림이면 Security 인증 실패 처리가 ErrorResponseWriter로 401 UNAUTHORIZED 공통 오류 본문을 쓴다.
- 403: role이 맞지 않으면 AccessDeniedHandler가 403 FORBIDDEN 공통 오류 본문을 쓴다.
- common은 서명과 exp만 검사한다. 폐기된 기기의 access 토큰을 막을지는 확인 필요.
- 토큰 발급 · 갱신 · 폐기는 auth(05_인증-auth.md)가 한다. 발급기는 위 클레임 이름 · 서명 키를 같이 쓰고 exp를 반드시 넣는다.

### 인증 없는 API (PublicEndpoints)
- 인증 없이 부르는 API는 그 API를 가진 도메인이 PublicEndpoints 빈을 내서 등록한다. 항목은 record PublicEndpoint(HttpMethod method, String pattern)이고 pattern은 경로 패턴이고 `**`를 쓸 수 있다(사람 결정).
- SecurityConfig가 모든 PublicEndpoints 빈을 모아 그 메서드 · 경로를 permitAll한다. common은 도메인 경로를 직접 적지 않는다.
- 그 경로에서는 Authorization Bearer 토큰을 읽지 않는다. 만료 · 서명 틀린 토큰이 붙어 와도 401이 아니라 그대로 처리되고 principal은 없다(요청 로그 principal은 ANONYMOUS, Idempotency 범위는 주체 null).
- springdoc 경로의 permitAll은 핵심 로직 「스웨거」를 따른다. /error는 permitAll이 아니다(확인 필요).

### Idempotency-Key
- 대상: 컨트롤러 메서드에 @IdempotencyKeyRequired를 단다. 같은 경로라도 HTTP 메서드마다 따로 정한다. 대상이 아닌 API는 헤더 없이도 처리한다.
- IdempotencyKeyFilter는 Security 필터 체인 뒤에 돈다(@Order로 순서를 적음). HandlerMapping으로 처리할 메서드를 찾아 애너테이션을 확인하고, 본문을 다시 읽을 수 있게 감싼다. 저장소는 ObjectProvider · @Lazy로 늦게 받는다(웹 서버 기동 단계에서 DB 연결을 먼저 만들지 않게).
- 헤더가 없으면 422 VALIDATION_FAILED(fields[0].field Idempotency-Key, reason 한국어 문구)로 거절하고 컨트롤러는 실행하지 않는다.
- 범위: (주체 종류, 주체 id, 키). 인증 없는 요청은 주체를 null로 저장해 모든 호출자가 같은 공간을 쓴다(팀장 결정).
- 요청 해시 = SHA-256(HTTP 메서드 + 경로 + 본문 바이트)의 16진수 64자. JSON 정규화는 하지 않는다. 경로는 쿼리 문자열을 뺀 요청 URI다.

| 기존 행(같은 주체 · 키) | 응답 |
|---|---|
| 없음 | 예약 → 컨트롤러 실행(아래 순서) |
| 같은 요청 해시 · 완료 | 저장한 상태 코드 · 본문을 그대로 돌려준다. 컨트롤러는 실행하지 않는다 |
| 다른 요청 해시 | 409 IDEMPOTENCY_KEY_REUSED. 저장된 해시는 바꾸지 않는다 |
| 처리 중(response_status null) | 409 IDEMPOTENCY_KEY_REUSED |

처리 순서

| 단계 | 동작 |
|---|---|
| 1. 예약 | response_status null(처리 중) 행을 INSERT한다. 저장소 save 트랜잭션에서 바로 커밋한다(필터는 트랜잭션 밖, open-in-view false) |
| 1a. 예약이 유니크 위반 | 동시에 들어온 같은 키 요청에 진 것. 409 IDEMPOTENCY_KEY_REUSED |
| 1b. 조회 · 예약 단계의 그 밖의 DB 오류 | DataAccessException(유니크 위반 제외)과 TransactionException(트랜잭션 시작 실패 등) 모두 500 INTERNAL_SERVER_ERROR. 예외를 필터 밖으로 던지지 않고 ErrorResponseWriter가 공통 본문(예외 메시지 · 클래스 이름 없음)을 쓴다. 컨트롤러는 실행하지 않고 행도 남지 않는다 |
| 2. 실행 | 컨트롤러를 실행한다 |
| 3a. 2xx로 끝남 | 행에 응답 상태 · 본문을 채운다(완료) |
| 3b. 4xx · 5xx로 끝남 | 행을 지운다. 같은 키 · 같은 본문으로 다시 보내면 다시 실행된다 |

- 재응답 본문은 jsonb에서 읽어 JSON 값이 첫 응답과 같다(키 순서 · 공백은 다를 수 있음). 응답 헤더(Location 등)는 되살리지 않는다.
- 3a의 응답 저장이 실패하면 지금은 행을 지우고 2xx를 보낸다(확인 필요).
- 만료 삭제: IdempotencyKeyCleaner(@Scheduled)가 created_at < 지금 − 24시간(엄격히 작음, 정확히 24시간인 행은 남김)인 행을 response_status와 상관없이(처리 중 행도) 지우고 지운 행 수를 돌려준다. 지금 시각은 Clock 빈에서 읽는다. 주기 · 첫 지연은 설정 pickup.idempotency.cleanup-interval(기본 PT1H).
- 서버 1대 전제다(2대 이상이면 ShedLock, 소스 8장).

### 요청 로깅
- 02_공통-규칙-global.md 「로그」 절의 규칙을 구현한다.
- AOP(@Around)가 아니라 HandlerInterceptor.afterCompletion에서 남긴다. @Valid 실패는 컨트롤러 호출 전에 나고, 예외 응답 status는 @RestControllerAdvice가 정한 뒤에야 알 수 있기 때문이다(팀장 결정).
- RequestIdFilter: X-Request-Id 헤더가 영숫자 · - · _ 64자 이하면 그대로 쓰고, 없거나 그 밖의 값이면 새 UUID를 만든다. MDC "requestId"에 넣는다.
- 로그 형식: application.yml의 logging.pattern.correlation을 "[%X{requestId:-}] "로 둔다. 같은 요청 동안 남는 모든 로그 줄(500 처리기 error 로그, Idempotency 저장 실패 로그 포함)에 [requestId]가 찍힌다.
- RequestLoggingInterceptor: @RestController 메서드 요청마다 INFO 한 줄을 key=value로 남긴다. 키는 requestId · principal · method · path · status · durationMs이고 예외로 끝나면 errorCode를 더한다.
- path는 실제 요청 경로가 아니라 매핑 패턴(예: /v1/invites/{token})이다. 매핑 패턴이 없으면 "unknown"을 남긴다. 그래서 경로 변수의 토큰 · 연결 코드 값이 로그에 남지 않는다(사람 결정).
- principal은 USER:{userId} · VEHICLE:{vehicleId}, 인증 없는 요청은 ANONYMOUS.
- status는 실제 응답 상태다. @ResponseStatus(CREATED)면 201, @PreAuthorize 거절이면 403(errorCode=FORBIDDEN).
- errorCode는 오류 처리기(GlobalExceptionHandler)가 요청 속성에 넣고 인터셉터는 읽기만 한다.
- 요청 본문 · 쿼리 문자열 · 헤더는 남기지 않는다.
- SSE: 경로가 아니라 비동기 처리 시작(afterConcurrentHandlingStarted)으로 구분한다. 연결 때 stream=open 한 줄, 끝날 때 stream=closed 한 줄을 남기고 이벤트를 보내는 동안은 남기지 않는다. 종료 줄의 durationMs는 연결 유지 시간이다. 두 줄은 같은 requestId를 쓴다(요청 속성으로 ASYNC 디스패치에 MDC를 되살림).
- 폴링: 경로가 아니라 컨트롤러 메서드에 단 @PollingLog(stateField = "status")로 구분한다. 응답 본문의 그 필드 값(enum 이름)이 같은 요청 경로(codeId별)의 직전 값과 같으면 남기지 않는다. 4xx · 5xx는 늘 남긴다. 직전 값은 메모리 LRU(크기 제한 · 만료, 서버 1대 전제)에 둔다. 직전 값의 키는 실제 요청 URI지만 메모리에서만 쓰고 로그 줄의 path는 매핑 패턴이다. auth LINK-02 컨트롤러에 단다.
- DB 오류 상세 숨김: spring.datasource.hikari.data-source-properties.logServerErrorDetail=false(pgjdbc). PostgreSQL 오류 상세(Failing row contains 행 값)가 예외 메시지에 들어가지 않아 DB 제약 위반 로그에 행 값이 남지 않는다(팀장 결정).

### 스웨거
- springdoc 경로(/v3/api-docs/** · /swagger-ui.html · /swagger-ui/**)는 프로필과 상관없이 인증 없이 연다(permitAll).
- 문서가 있는지는 springdoc.*.enabled로만 정한다. 공통 application.yml은 끄고 dev는 켠다. 그래서 prod에서는 이 경로가 404 NOT_FOUND다(401로 경로가 있다는 것을 드러내지 않음).
- SwaggerConfig(common.swagger)가 OpenAPI 빈을 등록한다. components.securitySchemes에 bearerAuth(type http · scheme bearer · bearerFormat JWT)를 두고 전역 security 요구에 bearerAuth를 건다(팀장 결정).

### 시간 · 좌표
- JVM 기본 시간대와 DB 연결 시간대는 UTC로 고정해 변환이 두 번 일어나지 않게 한다. JVM 시간대는 TimeZoneConfig가 기동 때 UTC로 바꾼다(방식은 확인 필요).
- Clock 빈: ClockConfig가 Clock.systemUTC()를 등록한다. todayKst와 만료 키 삭제가 이 빈을 쓴다.
- TimeUtils: UTC Instant → `+09:00` 문자열(형식 yyyy-MM-dd'T'HH:mm:ssXXX, 1초 미만은 버림), `+09:00` 요청 시각 → UTC Instant, 한국 날짜 기준 오늘(LocalDate).
- 좌표 변환: API `{lat, lng}` ↔ SRID 4326 JTS Point. schedule · place · pickup이 같이 쓴다.

### 비동기 · 스케줄링
- AsyncConfig: @EnableAsync와 이벤트 리스너용 ThreadPoolTaskExecutor 빈 eventTaskExecutor(스레드 이름 접두사 event-)를 등록한다. @Async 메서드는 이 풀에서 돈다. 크기는 설정 pickup.async.event.*(임시값 core 2 · max 4 · queue 100, 확인 필요).
- SchedulingConfig: @Scheduled 작업을 켠다. 서버 1대 전제.

### 테스트 픽스처
- 공통 테스트 기반 클래스는 PostgreSQL 17 + PostGIS 3 서버 하나(JVM당 1개, DB pickup_test)를 모든 테스트가 같이 쓴다. Windows는 내장 서버(EmbeddedPostgis, Docker 없음), 그 밖의 OS는 Testcontainers postgis/postgis:17-3.7 컨테이너(PostgisContainer)다. TestPostgis가 OS로 고르고 pickup.test.db로 강제한다.
- AccessTokenFixture는 서버와 같은 키 · 클레임 이름으로 VEHICLE · USER access 토큰(exp 30분)을 만든다. 만료 토큰 테스트는 만료 시각을 Nimbus 여유 60초보다 더 과거로 둔다.
- 스케줄러 테스트 격리: 공통 테스트 설정에서 pickup.idempotency.cleanup-interval을 아주 길게(예: P1D) 둔다. 04~09의 자동 완료 · 재발송 스케줄러도 같은 방식으로 둔다(팀장 결정).
- prod 프로필 테스트는 ddl-auto를 update로 덮어쓴다(테스트 전용 엔티티 때문에 validate를 쓸 수 없음).
- 증거 메모: FOLLOWUP의 #6 · #9 · #20 · #39 증거 인용은 문장 일부만 보여 준다(#6 JVM 시간대만, #9 VEHICLE 토큰만, #20 204 · 본문 null만, #39 reason 없는 경우만). 문장 전체를 덮는 테스트는 따로 있다.

### 시그니처

```java
// common.time
String TimeUtils.toKst(Instant utc);                 // "2026-11-03T18:12:00+09:00"
Instant TimeUtils.toUtc(OffsetDateTime requested);
LocalDate TimeUtils.todayKst(Clock clock);           // Clock 빈: ClockConfig(Clock.systemUTC())

// common.geo
Point GeoPoints.toPoint(double lat, double lng);     // SRID 4326
LatLng GeoPoints.toLatLng(Point point);

// common.security
record AuthPrincipal(PrincipalType type, long id, Long familyId, Role role) {}   // familyId · role은 null 가능
// PrincipalType: VEHICLE · USER / Role: GUARDIAN · CO_GUARDIAN · DELEGATE · TEACHER · CHILD
// 권한: ROLE_{role}(USER 토큰만) · PRINCIPAL_{type}
// Role · PrincipalType은 프레임워크에 기대지 않는 순수 enum이라 각 도메인의 domain에서도 쓴다(02 예외)
record PublicEndpoint(HttpMethod method, String pattern) {}
// PublicEndpoints: 인증 없는 API를 가진 도메인이 내는 빈. PublicEndpoint 목록을 담고 SecurityConfig가 모두 모아 permitAll

// common.error
// ErrorCode: 02의 공통 코드(TOO_MANY_REQUESTS 포함)와 여러 모듈이 같이 쓰는 도메인 코드(VEHICLE_ALREADY_LINKED). 코드마다 HTTP 상태 · 기본 문구
class BusinessException extends RuntimeException {   // 공개 생성자 없음
    static BusinessException of(ErrorCode errorCode);
    static BusinessException of(HttpStatus status, String code, String message);
    static BusinessException tooManyRequests(int retryAfterSeconds);
    static BusinessException conflictWithOverlap(HttpStatus status, String code, String message, List<Long> overlapScheduleIds);
    HttpStatus status(); String code(); Integer retryAfterSeconds(); List<Long> overlapScheduleIds();
}

// 컨트롤러 메서드에 다는 애너테이션
@IdempotencyKeyRequired                              // common.idempotency
@PollingLog(stateField = "status")                   // common.logging
@PreAuthorize("hasRole('GUARDIAN')")                 // role 검사(Spring 표준)
```

## 테스트 케이스
- [x] [기반] BaseTimeEntity를 상속한 엔티티를 저장하면 createdAt과 updatedAt이 채워진다
- [x] [기반] BaseTimeEntity를 상속한 엔티티를 수정해 저장하면 updatedAt만 바뀌고 createdAt은 그대로다
- [x] [기반] TimeUtils.toKst는 UTC 2026-11-03T09:12:00Z를 2026-11-03T18:12:00+09:00으로 바꾼다
- [x] [기반] TimeUtils.toUtc는 2026-11-03T18:12:00+09:00을 UTC 2026-11-03T09:12:00Z로 바꾼다
- [x] [기반] TimeUtils.todayKst는 UTC 2026-11-05T15:30:00Z 시점에 한국 날짜 2026-11-06을 돌려준다
- [x] [기반] 서버가 뜨면 JVM 기본 시간대와 DB 연결 시간대가 UTC다
- [x] [기반] GeoPoints.toPoint는 lat 37.501234 · lng 127.039876을 SRID 4326 Point로 바꾸고 toLatLng로 같은 값을 돌려준다
- [x] [기반] 공통 테스트 기반 클래스는 Testcontainers postgis/postgis 컨테이너에서 geography(Point,4326) 컬럼을 만들 수 있다
- [x] [기반] 공통 테스트 픽스처로 VEHICLE 주체와 USER 주체(familyId · role 지정)의 테스트용 access 토큰을 만들 수 있다
- [x] [기반] 비동기 설정은 @EnableAsync와 이벤트 리스너용 스레드 풀 빈을 등록한다
- [x] [web] JSON 형식이 틀린 본문을 보내면 400 INVALID_REQUEST를 돌려준다
- [x] [web] Bean Validation에 실패하면 422 VALIDATION_FAILED와 fields[].field · fields[].reason을 돌려준다
- [x] [web] 모든 오류 응답 본문은 error.code · error.message · error.retryAfterSeconds 키를 담는다
- [x] [web] 공통 예외에 retryAfterSeconds 30을 담아 던지면 429 응답의 error.retryAfterSeconds가 30이다
- [x] [web] Authorization 헤더 없이 인증이 필요한 API를 부르면 401 UNAUTHORIZED를 돌려준다
- [x] [web] 만료된 access 토큰으로 부르면 401 UNAUTHORIZED를 돌려준다
- [x] [web] 서명이 틀린 토큰으로 부르면 401 UNAUTHORIZED를 돌려준다
- [x] [web] 컨트롤러는 토큰의 주체 종류 · 주체 id · familyId · role을 AuthPrincipal로 받는다
- [x] [web] 허용되지 않은 role로 부르면 403 FORBIDDEN을 돌려준다
- [x] [persistence] idempotency_key에 키 · 요청 해시 · 응답 본문을 저장하고 키로 다시 읽는다
- [x] [web] Idempotency-Key 대상 API에 같은 키 · 같은 본문을 두 번 보내면 두 번째는 첫 응답과 같은 상태 코드 · 본문을 돌려준다
- [x] [web] Idempotency-Key 대상 API에 같은 키로 두 번째 요청이 오면 유스케이스는 한 번만 실행된다
- [x] [web] 같은 Idempotency-Key로 다른 본문을 보내면 409 IDEMPOTENCY_KEY_REUSED를 돌려준다
- [x] [web] Idempotency-Key 대상 API를 헤더 없이 부르면 거절된다
- [x] [web] Idempotency-Key 대상이 아닌 API는 헤더 없이도 처리된다
- [x] [서비스] 만료 키 삭제 작업은 저장한 지 24시간이 지난 idempotency_key 행만 지운다
- [x] [web] 요청 로깅 AOP는 컨트롤러 요청마다 requestId · 호출 주체 · HTTP 메서드 · 경로 · 응답 코드 · 걸린 시간을 한 줄로 남긴다
- [x] [web] 요청 로깅 AOP는 예외로 끝난 요청의 로그에 오류 코드를 남긴다
- [x] [web] 요청 로깅 AOP는 요청 본문의 전화번호 · 위치 · 토큰 · 연결 코드 값을 로그에 그대로 남기지 않는다
- [x] [web] 요청 로깅 AOP는 SSE 스트림 요청을 연결 때와 종료 때만 남긴다
- [x] [web] 요청 로깅 AOP는 GET /v1/vehicle-link-codes/{codeId}/status 폴링을 응답 status가 직전과 다를 때만 남긴다
- [x] [web] dev 프로필에서는 springdoc API 문서가 열린다
- [x] [web] prod 프로필에서는 springdoc API 문서가 꺼져 있다
- [x] [web] 컨트롤러 파라미터 제약과 @Valid 본문이 함께 있어 메서드 검증으로 처리될 때도 본문 필드 오류의 fields[].field는 본문 필드 이름이다
- [x] [web] @RequestParam · @RequestHeader 값 검증 실패의 fields[].field는 애너테이션에 적은 이름(예: Idempotency-Key)이다
- [x] [web] 따로 처리하지 않은 Spring MVC 요청 예외(빠진 요청 헤더 · 받을 수 없는 Accept · ResponseStatusException)는 500이 아니라 그 예외의 4xx 상태와 공통 오류 본문으로 돌려준다
- [x] [web] 서명은 맞지만 주체 클레임이 빠졌거나 틀린 토큰(principalType 없음 · 숫자가 아닌 sub · 없는 role 값)으로 부르면 500이 아니라 401 UNAUTHORIZED를 돌려준다
- [x] [web] Accept application/xml로 불러도 성공 · 오류 응답은 XML이 아니라 JSON이다(의존성으로 들어온 jackson-dataformat-xml 컨버터를 쓰지 않음)
- [x] [web] ResponseStatusException에 reason을 담아 던지면 그 reason이 error.message로 나간다
- [x] [web] familyId 클레임이 없는 VEHICLE access 토큰으로 부르면 401이 아니라 AuthPrincipal.familyId가 null로 들어온다
- [x] [web] 스웨거 doc 인터페이스를 구현한 컨트롤러의 파라미터에 검증 제약(@Positive 등)을 달아도 500(HV000151)이 아니라 422 VALIDATION_FAILED를 돌려준다
- [x] [web] 인증 없이 부르는 Idempotency-Key 대상 API도 같은 키 · 같은 본문의 두 번째 요청에 첫 응답과 같은 상태 코드 · 본문을 돌려준다
- [x] [통합] 실제 웹 서버(RANDOM_PORT)로 애플리케이션 컨텍스트가 뜨고 GET /actuator/health가 200 UP이다
- [x] [web] 같은 주체가 같은 Idempotency-Key로 두 요청을 동시에 보내면 유스케이스는 한 번만 실행되고 나머지 요청은 409 IDEMPOTENCY_KEY_REUSED를 받는다
- [x] [web] Idempotency-Key 예약 저장에서 유니크 위반이 아닌 DB 오류(NOT NULL 위반 등)가 나면 409 IDEMPOTENCY_KEY_REUSED가 아니라 500 공통 오류로 끝난다
- [x] [web] Idempotency-Key 대상 API가 4xx · 5xx로 끝나면 키를 저장하지 않아 같은 키 · 같은 본문으로 다시 보내면 유스케이스가 다시 실행된다
- [x] [web] 요청 로그의 requestId는 안전한 X-Request-Id 헤더 값(영숫자 · - · _ 64자 이하)을 그대로 쓰고 헤더가 없거나 그 밖의 값이면 새로 만든다
- [x] [web] @ResponseStatus(CREATED)를 단 컨트롤러 메서드의 요청 로그 status는 200이 아니라 201이다
- [x] [web] @PreAuthorize 권한 거절로 403이 난 요청의 로그는 status=200이 아니라 status=403 errorCode=FORBIDDEN이다
- [x] [web] 요청 로그의 path는 실제 경로 변수 값이 아니라 매핑 패턴(예: /v1/invites/{token})으로 남아 토큰 · 연결 코드 값이 로그에 없다
- [x] [web] 같은 요청 동안 남는 모든 로그 줄(500 처리기 error 로그 포함)에 requestId가 찍힌다
- [x] [web] exp 클레임이 없는 access 토큰은 서버 키로 서명돼 있어도 401 UNAUTHORIZED를 받는다
- [x] [web] Idempotency-Key 예약 단계에서 DataAccessException이 아닌 DB 오류(트랜잭션 시작 실패 등)가 나도 500 공통 오류 본문으로 끝난다
- [x] [web] DB 제약 위반이 나도 로그에 PostgreSQL 오류 상세(Failing row contains 행 값)가 남지 않는다
- [x] [web] 각 도메인이 PublicEndpoints 빈으로 등록한 메서드 · 경로는 토큰 없이 부를 수 있고 만료된 Bearer 토큰을 붙여도 401이 아니라 그대로 처리된다
- [x] [web] dev API 문서(/v3/api-docs)는 Bearer JWT 보안 스키마를 담는다
- [x] [web] 공통 오류 코드(ErrorCode)로 BusinessException을 만들어 던지면 그 코드의 HTTP 상태 · code · 문구로 응답한다(429는 retryAfterSeconds 포함)
- [x] [web] GlobalExceptionHandler는 클라이언트가 연결을 끊어 난 예외(AsyncRequestNotUsableException · Broken pipe IOException 등 DisconnectedClientHelper.isClientDisconnectedException이 true인 것)를 ERROR로 기록하지 않고 DEBUG로만 남기며 응답 본문을 쓰지 않는다
- [x] [web] GlobalExceptionHandler는 클라이언트 끊김이 아닌 처리하지 않은 예외는 지금처럼 ERROR로 기록하고 500 INTERNAL_SERVER_ERROR 본문을 돌려준다

## 확인 필요 사항

| 항목 | 현재 가정 |
|---|---|
| common을 별도 설계서로 둠 | 소스 2.2 패키지 트리에 common이 도메인 6개와 나란히 있고 테스트할 동작(Idempotency-Key, 오류 형식, 시간 변환)이 있어 따로 둠. 6개 도메인보다 먼저 구현 |
| idempotency_key 컬럼 이름 · 타입 | 해소: principal_type · principal_id · idempotency_key VARCHAR(255) · request_hash char(64)(SHA-256 16진수) · response_status INT NULL · response_body JSONB(테이블 절) |
| 처음 응답의 HTTP 상태 코드 저장 | 해소: response_status 컬럼에 저장. null은 처리 중(예약만 된 행) |
| Idempotency-Key 범위 | 해소: 주체별(principal_type, principal_id, 키). 인증 없는 요청은 주체를 null로 저장해 모든 호출자가 함께 쓰는 공간(NULLS NOT DISTINCT 유니크 인덱스, 팀장 결정) |
| Idempotency-Key 헤더가 빠졌을 때 오류 코드 | 해소: 422 VALIDATION_FAILED, fields[0].field Idempotency-Key |
| 같은 키 요청이 동시에 두 개 들어올 때 | 해소: 실행 전에 행을 예약(response_status null)하고 유니크 인덱스로 두 번째 예약을 막음. 처리 중인 키로 온 요청은 409 IDEMPOTENCY_KEY_REUSED(팀장 결정) |
| 409 STATE_VERSION_CONFLICT 본문에 세션 뷰를 싣는 필드 이름 | 소스에 없음. error와 나란히 session 필드로 가정 |
| 좌표 변환을 common에 둠 | 소스의 common 목록에는 없으나 schedule · place · pickup이 같이 써서 재사용 규칙(2.2)을 적용 |
| 좌표 소수점 6자리 초과 입력 | 반올림할지 422로 거절할지 소스에 없음. 반올림으로 가정. 반올림 위치(GeoPoints 또는 요청 DTO)도 정하지 않음 |
| JWT 서명 방식 · 키 형식 | 해소: HS256, 설정 pickup.jwt.secret(Base64, 32바이트 이상), 환경 변수 JWT_SECRET. 클레임은 sub · principalType · familyId · role · iat · exp이고 exp는 필수(없으면 401, 사람 결정). 핵심 로직 「인증」 |
| SSE · LINK-02 로그 예외를 AOP가 아는 방법 | 해소: 경로 기준이 아님. SSE는 비동기 처리 시작(afterConcurrentHandlingStarted), 폴링은 @PollingLog(stateField)로 구분. 로깅도 AOP가 아니라 RequestLoggingInterceptor(팀장 결정) |
| 요청 로그 path에 경로 변수 값 | 해소: path는 매핑 패턴, 패턴이 없으면 "unknown". 폴링 직전 값 키만 메모리에서 실제 URI(사람 결정 2026-11-07 리뷰 후) |
| 500 로그 · Idempotency 저장 실패 로그의 requestId | 해소: logging.pattern.correlation "[%X{requestId:-}] "로 모든 로그 줄에 찍음 |
| DB 제약 위반 로그의 행 값 | 해소: pgjdbc logServerErrorDetail=false(spring.datasource.hikari.data-source-properties). 02 「예외를 만드는 쪽이 지킨다」는 그대로 둠(팀장 결정) |
| 인증 없는 API를 여는 방법 | 해소: 도메인별 PublicEndpoints 빈(PublicEndpoint(HttpMethod, pattern))을 SecurityConfig가 모아 permitAll, 그 경로는 Bearer 토큰을 읽지 않음(사람 결정). 핵심 로직 「인증 없는 API」 |
| /error 경로 인증 | /error는 permitAll이 아님. 컨트롤러 밖 예외가 서블릿 컨테이너의 /error로 넘어가면 인증에 걸려 401이 될 수 있음. 지금 공통 오류는 GlobalExceptionHandler · ErrorResponseWriter가 직접 씀. /error를 permitAll로 열지 정함 |
| 스웨거 Bearer 보안 스키마 | 해소: common.swagger.SwaggerConfig OpenAPI 빈(bearerAuth, 전역 보안 요구, 팀장 결정). 전역 요구라 인증 없는 API도 문서에 자물쇠가 붙음. doc 인터페이스에서 그 API만 보안 요구를 뺄지 정함 |
| access 토큰 「폐기」 기준 | 02는 「만료 · 폐기면 401」인데 common은 서명 · exp만 검사하고 05의 폐기는 Device.revokedAt(refresh)뿐. 폐기한 기기의 access 토큰이 만료까지 통과함. 「폐기는 refresh 기준」으로 문서를 고칠지, 요청마다 기기 폐기를 확인할지 auth 전에 정함(05 「AUTH-04 뒤 이미 발급된 access 토큰」과 같이) |
| Role enum 중복 | 해소: 02에 「common 순수 타입(Role · PrincipalType · LatLng)은 domain에서 써도 된다」 예외를 둠. Role은 common.security.Role 하나만 두고 user/domain에 따로 만들지 않음(사람 결정 2026-11-07 리뷰 후) |
| 주체 종류별 필수 클레임 | 지금 role 없는 USER 토큰은 role null로 통과하고, 소수 familyId(12.5)는 잘려 12가 됨. 05 토큰 형식과 함께 정함 |
| JWT 키 검사 · 키와 클레임 이름의 위치 | 키가 32바이트보다 짧아도 기동되고 모든 인증이 401. 기동 때 막을지 정함. 키 만드는 코드가 JwtDecoderConfig · AccessTokenFixture 두 곳이고 클레임 이름 상수는 AuthPrincipalJwtConverter 안에만 있음. auth 발급기가 생길 때 SecretKey 빈 · 상수 위치를 하나로 모을지 정함 |
| 인증 없는 Idempotency-Key 공용 공간의 보안 | 다른 기기가 같은 키 · 같은 본문을 보내면 첫 기기의 응답(토큰 포함 가능, AUTH-02 · LINK-04 · FAM-06)이 그대로 감. 키가 무작위 UUID이고 본문에 1회용 코드가 있어 위험은 낮다고 봄. 토큰을 돌려주는 인증 없는 API는 재응답 대신 409로 할지 05에서 정함 |
| 응답 저장(완료) 실패 때 처리 | 컨트롤러가 2xx로 끝났는데 응답 저장이 실패하면 지금은 행을 지우고 2xx를 보냄. 같은 키로 다시 보내면 유스케이스가 다시 실행될 수 있음. 대안: 행을 남겨 24시간 409 / 처리 중 행에 짧은 만료 |
| 남은 처리 중 행 | 처리 도중 서버가 꺼지거나 Error로 끝나 남은 처리 중 행은 만료 삭제 전까지 같은 키를 409로 막음. 행 삭제가 실패하면 원래 예외가 덮임. 그대로 둘지 처리 중 행만 짧게 지울지 정함 |
| Idempotency 재응답 본문의 같음 기준 | jsonb로 저장해 키 순서 · 공백이 첫 응답과 글자 단위로 다름. JSON이 아닌 2xx 본문은 저장에 실패함. JSON 값이 같으면 된다고 가정. 글자까지 같아야 하면 response_body를 text로 바꿈 |
| Idempotency 해시 · 재응답 범위 | 해시 경로에 쿼리 문자열이 들어가지 않음, 재응답에 헤더(Location 등)를 되살리지 않음, 본문 크기 제한 없음, 비동기 반환 API면 빈 200이 저장됨, 255자를 넘는 키 처리. 각각 그대로 둘지 정함 |
| 오류 응답 Content-Type charset | 필터 단계 오류(401 · 403 · 409)만 charset=UTF-8이 붙고 나머지는 없음(본문 바이트는 같음). 한쪽으로 맞출지 정함 |
| 405 · 401 · 415 응답 헤더 | 405에 Allow, 401에 WWW-Authenticate: Bearer, 415에 Accept 헤더가 없음. 붙일지 정함 |
| 본문 형식 오류의 코드 | JSON 값 타입 · enum · 시각 형식 오류와 빠진 본문이 모두 400 INVALID_REQUEST(fields 없음). 유지할지 422로 바꿀지 정함. INVALID_REQUEST 문구 「요청 형식을 확인해 주세요」는 구현자가 정함 |
| 도메인 코드의 ResponseStatusException | Spring 4xx 예외의 code는 HttpStatus 이름(CONFLICT 등)으로 나감. 도메인 코드에서 금지하고 BusinessException만 쓸지 정함. reason 없는 404 예외와 매핑 없는 경로의 문구가 다름 |
| 422 fields 표기 범위 | 클래스 단위 제약 오류는 fields에 안 담김. 중첩 · 배열 필드 이름 형식(a.b, a[].b 또는 children[0].name), 한 필드 여러 위반, List 본문 인덱스, 이름이 다른 @PathVariable은 정하지 않음 |
| BusinessException 생성자 | 해소: 공개 생성자 없음. 정적 팩토리 of(ErrorCode) · of(HttpStatus, String code, String message) · tooManyRequests(int retryAfterSeconds), ErrorCode에 TOO_MANY_REQUESTS 추가(팀장 결정). conflictWithOverlap(HttpStatus, String code, String message, List<Long> overlapScheduleIds) 추가(일정 SET-02 · 03의 overlapScheduleIds용, 06 결정). IdempotencyKeyEntity 6인자 생성자, status · code null 방어, 5xx BusinessException 로그는 다음 리뷰 때 다시 봄 |
| 도메인 코드 429의 retryAfterSeconds | of(HttpStatus, code, message)에는 retryAfterSeconds가 없고 tooManyRequests는 code가 TOO_MANY_REQUESTS로 고정됨. 05의 429 LINK_CODE_REISSUE_LIMIT · LINK_CODE_ATTEMPT_LIMIT에 retryAfterSeconds를 채울지, 채운다면 어떤 팩토리로 할지 auth 전에 정함 |
| 이벤트 스레드 풀 크기 · 포화 정책 | 소스에 없음. 임시값 core 2 · max 4 · queue 100(pickup.async.event.*). 큐가 찬 뒤에야 core를 넘어 늘어 실제 동시 실행은 2개(제안: core = max 4). 포화 때 기본 AbortPolicy라 AFTER_COMMIT 이벤트가 버려짐(재발송 스케줄러가 보완) → 유지 또는 CallerRunsPolicy. 직접 만든 Executor 빈 때문에 Boot의 applicationTaskExecutor가 빠져 MVC 비동기가 SimpleAsyncTaskExecutor를 쓸 수 있음(SES-08 때 확인). @Scheduled 실행 스레드는 기본 1개(08 시작 때 정함) |
| SSE 종료 줄 · 다른 비동기 응답 로그 | emitter 타임아웃 · 연결 끊김 · completeWithError 때 stream=closed 줄의 status · errorCode, Callable · DeferredResult 응답의 로그는 정하지 않음. SSE 타임아웃(AsyncRequestTimeoutException, 503)이 지금 500 + ERROR 로그로 처리됨. pickup SES-08 때 정함 |
| JVM 시간대 고정 방식 | 지금은 TimeZoneConfig 생성자에서 바꿈(빈 순서에 기대므로 Logback 등 먼저 뜬 부품은 +09:00 유지). 대안: main에서 run 전 설정 / 배포 때 TZ=UTC · -Duser.timezone=UTC / EnvironmentPostProcessor. 테스트 JVM을 build.gradle jvmArgs로 고정할지, 운영 로그 시각을 UTC로 찍도록 로그 패턴에 시간대를 넣을지도 함께 정함 |
| TimeUtils 경계 값 | toKst(null)을 null로 돌려줄지 호출 쪽에서 막을지, toKst가 1초 미만을 버려도 되는지, toUtc가 +09:00 말고 Z 등도 받을지 422로 거절할지 정하지 않음. prod 프로필 시간대 설정은 테스트 없이 설정 검토로만 확인함 |
| BaseTimeEntity 보조 테스트 | TIMESTAMPTZ 컬럼 테스트가 체크리스트 밖 보조 테스트로 있음. 테스트 케이스로 올릴지 정함. createdAt이 updatable=false라 merge 저장 때 돌려받은 엔티티의 createdAt이 null일 수 있음(04~09 변환 때 주의) |
