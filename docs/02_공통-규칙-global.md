# 02. 공통 규칙 (global)

모든 도메인에 적용되는 규칙. 패키지 구조, 응답 형식, 예외, 테스트 규칙을 여기에 적는다.

출처: reference/pickup-backend-design.md (2026-11-03, @minwoo) 1장 · 2.1~2.4 · 3.1 · 4.1 · 4.13 · 4.14. 공통 부품의 테스트 케이스는 03_공유모듈-shared.md에 있다.

## 도메인 목록

| 파일 | key · 패키지 | 책임 |
|---|---|---|
| 03_공유모듈-shared.md | shared (패키지 common) | 보안 설정, 오류 형식, Idempotency-Key, 요청 로깅, 비동기 설정, TimeUtils, BaseTimeEntity |
| 04_사용자-user.md | user | 가족 · 사용자 · 차량, 멤버 등록 · 수정 · 해제, 차량 연결 해제, 합류 안내 방식 |
| 05_인증-auth.md | auth | QR 연결(가입 · 로그인 · RELINK), 멤버 초대 · 수락, 토큰 발급 · 갱신 · 폐기, 기기 · 푸시 토큰 |
| 06_일정-schedule.md | schedule | 학원 · 일정, 설정 요약 · 완료, 차량용 주간 일정 · 설정 요약 |
| 07_장소-place.md | place | 보호자가 지정한 픽업 장소, 학원별 기본 장소 |
| 08_픽업-pickup.md | pickup | 픽업 한 건의 시작~완료: 경로 확정, 합류 호출, 정차 · 탑승, 예외, 결과 |
| 09_알림-notification.md | notification | 푸시 발송, 수신 확인, 재발송 |

의존 방향(소스 2장): auth → user(User 생성 요청), place → schedule(Academy 참조), pickup → schedule · place(세션 시작 때 스냅샷), notification은 pickup이 발행한 이벤트를 커밋 뒤에 받고 푸시 토큰은 auth의 Device에서 읽는다.

## 용어

| 용어 | 뜻 |
|---|---|
| 픽업 세션 | 오늘 픽업 한 건. 시작부터 완료 · 취소까지 |
| 합류 호출 | 차량이 도착 3분 전 또는 700m 이내일 때 자녀에게 보내는 「이제 나와요」 안내 |
| 가드 G1~G5 | 합류 호출 직전에 확인하는 조건. G1 세션 유효 · G2 장소 확정 · G3 경로 일치 · G4 ETA 유효 · G5 준비 시간 지남 |
| stateVersion | 세션이 바뀔 때마다 1씩 오르는 번호. 화면이 본 상태와 서버 상태가 같은지 확인할 때 씀 |
| role | 가족 안 역할. GUARDIAN · CO_GUARDIAN · DELEGATE · TEACHER · CHILD |
| 자녀(childId) | role = CHILD인 User. 경로의 {childId}는 그 userId |

## 역할과 권한

| role | 누구 | 할 수 있는 것 |
|---|---|---|
| GUARDIAN | 주 보호자(차량을 연결한 사람) | 설정 전부, 멤버 추가 · 역할 변경, 보류 · 철회 · 취소 · 장소 변경 |
| CO_GUARDIAN | 공동 보호자 | 설정 변경, 보류 · 철회 · 취소 · 장소 변경. 역할 변경은 불가 |
| DELEGATE | 대리보호자 | 진행 중 픽업 조회, 보류 · 철회, 직접 연락 |
| TEACHER | 학원 선생님 · 인계자 | 진행 중 픽업 조회 |
| CHILD | 자녀 | 자녀 화면, 준비 시간 추가, 열람 확인, 도움 요청 |

토큰 주체는 VEHICLE(차량 앱)과 USER(보호자 · 자녀 · 선생님 · 대리보호자 앱) 두 가지다. API별 호출 주체는 각 도메인 설계서의 API 명세 「호출」 열이 기준이다.

## 패키지 구조 (헥사고날)

도메인마다 같은 모양의 패키지를 갖는다. 바깥(웹 · DB · FCM)은 어댑터, 안쪽(규칙)은 도메인이고 둘은 포트(인터페이스)로만 잇는다.

```text
com.{team}.pickupservice
├─ {도메인}
│  ├─ adapter
│  │  ├─ in
│  │  │  ├─ web          Controller (doc 인터페이스 구현), 요청 · 응답 DTO(record)
│  │  │  │  └─ doc       ...ApiDoc (스웨거 어노테이션만 모은 인터페이스, 컨트롤러 1개당 1개)
│  │  │  ├─ sse          (pickup만) SessionStreamController
│  │  │  ├─ scheduler    (pickup · notification) 자동 완료 · 재발송 작업
│  │  │  └─ event        (notification) FCM 발송 리스너
│  │  └─ out
│  │     ├─ persistence  JPA 엔티티, Spring Data 저장소, 도메인 ↔ 엔티티 변환
│  │     └─ event        도메인 이벤트 발행, SSE 송출 리스너
│  ├─ application
│  │  ├─ port
│  │  │  ├─ in           ...UseCase
│  │  │  └─ out          Load...Port, Save...Port, Publish...Port
│  │  └─ service         ...Service (in 포트 구현, 트랜잭션 경계)
│  └─ domain             도메인 객체와 규칙
└─ common                보안 설정, 오류 형식, Idempotency-Key 필터, 스웨거 공통 설정, 요청 로깅(인터셉터), 비동기 설정, TimeUtils, BaseTimeEntity
```

- 의존 방향은 항상 안쪽이다. adapter → application → domain. domain은 어떤 패키지도 모른다. 예외: 프레임워크에 기대지 않는 common의 순수 타입(enum · 값 객체: `common.security.Role` · `common.security.PrincipalType` · `common.geo.LatLng`)은 domain에서 써도 된다. 그래서 Role enum은 `common.security.Role` 하나만 둔다.
- domain에는 Spring · JPA 어노테이션을 붙이지 않는다. JPA 엔티티는 adapter/out/persistence에 따로 두고 도메인 객체와 서로 변환한다.
- 조회는 Spring Data JPA 메서드 이름 쿼리(findBy…)가 기본이다. 동적 쿼리는 Specification이나 간단한 JPQL(@Query)로 하고 QueryDSL은 쓰지 않는다. 예외: SPOT-01 지도 반경 조회는 PostGIS 함수가 필요해 네이티브 쿼리로 둔다.
- 다른 모듈이 필요하면 그 모듈의 in 포트(유스케이스)만 부른다.
- 다른 모듈이 뒤이어 처리할 일(알림 발송 등)은 in 포트를 부르지 않고 Spring 이벤트로 알린다(아래 「모듈 사이 이벤트」).
- 컨트롤러는 in 포트 인터페이스만 알고 서비스 클래스를 직접 주입받지 않는다.
- 스웨거 설명 · 예시 · 오류 응답 어노테이션은 adapter/in/web/doc 인터페이스에만 둔다. 컨트롤러에는 경로와 호출 코드만 남긴다.
- 의존성은 생성자로 주입한다. 기본은 private final 필드 + @RequiredArgsConstructor. 필드 @Autowired는 쓰지 않는다. 같은 타입 빈이 여럿이면 생성자를 직접 쓰고 파라미터에 @Qualifier를 붙인다.
- 주석은 최대한 달지 않는다. Javadoc · 설명 주석 대신 이름으로 뜻을 드러낸다. API 설명은 스웨거 문서 인터페이스에, 설계 근거는 설계서에 둔다.
- 새 코드를 만들기 전에 있는 코드를 먼저 찾아 재사용한다. 여러 모듈이 쓰는 것은 common에 한 번만 만든다.
- 이 규칙은 코드 리뷰로 지킨다. ArchUnit 같은 자동 검사는 쓰지 않는다. 그래서 설계서에 [아키텍처] 항목을 두지 않는다.

예시(소스 2.2, SES-02): `PickupSessionApiDoc`에 `@Tag` · `@Operation` · `@ApiResponse`를 두고, `PickupSessionController implements PickupSessionApiDoc`는 `StartPickupUseCase`만 주입받아 `startPickupUseCase.start(request.toCommand())` 결과를 `StartPickupResponse.from(result)`로 돌려준다.

## lane과 패키지

| lane | 맡는 곳 |
|---|---|
| [기반] | domain(도메인 객체 · 규칙), application/port/in · out 인터페이스와 Command · Result record, JPA 엔티티 클래스, 테스트 시드 · 픽스처 |
| [persistence] | adapter/out/persistence(저장소, 쿼리, 도메인 ↔ 엔티티 변환, out 포트 구현) |
| [서비스] | application/service, adapter/in/scheduler, adapter/out/event와 이벤트 리스너, 외부 API · FCM 호출 어댑터 |
| [web] | adapter/in/web(컨트롤러 · DTO · doc 인터페이스), adapter/in/sse |

계층 병렬 도메인은 [기반]을 먼저 끝내고 나머지 세 lane을 나란히 진행한다.

## 실행 환경

설정 파일은 dev와 prod 두 개로만 나눈다. 공통 설정은 `application.yml`, 다른 값만 `application-dev.yml` · `application-prod.yml`에 두고 `SPRING_PROFILES_ACTIVE`로 고른다.

| 항목 | dev | prod |
|---|---|---|
| 용도 | 개발 · 테스트 · 시연 | 나중 운영용 설정(지금은 띄우지 않음) |
| DB | dev 전용 | prod 전용(dev와 공유하지 않음) |
| 테이블 생성 | ddl-auto: update | ddl-auto: validate(변경은 SQL 직접 실행) |
| FCM | 테스트용 Firebase 프로젝트 | 운영용 Firebase 프로젝트 |
| API 문서(스웨거) | 켬 | 끔 |
| 로그 | DEBUG, SQL 출력 | INFO, SQL 출력 안 함 |
| 비밀값(DB 비밀번호, JWT 키, Firebase 키, 전화번호 암호화 키) | 환경 변수 | 환경 변수 |

비밀값은 설정 파일이나 저장소에 넣지 않는다.

## 기술 스택

Java 21 + Spring Boot 4 서버 하나에 PostgreSQL(PostGIS) 하나만 붙인다. 버전은 개발 시작 때의 최신 안정판으로 고정한다.

| 구분 | 기술 | 쓰는 곳 |
|---|---|---|
| 언어 · 프레임워크 | Java 21, Spring Boot 4.x, Gradle | 서버 전체 |
| 웹 | Spring MVC, SseEmitter | REST API, 차량 실시간 스트림(SES-08) |
| 보안 | Spring Security (OAuth2 Resource Server, JWT) | Bearer 토큰 검증, 주체(VEHICLE · USER)와 role 확인 |
| DB | PostgreSQL 17 + PostGIS 3 | 모든 테이블, 좌표(geography)와 반경 조회 |
| ORM · 쿼리 | Spring Data JPA(Hibernate), Hibernate Spatial | 메서드 이름 쿼리 기본, 좌표 타입 매핑 |
| 중복 요청 처리 | PostgreSQL 테이블 idempotency_key | 키 · 요청 해시 · 응답 본문 저장, 24시간 뒤 스케줄러가 삭제 |
| 스케줄러 | @Scheduled | 10초 자동 완료, 알림 재발송, 만료 Idempotency-Key 삭제. 서버 1대 전제 |
| 내부 이벤트 | ApplicationEventPublisher, @TransactionalEventListener, @Async | 커밋 뒤 FCM · SSE 발송 |
| 푸시 | Firebase Admin SDK (FCM) | 보호자 · 자녀 앱 알림과 데이터 메시지 |
| 외부 호출 | RestClient | 지도 API |
| 요청 검증 | Bean Validation (@Valid) | 요청 값 형식 · 범위 |
| API 문서 | springdoc-openapi | doc 인터페이스로 문서화, dev에서만 켬 |
| 코드 편의 | Lombok | @RequiredArgsConstructor, @Getter |
| 테스트 | JUnit 5, Testcontainers(PostGIS) | 실제 DB 통합 테스트 |
| 상태 확인 | Spring Boot Actuator | 헬스 체크 |

테이블 만드는 방식
- Flyway 같은 마이그레이션 도구는 쓰지 않는다. JPA가 엔티티를 보고 테이블을 만든다.
- dev는 ddl-auto: update. 컬럼 삭제 · 이름 변경 · 타입 변경은 자동 반영되지 않으므로 dev DB를 지우고 다시 띄운다.
- DB는 postgis/postgis 도커 이미지로 띄운다.
- JPA가 못 만드는 VIN 부분 유니크 인덱스는 src/main/resources/schema.sql에 CREATE UNIQUE INDEX IF NOT EXISTS로 적는다. spring.jpa.defer-datasource-initialization: true, spring.sql.init.mode: always로 JPA 테이블 생성 뒤에 실행한다.
- prod는 ddl-auto: validate로 두고 바꿀 때는 SQL을 직접 실행한다.

## 테이블 공통 규칙

- PK는 BIGINT GENERATED BY DEFAULT AS IDENTITY, 엔티티는 @GeneratedValue(strategy = IDENTITY).
- 상태값은 VARCHAR에 저장하고 @Enumerated(STRING)로 읽는다. DB ENUM 타입은 쓰지 않는다.
- 시각은 TIMESTAMPTZ(UTC 저장). 서버 · DB 연결 시간대도 UTC. 시간만 있는 TIME 값과 픽업 날짜(DATE)는 한국 시간 기준 그대로 저장한다.
- 좌표는 geography(Point,4326). 지도 · 반경 조회 칸에는 GIST 인덱스. 엔티티는 Hibernate Spatial의 JTS Point로 받는다. 700m 판정은 PostGIS가 아니라 차량이 보낸 remainingMeters로 서버 코드에서 한다.
- 모든 테이블에 createdAt · updatedAt(TIMESTAMPTZ)을 두고 BaseTimeEntity 하나로 처리한다. 도메인 설계서 테이블에서는 생략한다.
- 테이블 · 컬럼 이름은 snake_case, 엔티티는 camelCase(Spring Boot 기본 이름 규칙). User 엔티티의 테이블만 users.
- JSON 값은 JSONB.
- 전화번호(User.phone)는 AES-256-GCM으로 암호화해 저장한다(키는 환경 변수). 자녀 위치는 이력 없이 SessionChild에 최신 1점만 덮어쓰고 탑승 완료 · 호출 철회 · 픽업 취소 때 비운다.

## API 공통 규칙

- 인증: `Authorization: Bearer {accessToken}`. 토큰에 주체(VEHICLE 또는 USER), familyId, role이 들어 있다. 같은 조회라도 주체별로 필요한 필드만 돌려준다.
- 중복 방지: Idempotency-Key가 「필요」로 표시된 API는 헤더가 필수이고 같은 키면 처음 결과를 그대로 돌려준다. 필요 여부는 각 API 명세의 Idempotency-Key 열이 기준이다(소스 4.14가 4.2~4.11 표보다 우선).
- 동시 처리: 세션을 바꾸는 요청은 화면이 보던 stateVersion을 보낸다. 다르면 409 STATE_VERSION_CONFLICT와 최신 세션 뷰를 돌려준다.
- 시각: 서비스 시간대는 Asia/Seoul(UTC+9). DB는 UTC, 응답은 TimeUtils로 `2026-11-03T18:12:00+09:00`처럼 내린다. 요청 시각도 TimeUtils로 UTC로 바꿔 저장한다. 「오늘」 판단, 픽업 날짜, HH:MM 값은 한국 시간 기준 그대로 다룬다.
- 좌표: API는 WGS84 `{"lat": 37.501234, "lng": 127.039876}`(소수점 6자리). 장소 · 학원 좌표는 geography, 자녀 위치처럼 계속 바뀌는 값은 위도 · 경도 숫자 칸.
- ID 값은 모두 숫자(BIGINT). 필드 이름의 `a.b`는 객체 a 안의 b, `a[].b`는 배열 a의 각 항목 안의 b.
- 404는 대상이 없을 때와 다른 가족의 것일 때 모두 쓴다.

## 오류 형식과 공통 오류

```json
{ "error": { "code": "VALIDATION_FAILED", "message": "입력값을 확인해 주세요", "retryAfterSeconds": null, "fields": [{ "field": "name", "reason": "1~20자여야 합니다" }] } }
```

code 이름은 기획안 「4. 이벤트 · 상태」 탭의 ERR 이름을 그대로 쓴다. 아래 공통 오류는 모든 API에 해당하고, API별 오류는 각 도메인 설계서에 적는다.

| HTTP | code | 언제 |
|---|---|---|
| 400 | INVALID_REQUEST | JSON 형식이 틀림, 필수 요청 파라미터 · 요청 헤더가 빠짐, 요청 파라미터 타입이 틀림 (Idempotency-Key 헤더가 빠지면 400이 아니라 422 VALIDATION_FAILED) |
| 401 | UNAUTHORIZED | 토큰이 없거나 만료 · 폐기됨 |
| 403 | FORBIDDEN | 토큰의 role로는 할 수 없는 동작 |
| 404 | NOT_FOUND | 대상이 없거나 다른 가족의 것, 매핑 없는 경로 |
| 405 | METHOD_NOT_ALLOWED | 그 경로가 받지 않는 HTTP 메서드 |
| 406 | NOT_ACCEPTABLE | 요청 Accept로는 줄 수 없는 응답 형식(응답 본문은 항상 JSON) |
| 415 | UNSUPPORTED_MEDIA_TYPE | 받지 않는 Content-Type |
| 409 | STATE_VERSION_CONFLICT | 보낸 stateVersion이 서버와 다름. 응답에 최신 세션 뷰가 함께 감 |
| 409 | IDEMPOTENCY_KEY_REUSED | 같은 Idempotency-Key로 다른 내용을 보냄, 또는 같은 키의 앞 요청이 아직 처리 중 |
| 422 | VALIDATION_FAILED | 필드 검증 실패. fields에 어느 필드가 왜 틀렸는지 담음 |
| 429 | TOO_MANY_REQUESTS | 횟수 제한. retryAfterSeconds에 다시 시도할 수 있는 초 |
| 500 | INTERNAL_SERVER_ERROR | 처리하지 않은 예외. message는 고정 문구, 예외 내용은 서버 로그에만 |

세션 뷰(SessionView)는 pickup 전용 공통 응답이라 08_픽업-pickup.md에 둔다.

## 모듈 사이 이벤트

1. 서비스가 트랜잭션 안에서 행을 저장하고 ApplicationEventPublisher로 이벤트를 발행한다.
2. 받는 쪽은 @TransactionalEventListener(phase = AFTER_COMMIT) + @Async로 커밋 뒤 별도 스레드에서 처리한다. 롤백되면 아무것도 나가지 않고, FCM이 느려도 API 응답은 기다리지 않는다.
3. Kafka · RabbitMQ 같은 메시지 큐와 Outbox 테이블은 쓰지 않는다. 서버 1대, 같은 JVM 안의 이벤트로 충분하다.
4. 메모리 이벤트라 서버가 발송 도중 꺼지면 사라질 수 있다. 알림은 재발송 스케줄러가 보완한다(09_알림-notification.md).

## 로그

공통
- 전화번호 · 위치 · 토큰 · 연결 코드는 로그에 남기지 않거나 가린다.
- 예외 메시지에 개인정보(전화번호 · 위치 · 토큰 · 연결 코드)를 넣지 않는다. 500 처리와 Idempotency 저장 실패 로그는 예외 내용을 서버 로그에 그대로 남기고 따로 가리지 않으므로, 예외를 만드는 쪽이 지킨다.

요청 로그 (구현: 03_공유모듈-shared.md 「요청 로깅」)
- 대상은 adapter/in/web의 모든 컨트롤러 메서드(@RestController). AOP가 아니라 HandlerInterceptor가 요청이 끝난 뒤 남긴다.
- 형식: 요청 하나에 INFO 한 줄, key=value. 키는 requestId · principal · method · path · status · durationMs이고, 예외로 끝나면 errorCode를 더한다.
- principal은 USER:{userId} · VEHICLE:{vehicleId}, 인증 없는 요청은 ANONYMOUS. status는 실제 응답 상태다.
- requestId: X-Request-Id 헤더가 영숫자 · - · _ 64자 이하면 그대로 쓰고, 없거나 그 밖의 값이면 새 UUID를 만든다. MDC "requestId"에 넣어 같은 요청의 다른 로그(500 로그 등)에도 찍는다.
- 요청 본문 · 쿼리 문자열 · 헤더는 남기지 않는다.
- SSE 스트림(SES-08)은 연결(stream=open) · 종료(stream=closed) 두 줄만 남긴다. 두 줄은 같은 requestId이고 종료 줄의 durationMs는 연결 유지 시간이다.
- 2초마다 오는 LINK-02 폴링은 상태가 바뀔 때만 남긴다. 컨트롤러 메서드에 @PollingLog(stateField = "status")를 달고, 4xx · 5xx는 늘 남긴다.

## 테스트 규칙

- JUnit 5. DB가 닿는 테스트는 Testcontainers(postgis/postgis)로 실제 DB를 띄운다.
- 테스트 픽스처는 common에 한 번만 만들고 모든 모듈이 같이 쓴다.
- 아키텍처 규칙(의존 방향, domain에 Spring · JPA 없음)은 테스트가 아니라 코드 리뷰로 확인한다.

## 도메인 완료 기준
- [ ] 설계서의 테스트 케이스가 모두 GREEN
- [ ] 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨
