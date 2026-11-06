# 픽업 서비스 백엔드 설계서

Nov 3, 2026 · @minwoo

## 1. 개요

| 항목 | 내용 |
| --- | --- |
| 목적 | 차량 IVI·보호자 앱·자녀 앱 화면 32개가 동작하는 데 필요한 도메인, 데이터 모델, API를 정의 |
| 범위 | 백엔드 API와 데이터. 차량 내비 SDK·Pleos 연동 상세와 인프라는 제외 |
| 근거 자료 | 기획안 시트 「1. 핵심 기능」「3. 화면 명세서」「4. 이벤트·상태」, Figma `Wireframe` 페이지 |
| 규모 | 도메인 6개 · 엔티티 16개 · API 57개 |

**용어**

| 용어 | 뜻 |
| --- | --- |
| 픽업 세션 | 오늘 픽업 한 건. 시작부터 완료·취소까지 |
| 합류 호출 | 차량이 도착 3분 전 또는 700m 이내일 때 자녀에게 보내는 「이제 나와요」 안내 |
| 가드 G1\~G5 | 합류 호출을 보내기 직전에 확인하는 조건(G1 세션 유효 · G2 장소 확정 · G3 경로 일치 · G4 ETA 유효 · G5 준비 시간 지남). G5는 자녀가 SIG-03으로 더한 시간(callNotBefore)이 아직 안 지났으면 호출을 막음 |
| stateVersion | 세션이 바뀔 때마다 1씩 오르는 번호. 화면이 본 상태와 서버 상태가 같은지 확인할 때 씀 |
| role | 가족 안 역할. GUARDIAN · CO\_GUARDIAN · DELEGATE · TEACHER · CHILD |

## 2. 도메인 설계

&#91;embedded content: 도메인 6개와 의존 방향\]

| 도메인(패키지) | 책임 | 엔티티 | API | 다른 도메인과의 관계 |
| --- | --- | --- | --- | --- |
| auth | QR 연결(가입·로그인·RELINK), 멤버 초대·수락, 토큰 발급·갱신·폐기, 기기·푸시 토큰 | VehicleLinkCode, MemberInvite, Device(refresh 토큰 포함) | AUTH, LINK-01\~04, FAM-04\~06 | 가입·초대 수락 때 user에 User 생성을 요청. 다른 도메인은 auth가 검증한 토큰만 받음 |
| user | 보호자·자녀·선생님·대리보호자와 전화번호, 가족과 역할(role), 차량(VIN), 차량 연결 해제, 합류 안내 방식 | User, Family, Vehicle | FAM-01\~03·07, LINK-05, SET-06 | 차량은 가족이 소유한 자산이라 Family 아래에 둠 |
| schedule | 학원·일정, 차량용 주간 일정·설정 요약 | Academy, PickupSchedule | SET | 자녀 = role = CHILD인 User |
| place | 보호자가 지도에서 지정한 픽업 장소, 학원별 기본 장소 | PickupSpot | SPOT | 가족(Family) 단위로 저장해 형제가 공유. 학원은 schedule의 Academy를 참조 |
| pickup (핵심) | 픽업 한 건의 시작\~완료: 경로 확정, 합류 호출, 정차·탑승, 예외, 결과. | PickupSession, SessionChild, Call, SessionEvent, PickupOutcome | SES, SIG, CALL, OUT | 세션 시작 때 schedule·place 값을 스냅샷으로 복사 |
| notification | 푸시 발송, 수신 확인, 재발송 | Notification | NOTI | pickup이 ApplicationEventPublisher로 발행한 이벤트를 커밋 뒤에 받아 FCM으로 보냄(별도 큐 없음). 푸시 토큰은 auth의 Device에서 읽음 |

처음 구현은 이 6개를 모듈로 나눈 모듈러 모놀리스이고, 모듈 안은 헥사고날(포트·어댑터) 구조로 나눕니다(2.2). pickup은 세션 시작 때 schedule·place 값을 스냅샷으로 복사해, 픽업 도중 설정이 바뀌어도 진행 중인 픽업은 영향받지 않게 합니다.

### 2.1 역할과 권한

| role | 누구 | 할 수 있는 것 |
| --- | --- | --- |
| GUARDIAN | 주 보호자(차량을 연결한 사람) | 설정 전부, 멤버 추가·역할 변경, 보류·철회·취소·장소 변경 |
| CO\_GUARDIAN | 공동 보호자 | 설정 변경, 보류·철회·취소·장소 변경. 역할 변경은 불가 |
| DELEGATE | 대리보호자 | 진행 중 픽업 조회, 보류·철회, 직접 연락 |
| TEACHER | 학원 선생님·인계자 | 진행 중 픽업 조회 |
| CHILD | 자녀 | 자녀 화면(자녀 실내 대기 화면, 자녀 합류 안내\~픽업 완료 화면), 준비 시간 추가, 열람 확인, 도움 요청 |

### 2.2 패키지 구조 (헥사고날)

도메인 6개가 각각 같은 모양의 패키지를 갖습니다. 바깥(웹·DB·FCM)은 어댑터, 안쪽(규칙)은 도메인이고, 둘은 포트(인터페이스)로만 이어집니다. 아래는 pickup 예시이고 나머지 모듈도 같습니다.

```text
com.{team}.pickupservice
├─ pickup
│  ├─ adapter
│  │  ├─ in
│  │  │  ├─ web          PickupSessionController (doc 인터페이스를 구현), 요청·응답 DTO(record)
│  │  │  │  └─ doc       PickupSessionApiDoc … (스웨거 어노테이션만 모은 인터페이스)
│  │  │  ├─ sse          SessionStreamController (SES-08, 차량 전용)
│  │  │  └─ scheduler    10초 자동 완료·재발송 작업
│  │  └─ out
│  │     ├─ persistence  JPA 엔티티, Spring Data 저장소, 도메인 ↔ 엔티티 변환
│  │     └─ event        도메인 이벤트 발행(ApplicationEventPublisher), SSE 송출 리스너
│  ├─ application
│  │  ├─ port
│  │  │  ├─ in           StartPickupUseCase, ConfirmRouteUseCase, ReceiveTelemetryUseCase …
│  │  │  └─ out          LoadSessionPort, SaveSessionPort, PublishSessionEventPort …
│  │  └─ service         StartPickupService … (in 포트 구현, 트랜잭션 경계)
│  └─ domain             PickupSession, SessionChild, Call, 호출 판정 규칙(CallTrigger, Guard)
├─ auth · user · schedule · place · notification   (같은 구조, notification의 adapter/in/event에 FCM 발송 리스너)
└─ common               보안 설정, 오류 형식, Idempotency-Key 필터, 스웨거 공통 설정, 요청 로깅 AOP, 비동기 설정(@EnableAsync·이벤트용 스레드 풀), 시간 변환 유틸(TimeUtils), BaseTimeEntity
```

- 의존 방향은 항상 안쪽입니다. adapter → application → domain. domain은 어떤 패키지도 모릅니다.
- domain에는 Spring·JPA 어노테이션을 붙이지 않습니다. JPA 엔티티는 adapter/out/persistence에 따로 두고 도메인 객체와 서로 변환합니다.
- 조회는 Spring Data JPA의 메서드 이름 쿼리(findBy…)를 기본으로 씁니다. 조건에 따라 쿼리가 바뀌는 동적 쿼리는 Spring Data의 Specification이나 간단한 JPQL(@Query)로 처리하고, QueryDSL은 쓰지 않습니다. 예외: 지도 범위·반경 조회(SPOT-01)는 PostGIS 함수(ST\_DWithin·ST\_MakeEnvelope)가 필요해 네이티브 쿼리로 둡니다
- 다른 모듈이 필요하면 그 모듈의 in 포트(유스케이스)만 부릅니다. 예: pickup이 세션을 만들 때 schedule의 일정 조회 유스케이스를 호출.
- 다른 모듈이 뒤이어 처리할 일(알림 발송 등)은 in 포트를 부르지 않고 Spring 이벤트로 알립니다. 서비스는 ApplicationEventPublisher로 이벤트를 발행만 하고, 받는 쪽은 @TransactionalEventListener(phase = AFTER\_COMMIT)로 커밋이 끝난 뒤에 처리합니다. Kafka·RabbitMQ 같은 별도 메시지 큐와 Outbox 테이블은 쓰지 않습니다(4.13).
- 컨트롤러는 in 포트 인터페이스만 알고, 서비스 클래스를 직접 주입받지 않습니다.
- 스웨거 문서는 adapter/in/web/doc에 인터페이스로 정의하고, 컨트롤러가 그 인터페이스를 구현합니다. 설명·예시·오류 응답 같은 어노테이션은 인터페이스에만 두고, 컨트롤러에는 경로와 호출 코드만 남깁니다. 문서 인터페이스는 컨트롤러 1개당 1개입니다(예: PickupSessionApiDoc ↔ PickupSessionController).
- 의존성은 생성자로 주입하고, 기본은 private final 필드 + @RequiredArgsConstructor입니다. 필드에 @Autowired를 붙이는 방식은 쓰지 않습니다. 같은 타입의 빈이 여러 개라 골라야 할 때(예: 같은 인터페이스의 구현이 둘)는 생성자를 직접 쓰고 파라미터에 @Qualifier("빈 이름")을 붙입니다. @RequiredArgsConstructor는 필드의 @Qualifier를 생성자로 옮겨 주지 않기 때문입니다.
- 주석은 최대한 달지 않습니다. 모든 메서드에 붙이는 Javadoc이나, 코드가 무엇을 하는지·왜 그렇게 짰는지 설명하는 주석은 쓰지 않고, 클래스·메서드·변수 이름으로 뜻이 드러나게 씁니다. API 설명은 스웨거 문서 인터페이스에, 설계 근거는 이 문서에 둡니다.
- 모든 요청은 common의 로깅 AOP가 남깁니다. 대상은 adapter/in/web의 모든 컨트롤러 메서드(@RestController)이고, 요청 번호(requestId)·호출 주체(userId 또는 vehicleId)·HTTP 메서드·경로·응답 코드·걸린 시간을 한 줄로 남기고, 예외가 나면 오류 코드도 남깁니다. 전화번호·위치·토큰·연결 코드 같은 개인정보·비밀값은 로그에 남기지 않거나 가립니다. SSE 스트림(SES-08)은 연결·종료 때만, 2초마다 오는 연결 상태 폴링(LINK-02)은 상태가 바뀔 때만 남깁니다.
- 새 코드를 만들기 전에 이미 있는 코드를 먼저 찾아 재사용합니다. 여러 모듈이 쓰는 것(BaseTimeEntity, 공통 응답·오류 형식, 공통 예외, 시간 변환 유틸, 페이징, 테스트 픽스처)은 common에 한 번만 만들고, 같은 로직이 두 번 나오면 메서드나 클래스로 뽑아 함께 씁니다.
- 이 규칙은 코드 리뷰로 지킵니다. ArchUnit 같은 자동 검사는 쓰지 않습니다.

스웨거 문서 인터페이스와 컨트롤러 예시(SES-02). 위는 `adapter/in/web/doc/PickupSessionApiDoc.java`, 아래는 `adapter/in/web/PickupSessionController.java`입니다.

```java
@Tag(name = "픽업 세션")
public interface PickupSessionApiDoc {

    @Operation(summary = "픽업 시작 (SES-02)", description = "같은 날·학원·시간대 일정을 한 세션으로 묶음")
    @ApiResponse(responseCode = "201", description = "새 세션")
    @ApiResponse(responseCode = "422", description = "NOT_TODAY · SETUP_INCOMPLETE")
    ResponseEntity<StartPickupResponse> startPickup(StartPickupRequest request);
}

@RestController
@RequestMapping("/v1/pickup-sessions")
@RequiredArgsConstructor
public class PickupSessionController implements PickupSessionApiDoc {

    private final StartPickupUseCase startPickupUseCase;

    @Override
    @PostMapping
    public ResponseEntity<StartPickupResponse> startPickup(@Valid @RequestBody StartPickupRequest request) {
        StartPickupResult result = startPickupUseCase.start(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(StartPickupResponse.from(result));
    }
}
```

### 2.3 실행 환경 (dev / prod)

설정 파일을 dev와 prod 두 개로만 나눕니다. 로컬 개발과 시연은 모두 dev로 실행하고, prod는 나중에 운영할 때를 위한 설정만 미리 둡니다. 공통 설정은 `application.yml`에, 다른 값만 `application-dev.yml`·`application-prod.yml`에 두고, 서버를 띄울 때 `SPRING_PROFILES_ACTIVE`로 고릅니다.

| 항목 | dev | prod |
| --- | --- | --- |
| 용도 | 개발·테스트·시연 | 나중 운영용 설정 (지금은 띄우지 않음) |
| DB | dev 전용 | prod 전용 (dev와 절대 공유하지 않음) |
| 테이블 생성 | JPA ddl-auto: update (엔티티 기준으로 자동 생성) | JPA ddl-auto: validate (검사만, 변경은 SQL을 직접 실행) |
| FCM | 테스트용 Firebase 프로젝트 | 운영용 Firebase 프로젝트 |
| API 문서(스웨거) | 켬 | 끔 |
| 로그 | DEBUG, SQL 출력 | INFO, SQL 출력 안 함 |
| 비밀값(DB 비밀번호, JWT 키, Firebase 키) | 환경 변수 | 환경 변수 |

비밀값은 두 환경 모두 설정 파일이나 저장소에 넣지 않고 환경 변수로만 넣습니다.

### 2.4 기술 스택

Java 21 + Spring Boot 4 서버 하나에 PostgreSQL(PostGIS) 하나만 붙입니다. 버전은 개발을 시작할 때의 최신 안정판으로 고정합니다.

| 구분 | 기술 | 이 프로젝트에서 쓰는 곳 |
| --- | --- | --- |
| 언어·프레임워크 | Java 21, Spring Boot 4.x, Gradle | 서버 전체 |
| 웹 | Spring MVC, SseEmitter | REST API, 차량 실시간 스트림(SES-08) |
| 보안 | Spring Security (OAuth2 Resource Server, JWT) | Bearer 토큰 검증, 주체(VEHICLE·USER)와 role 확인 |
| DB | PostgreSQL 17 + PostGIS 3 | 모든 테이블. 장소·학원 좌표(geography)와 반경·지도 범위 조회 |
| ORM·쿼리 | Spring Data JPA(Hibernate), Hibernate Spatial | 메서드 이름 쿼리가 기본, 좌표 타입 매핑 |
| 중복 요청 처리 | PostgreSQL 테이블(idempotency\_key) | Idempotency-Key 처리 결과(키·요청 해시·응답 본문)를 저장하고 24시간 뒤 스케줄러가 삭제. 캐시 서버 없이 DB 테이블로 |
| 스케줄러 | @Scheduled | 10초 자동 완료, 알림 재발송, 만료된 Idempotency-Key 삭제. 서버 1대 전제(2대 이상이면 ShedLock 추가, 8장) |
| 내부 이벤트 | ApplicationEventPublisher, @TransactionalEventListener, @Async | 커밋 뒤 FCM·SSE 발송 (4.13) |
| 푸시 | Firebase Admin SDK (FCM) | 보호자·자녀 앱 알림과 데이터 메시지 |
| 외부 호출 | RestClient | 지도 등 외부 API 호출 |
| 요청 검증 | Bean Validation (@Valid) | 요청 값 형식·범위 검사 |
| API 문서 | springdoc-openapi (Swagger UI) | adapter/in/web/doc 인터페이스로 문서화, dev에서만 켬 |
| 코드 편의 | Lombok | @RequiredArgsConstructor 생성자 주입, @Getter |
| 테스트 | JUnit 5, Testcontainers(PostGIS) | 실제 DB로 통합 테스트 |
| 상태 확인 | Spring Boot Actuator | 헬스 체크 |

**테이블 만드는 방식** 별도 마이그레이션 도구(Flyway 등)는 쓰지 않고, 테이블은 JPA가 엔티티를 보고 만듭니다.

- dev는 ddl-auto: update입니다. 새 테이블·새 컬럼은 서버를 띄울 때 자동으로 생깁니다. 컬럼 삭제·이름 변경·타입 변경은 자동으로 반영되지 않으니, 그럴 때는 dev DB를 지우고 다시 띄웁니다.
- DB는 postgis/postgis 도커 이미지로 띄웁니다. 기본 DB에 PostGIS가 이미 켜져 있어 geography 컬럼을 바로 만들 수 있습니다.
- JPA가 만들 수 없는 VIN 부분 유니크 인덱스는 src/main/resources/schema.sql에 CREATE UNIQUE INDEX IF NOT EXISTS로 적고, spring.jpa.defer-datasource-initialization: true와 spring.sql.init.mode: always로 JPA가 테이블을 만든 뒤에 실행되게 합니다.
- prod는 ddl-auto: validate로 두어 서버가 테이블을 바꾸지 않게 하고, 바꿀 때는 SQL을 직접 실행합니다.

## 3. 데이터 모델 (ERD)

엔티티는 15개입니다. 점선 위는 기준 정보(auth · user · schedule · place), 아래는 픽업 진행(pickup · notification)이고, 색 선 4개(픽업 세션 → 차량·픽업 장소, 세션 자녀 → 픽업 일정·사용자)가 둘을 잇습니다. 선이 없는 연결(픽업 결과의 정정자 → 사용자, 알림 → 기기)은 색 글씨 FK로 표시했습니다. 차량 신호(SIG-01 ETA·위치, SIG-02 정차·도어)는 원본을 저장하지 않고 결과만 픽업 세션·세션 자녀·세션 이벤트에 반영합니다.

&#91;embedded content: ERD · 엔티티 16개 전체 (점선 위 기준 정보, 아래 픽업 진행)\]

차량(Vehicle)의 PK는 vehicleId이고, VIN은 중복만 막는 UNIQUE로 둡니다. 연결을 해제해도 행은 지우지 않고 unlinkedAt만 채우며, 같은 차를 다른 가족이 연결하면 새 행이 생겨 가족별 픽업 기록이 섞이지 않습니다. 그래서 UNIQUE는 연결 중인 행끼리만 걸리게 합니다. `PostgreSQL의 부분 유니크 인덱스(CREATE UNIQUE INDEX … ON vehicle (vin) WHERE unlinked_at IS NULL)`를 씁니다.

상태값은 한 컬럼에 몰지 않고, 누가 바꾸는지에 따라 5개 컬럼으로 나눠 저장합니다.

| 컬럼 | 값 | 뜻 | 바뀌는 계기 |
| --- | --- | --- | --- |
| **PickupSession.sessionStatus**\<br>세션 전체 | `ACTIVE` | 픽업 진행 중 | SES-02 픽업 시작 |
|  | `ON_HOLD` | 예외로 멈춤 | SES-13 예외 발생 (incident 열림) |
|  | `COMPLETED` | 픽업 완료 | 세션 자녀가 모두 탑승 또는 미탑승 |
|  | `CANCELLED` | 취소됨 | 보호자·운전자 취소 |
| **PickupSession.vehiclePhase**\<br>차량 위치 단계 | `ROUTING` | 장소로 이동 중 | 세션 시작 |
|  | `APPROACHING` | 장소에 가까워짐 | SIG-01 ETA·거리 기준 |
|  | `STOPPED` | 장소에 정차 | SIG-02 STOPPED |
|  | `DEPARTED` | 출발함 | SIG-02 출발 신호 |
| **PickupSession.spotStatus**\<br>픽업 장소 | `SELECTED` | 장소 선택됨 | 기본 장소 적용 / 변경 |
|  | `ROUTING` | 경로 계산 중 | 내비 경로 요청 |
|  | `CONFIRMED` | 경로 확정, 자녀에게 공개 | SES-04 경로 확정 |
|  | `INVALIDATED` | 쓸 수 없게 됨 → 다시 선택 | 장소 변경·사용 불가 |
| **SessionChild.childPhase**\<br>아이별 | `WAITING` | 실내 대기 (자녀 실내 대기 화면). 도착 예정 시각을 보며 준비 시간 추가 가능 | 세션 시작 |
|  | `CALLED` | 합류 호출 받음 (자녀 합류 안내 화면) | 호출 발송 |
|  | `MEETING` | 차로 이동 중 | 호출 확인 후 |
|  | NOT\_BOARDED | 오늘 타지 않음(미탑승). 이유는 남기지 않음 | SES-11 완료 때 notBoardedChildUserIds로 지정 |
| **Call.status**\<br>호출 1건 | `ACCEPTED` | 호출 생성, 발송 전 | 호출 조건 통과 (CALL-01로 보류 가능) |
|  | `SENT` | FCM 발송함 | 발송 |
|  | `DELIVERED` | 폰에 도착 | NOTI-01 수신 확인 |
|  | `ACKNOWLEDGED` | 아이가 확인 | CALL-04 「확인했어요」 |
|  | `REVOKED` | 철회됨 | CALL-03 철회 → 자녀 안전 대기 화면 |

전체 흐름은 9.6 상태 다이어그램에서 볼 수 있습니다.

**지금은 만들지 않은 테이블**

현재 기획에 필요 없어 넣지 않았고, 아래 기능이 생기면 그때 추가합니다.

| 테이블 | 지금은 어떻게 처리하나 | 이럴 때 추가 |
| --- | --- | --- |
| 도움 요청 (HelpRequest) | 세션 이벤트 HELP\_REQUESTED 한 줄(payload: childUserId, type) + 알림. 1분 제한은 마지막 HELP\_REQUESTED 시각으로 판단 | 요청마다 확인·처리 상태를 관리하거나, 도움 요청 목록 화면이 생길 때 |
| 예외 (Incident) | 픽업 세션의 incident 컬럼 + 세션 이벤트 INCIDENT\_OPENED | 한 세션에 예외가 동시에 둘 이상 열려야 할 때 |
| 결과 정정 (OutcomeCorrection) | 픽업 결과의 정정 컬럼(correctedStopAt 등). 원본 actualStopAt은 유지 | 정정을 여러 번 받거나 승인·반려 절차가 생길 때 |
| 차량 이벤트 (VehicleEvent) | 원본은 저장하지 않고 결과만 반영(vehiclePhase·actualStopAt·boardedAt) + 세션 이벤트 | 도어·정차 신호 원본을 다시 분석해야 할 때 |
| 자녀 정책 (ChildPolicy) | 남는 값이 합류 안내 방식 하나라 Family.joinCallMode 컬럼으로 둠 | 준비 시간·인계 방식처럼 자녀마다 다른 정책이 다시 생길 때 |
| 도착 예측 (EtaPrediction) | 픽업 세션의 etaAt·remainingMeters·etaUpdatedAt(차량 내비 값, 최신값만) | 개발이 끝난 뒤 자체 예측 모델을 만들 때(8장 추후 개발 사항) |

### 3.1 테이블 정의 (컬럼 타입)

PostgreSQL 17 + PostGIS 3 기준입니다. 공통 규칙은 다음과 같습니다.

- PK는 BIGINT GENERATED BY DEFAULT AS IDENTITY를 쓰고, 엔티티는 @GeneratedValue(strategy = IDENTITY)로 받습니다.
- 상태값은 VARCHAR에 저장하고 JPA에서 @Enumerated(STRING)로 읽습니다. DB ENUM 타입은 값을 바꿀 때마다 테이블을 변경해야 해서 쓰지 않습니다.
- 시각은 TIMESTAMPTZ(timestamp with time zone)에 저장합니다. PostgreSQL은 이 타입을 항상 UTC(+0)로 저장하고, 서버·DB 연결 시간대도 UTC로 맞춰 UTC로 읽은 뒤 공통 유틸로 한국 시간(+9)으로 바꿉니다(4.1). 단 수업 종료처럼 시간만 있는 TIME 값과 픽업 날짜(DATE)는 한국 시간 기준 그대로 저장합니다.
- 좌표는 PostGIS의 geography(Point,4326)이고, 지도·반경 조회에 쓰는 칸에는 GIST 인덱스를 겁니다. geography는 거리를 미터로 계산합니다(ST\_DWithin). 엔티티에서는 Hibernate Spatial의 JTS Point 타입으로 받습니다. 합류 호출의 700m 판정은 PostGIS를 쓰지 않고, 차량이 보내는 남은 거리(remainingMeters)로 서버 코드에서 합니다.
- 모든 테이블에 createdAt·updatedAt(TIMESTAMPTZ)을 공통으로 두고, 공통 클래스(BaseTimeEntity) 하나로 처리합니다. 그래서 아래 표에서는 생략합니다.
- 테이블·컬럼 이름은 snake\_case입니다. 엔티티는 camelCase로 쓰고 Spring Boot 기본 이름 규칙이 바꿔 줍니다(pickupSession → pickup\_session, unlinkedAt → unlinked\_at). user는 PostgreSQL 예약어라 User 엔티티의 테이블만 users로 둡니다.
- JSON 값은 JSONB에 저장합니다(세션 이벤트 payload 등).
- 개인정보: 전화번호(phone)는 암호화해서 저장합니다(AES-256-GCM, 키는 환경 변수). 자녀 위치는 이력을 남기지 않고 세션 자녀(SessionChild)에 최신 1점만 덮어쓰며, 탑승 완료·호출 철회·픽업 취소 때 비웁니다.

**auth**

차량 연결 코드 (VehicleLinkCode)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| codeId | BIGINT | PK |  |
| vehicleId | BIGINT | FK → Vehicle |  |
| code | CHAR(8) | INDEX | QR·직접 입력용 8자 |
| status | VARCHAR(20) |  | ISSUED · USED · EXPIRED |
| expiresAt | TIMESTAMPTZ |  | 발급 후 5분 |
| usedAt | TIMESTAMPTZ | NULL |  |

멤버 초대 (MemberInvite)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| inviteId | BIGINT | PK |  |
| userId | BIGINT | FK → User | 초대받는 멤버 |
| token | VARCHAR(64) | UNIQUE | 초대 링크 토큰 |
| expiresAt | TIMESTAMPTZ |  | 발급 후 24시간 |
| usedAt | TIMESTAMPTZ | NULL |  |

기기 (Device)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| deviceId | BIGINT | PK |  |
| ownerType | VARCHAR(10) |  | USER · VEHICLE |
| ownerId | BIGINT | INDEX(ownerType, ownerId) | userId 또는 vehicleId |
| platform | VARCHAR(10) |  | ANDROID · IOS · VEHICLE |
| pushToken | VARCHAR(255) | NULL | FCM 토큰 |
| pushUpdatedAt | TIMESTAMPTZ | NULL |  |
| refreshTokenHash | CHAR(64) |  | refresh 토큰의 SHA-256 해시 |
| revokedAt | TIMESTAMPTZ | NULL | 폐기 시각 |

**user**

가족 (Family)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| familyId | BIGINT | PK |  |
| joinCallMode | VARCHAR(20) | 기본 AUTO | AUTO · GUARDIAN\_CONFIRM |
| settingsVersion | INT | 기본 0 | 설정이 바뀔 때마다 +1 |

사용자 (User)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| userId | BIGINT | PK |  |
| familyId | BIGINT | FK → Family |  |
| role | VARCHAR(20) |  | GUARDIAN · CO\_GUARDIAN · DELEGATE · TEACHER · CHILD |
| name | VARCHAR(20) |  | 1\~20자 |
| phone | VARCHAR(255) | NULL | 국제 형식(+8210…). AES-256-GCM 암호문(nonce·태그 포함, Base64)은 60자가 넘어 20자에 안 들어감 |

차량 (Vehicle)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| vehicleId | BIGINT | PK |  |
| familyId | BIGINT | FK → Family, NULL | 연결 승인 전에는 비어 있음 |
| vin | CHAR(17) | 연결 중인 행끼리 UNIQUE | 차대번호 |
| model | VARCHAR(50) |  |  |
| plateNumber | VARCHAR(20) |  |  |
| unlinkedAt | TIMESTAMPTZ | NULL | 연결 해제 시각 |

**schedule**

학원 (Academy)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| academyId | BIGINT | PK |  |
| name | VARCHAR(100) |  |  |
| address | VARCHAR(255) |  |  |
| location | geography(Point,4326) | GIST | 학원 위치 |
| entrance | geography(Point,4326) | NULL | 출입구. 없으면 일정이 DRAFT |
| phone | VARCHAR(20) | NULL | 대표번호 |

픽업 일정 (PickupSchedule)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| scheduleId | BIGINT | PK |  |
| childUserId | BIGINT | FK → User | role = CHILD |
| academyId | BIGINT | FK → Academy |  |
| weekdays | VARCHAR(30) |  | 예: MON,WED,FRI |
| classEndTime | TIME |  |  |
| targetPickup | SMALLINT |  | 수업 종료 후 몇 분 뒤인지(0 = 종료 즉시, 5 = 5분 후, 직접 = 입력값) |
| status | VARCHAR(10) |  | READY · DRAFT |

**place**

픽업 장소 (PickupSpot)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| spotId | BIGINT | PK |  |
| familyId | BIGINT | FK → Family |  |
| academyId | BIGINT | FK → Academy |  |
| name | VARCHAR(50) |  |  |
| memo | VARCHAR(200) | NULL |  |
| stopPoint | geography(Point,4326) | GIST | 정차 지점 |
| meetingPoint | geography(Point,4326) | WGS84 | 만남 지점 |
| isDefault | BOOLEAN |  | 가족·학원마다 하나만 true |
| createdBy | BIGINT | FK → User |  |

**pickup**

픽업 세션 (PickupSession)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| sessionId | BIGINT | PK |  |
| vehicleId | BIGINT | FK → Vehicle |  |
| spotId | BIGINT | FK → PickupSpot |  |
| spotName | VARCHAR(50) |  | 세션 시작·SES-05 때 PickupSpot에서 복사(스냅샷). 나중에 장소를 수정해도 오늘 운행과 지난 결과는 이 값을 씀 |
| stopPoint | geography(Point,4326) |  | 정차 지점 스냅샷 |
| meetingPoint | geography(Point,4326) |  | 만남 지점 스냅샷 |
| date | DATE | INDEX(vehicleId, date) | 픽업 날짜 |
| sessionStatus | VARCHAR(20) |  | ACTIVE · ON\_HOLD · COMPLETED · CANCELLED |
| vehiclePhase | VARCHAR(20) |  | ROUTING · APPROACHING · STOPPED · DEPARTED |
| spotStatus | VARCHAR(20) |  | SELECTED · ROUTING · CONFIRMED · INVALIDATED |
| stateVersion | INT | 기본 0 | 바뀔 때마다 +1 |
| spotVersion | INT | 기본 0 |  |
| routeVersion | INT | 기본 0 |  |
| callHeld | BOOLEAN | 기본 false | CALL-01 발송 전 보류. 메모리가 아니라 DB에 두어 서버가 재시작돼도 유지 |
| etaAt | TIMESTAMPTZ | NULL | 내비 ETA, 최신값만 |
| remainingMeters | INT | NULL |  |
| etaUpdatedAt | TIMESTAMPTZ | NULL | 60초 넘게 갱신 없으면 stale |
| boardingDetectedAt | TIMESTAMPTZ | NULL | SIG-02 DOOR\_CYCLE로 탑승 후보가 된 시각 |
| boardingCancelableUntil | TIMESTAMPTZ | NULL | 10초 취소 창 마감. 자동 완료 스케줄러는 이 시각이 지난 세션을 찾아 처리하므로 재시작해도 안 사라짐 |
| boardingDoor | VARCHAR(20) | NULL | 인식한 문 |
| incidentType | VARCHAR(30) | NULL | 예: CHILD\_NOT\_ON\_BOARD |
| incidentCause | VARCHAR(100) | NULL |  |
| incidentOpenedAt | TIMESTAMPTZ | NULL |  |
| incidentResolvedAt | TIMESTAMPTZ | NULL |  |

세션 자녀 (SessionChild)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| sessionChildId | BIGINT | PK |  |
| sessionId | BIGINT | FK → PickupSession, UNIQUE(sessionId, childUserId) |  |
| childUserId | BIGINT | FK → User |  |
| scheduleId | BIGINT | FK → PickupSchedule |  |
| childPhase | VARCHAR(20) |  | WAITING · CALLED · MEETING · NOT\_BOARDED |
| extraPrepMinutes | SMALLINT | 기본 0 | 0\~15 |
| callNotBefore | TIMESTAMPTZ | NULL | SIG-03 때 지금 + 누적 추가 분. 이 시각 전에는 호출 안 함(가드 G5). 서버가 재시작돼도 유지 |
| lastLat | NUMERIC(9,6) | NULL | SIG-04 최신 위치. 이력 없이 덮어씀 |
| lastLng | NUMERIC(9,6) | NULL |  |
| lastLocatedAt | TIMESTAMPTZ | NULL | 측정 시각. 탑승 완료·철회·취소 때 세 컴럼 모두 NULL |
| acknowledgedAt | TIMESTAMPTZ | NULL | 「확인했어요」 |
| boardedAt | TIMESTAMPTZ | NULL | 탑승 |

세션 이벤트 (SessionEvent)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| eventId | BIGINT | PK |  |
| sessionId | BIGINT | FK → PickupSession, UNIQUE(sessionId, seq) |  |
| seq | BIGINT |  | 세션 안 순번(SES-09 afterSeq) |
| type | VARCHAR(40) |  | 예: CALL\_SENT |
| stateVersion | INT |  | 이벤트 시점의 세션 버전 |
| occurredAt | TIMESTAMPTZ |  |  |
| payload | JSONB | NULL |  |

합류 호출 (Call)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| callId | BIGINT | PK |  |
| sessionId | BIGINT | FK → PickupSession |  |
| prevCallId | BIGINT | FK → Call, NULL | 재호출이면 이전 호출 |
| triggerType | VARCHAR(20) |  | ETA\_3MIN · DIST\_700M · MANUAL |
| status | VARCHAR(20) |  | ACCEPTED · SENT · DELIVERED · ACKNOWLEDGED · REVOKED |
| spotVersion | INT |  | 호출 시점의 장소 버전 |
| sentAt | TIMESTAMPTZ | NULL |  |
| revokedAt | TIMESTAMPTZ | NULL |  |
| revokeReason | VARCHAR(100) | NULL |  |

픽업 결과 (PickupOutcome)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| sessionId | BIGINT | PK, FK → PickupSession | 세션과 1:1 |
| completedAt | TIMESTAMPTZ |  |  |
| boardingMethod | VARCHAR(20) |  | DOOR · IMMEDIATE · MANUAL |
| predictedStopAt | TIMESTAMPTZ | NULL |  |
| actualStopAt | TIMESTAMPTZ | NULL | 원본, 바꾸지 않음 |
| correctedStopAt | TIMESTAMPTZ | NULL | 정정값 |
| correctionReason | VARCHAR(30) | NULL |  |
| correctionStatus | VARCHAR(20) | NULL |  |
| correctedBy | BIGINT | FK → User, NULL |  |

**notification**

알림 (Notification)

| 컬럼 | 타입 | 키·제약 | 설명 |
| --- | --- | --- | --- |
| notificationId | BIGINT | PK |  |
| sessionId | BIGINT | FK → PickupSession |  |
| callId | BIGINT | FK → Call, NULL | 합류 안내일 때만 |
| type | VARCHAR(30) |  | 예: SESSION\_STARTED · DEPARTURE · CALL · PREP\_EXTENDED |
| deviceId | BIGINT | FK → Device | 받는 기기 |
| sentAt | TIMESTAMPTZ | NULL |  |
| deliveredAt | TIMESTAMPTZ | NULL | NOTI-01 |
| openedAt | TIMESTAMPTZ | NULL |  |

## 4. API 설계

화면에서 도출한 API는 57개이고, 묶음 제목 옆 괄호가 속한 도메인입니다. ETA는 차량 내비 값을 그대로 쓰고 호출 판정은 서버 안에서 하므로 따로 API를 두지 않습니다. ETA와 호출 상태는 세션 조회(SES-03)에 담고, 바뀔 때마다 차량에는 실시간 스트림(SES-08, SSE)으로, 보호자·자녀 앱(모바일)에는 FCM 데이터 메시지로 보냅니다.

### 4.1 공통 규칙

- **인증**: `Authorization: Bearer {accessToken}`. 토큰에 주체(VEHICLE 또는 USER), familyId, 가족 안 role이 들어 있어, 같은 조회라도 주체별로 필요한 필드만 돌려줍니다.
- **중복 방지**: 상태를 바꾸는 요청은 `Idempotency-Key` 헤더가 필수이고, 같은 키면 처음 결과를 그대로 돌려줍니다.
- **동시 처리**: 세션을 바꾸는 요청은 화면이 보고 있던 `stateVersion`을 보냅니다. 서버와 다르면 409 `STATE_VERSION_CONFLICT`와 최신 세션을 돌려줍니다.
- **오류 형식**: `{"error":{"code":"NO_LEGAL_SPOT","message":"…","retryAfterSeconds":null}}`. code 이름은 기획안 「4. 이벤트·상태」 탭의 ERR 이름을 그대로 씁니다.
- **시각**: 서비스 기준 시간대는 한국 시간(Asia/Seoul, UTC+9)입니다. DB에는 UTC(+0)로 저장하고, 읽어서 응답할 때 공통 유틸(`TimeUtils`)로 +9를 적용해 `2026-11-03T18:12:00+09:00`처럼 내려줍니다. 요청으로 받은 시각도 같은 유틸로 UTC로 바꿔 저장합니다. 서버(JVM)·DB 연결의 기본 시간대는 UTC로 고정해 변환이 두 번 일어나지 않게 합니다. 「오늘」 판단(SES-02 NOT\_TODAY)과 픽업 날짜, 수업 종료처럼 시간만 있는 값(HH:MM)은 한국 시간 기준 그대로 다룹니다.
- **좌표**: API는 WGS84 `{"lat": 37.501234, "lng": 127.039876}`(소수점 6자리, 약 10cm 정밀도)로 주고받습니다. DB는 장소·학원 좌표를 PostGIS geography(Point,4326)로 저장하고 GIST 인덱스를 걸어, 「지도 범위 안」·「반경 N m 이내」 조회를 빠르게 합니다. 자녀 위치·차량 위치처럼 계속 쌓이는 값은 위도·경도 숫자 칸으로 둡니다.

### 4.2 인증·기기 (auth)

별도 로그인 화면이 없어서 로그인 API도 두지 않습니다. 보호자 토큰은 LINK-04(차량 연결 승인), 자녀·선생님·대리보호자 토큰은 FAM-06(초대 수락), 차량 토큰은 LINK-02(연결 상태 폴링 응답)에서 발급합니다.

| ID | Method · 경로 | 쓰는 화면 | 요청 → 응답 | 규칙·오류 |
| --- | --- | --- | --- | --- |
| AUTH-01 | POST /v1/auth/refresh | 전체 | refreshToken → 새 accessToken·refreshToken | 만료·폐기면 401. 보호자는 QR 재연결, 자녀는 재초대로 복구 |
| AUTH-02 | POST /v1/vehicle-devices | 차량 연결 화면 최초 실행 | vin, model → deviceId, deviceToken | 연결 전 토큰은 LINK-01·02만 호출 가능. 이미 보호자가 연결된 VIN은 409 VEHICLE\_ALREADY\_LINKED(VIN만 알면 다시 등록해 남의 계정을 가져가는 것을 막음). 재연결(RELINK) 코드는 실제 연결된 차량 기기만 발급 가능 |
| AUTH-03 | PUT /v1/devices/me/push-token | 보호자·자녀 앱 실행 시, 차량 연결 화면 | platform, pushToken → 204 | 첫 등록은 LINK-04·FAM-06 요청에 함께 실림. 이 API는 앱 실행 때와 Firebase가 새 토큰을 줄 때(onNewToken) 갱신용. 푸시 토큰은 로그인과 무관하게 기기의 Firebase SDK가 만든 값을 등록만 함. 차량 앱은 FCM을 쓰지 않고 SSE(SES-08)와 폴링(LINK-02)으로만 받으므로 푸시 토큰을 등록하지 않음 |
| AUTH-04 | DELETE /v1/vehicle-devices/me/session | 차량 설정 요약 화면 | — → 204 | 진행 중 픽업이 있으면 409 ACTIVE\_PICKUP\_EXISTS |

### 4.3 차량 연결 (auth, LINK-05만 user)

| ID | Method · 경로 | 쓰는 화면 | 요청 → 응답 | 규칙·오류 |
| --- | --- | --- | --- | --- |
| LINK-01 | POST /v1/vehicle-link-codes | 차량 연결 화면 | — → codeId, code(8자), qrPayload, expiresAt, reissuesLeft | 유효 5분. 새로 받으면 이전 코드 즉시 폐기. 연속 5회 초과 429 |
| LINK-02 | GET /v1/vehicle-link-codes/{codeId}/status | 차량 연결 화면 | — → status(WAITING / LINKED / RELINKED / EXPIRED), 연결되면 vehicleId·familyId·accessToken·refreshToken | 차량 앱이 2초마다 호출(폴링). LINKED·RELINKED면 토큰을 저장하고 1초 뒤 차량 오늘의 픽업 화면으로, EXPIRED면 LINK-01로 새 코드. 토큰은 한 번만 내려주고 코드는 바로 폐기. 코드를 발급한 기기만 조회 가능(403) |
| LINK-03 | GET /v1/vehicle-link-codes/by-code/{code} | 보호자 QR 스캔 화면, 보호자 연결 승인 화면 | — → status(NEW / RELINK), vehicleModel, 번호판(뒷자리 마스킹), expiresAt | RELINK = 이미 가족에 연결된 차량 → 「이 차량의 보호자로 다시 로그인」 안내. 410 LINK\_CODE\_EXPIRED · 404 LINK\_CODE\_INVALID. 기기당 5회 실패 시 1분 429. 연결은 만들지 않음 |
| LINK-04 | POST /v1/vehicle-links | 보호자 연결 승인 화면 | code, consents, mode(NEW / RELINK), platform, pushToken → 201 vehicleId, familyId, accessToken, refreshToken | NEW: User(role = GUARDIAN)를 만듦(가입·로그인). RELINK: 기존 GUARDIAN에 새 토큰 발급, 이전 기기 토큰 폐기, 이전 기기에 「다른 기기에서 로그인했어요」 푸시(계정 복구). 요청의 pushToken으로 Device를 바로 만들어 AUTH-03 호출을 생략. 코드 재검증 후 성공 즉시 코드 폐기. 차량 주행 중이면 422 VEHICLE\_MOVING |
| LINK-05 | DELETE /v1/vehicle-links/{vehicleId} | 차량 설정 요약 화면, 보호자 가족 프로필 화면 | — → 204 | 차량 매각·교체 시 연결 해제. 행은 지우지 않고 unlinkedAt만 채움(이전 가족 기록 보존). GUARDIAN만. 진행 중 픽업이 있으면 409. 해제 뒤 같은 차량을 연결하면 NEW로 새 vehicleId가 생김 |

### 4.4 가족 멤버 (user, 초대 FAM-04\~06은 auth)

| ID | Method · 경로 | 쓰는 화면 | 요청 → 응답 | 규칙·오류 |
| --- | --- | --- | --- | --- |
| FAM-01 | GET /v1/families/me | 보호자 가족 프로필 화면, 보호자 설정 완료 화면 | — → 멤버(userId·이름·전화번호·role·기기 연결 상태), 차량 |  |
| FAM-02 | POST /v1/families/me/members | 보호자 가족 프로필 화면 | role(CHILD / CO\_GUARDIAN / DELEGATE / TEACHER), name, phone → 201 userId | 자녀 등록과 인계자 추가를 하나로 통합. 앱을 안 쓰는 선생님도 전화번호만으로 등록 가능(기기 없음). 이름 1\~20자, 이모지·제어문자 422. 자녀 외 멤버는 최대 5명 |
| FAM-03 | PATCH /v1/families/me/members/{userId} | 보호자 가족 프로필 화면 | name, phone, role → 200 | role 변경은 GUARDIAN만. 전화번호 형식 오류 422 |
| FAM-04 | POST /v1/families/me/members/{userId}/invites | 보호자 가족 프로필 화면 | replaceDevice → inviteUrl, expiresAt | 24시간·1회용, 재발급 시 이전 링크 폐기. 이미 기기가 연결돼 있는데 replaceDevice=false면 409 |
| FAM-05 | GET /v1/invites/{token} | 자녀 가족 연결 화면, 선생님·대리보호자 앱 | — → 초대한 보호자 이름, 멤버 이름, role, expiresAt | 로그인 전 호출. 만료 410, 사용됨 409 |
| FAM-06 | POST /v1/invites/{token}/acceptance | 자녀 가족 연결 화면, 선생님·대리보호자 앱 | consents, platform, pushToken → accessToken, refreshToken | role별 필수 동의가 다름(CHILD는 위치·알림 필수). Device 생성과 FCM 토큰 저장까지 한 번에 처리 |
| FAM-07 | DELETE /v1/families/me/members/{userId} | 보호자 가족 프로필 화면 | — → 204 | 진행 중 픽업에 묶인 멤버면 409. 기기·토큰도 함께 폐기 |

픽업 설정 API 경로의 `{childId}`는 role = CHILD인 사용자의 userId입니다.

### 4.5 일정·설정 (schedule, SET-06만 user)

| ID | Method · 경로 | 쓰는 화면 | 요청 → 응답 | 규칙·오류 |
| --- | --- | --- | --- | --- |
| SET-01 | GET /v1/places/academies?query= | 보호자 학원·일정 화면 | — → 학원 이름·주소·좌표 목록 | 외부 지도 API 중계 |
| SET-02 | POST /v1/children/{childId}/schedules | 보호자 학원·일정 화면 | 학원(출입구 좌표 포함), weekdays, classEndTime, targetPickup(종료 즉시·5분 후·직접) → 201 scheduleId, academyId, status | 출입구 없으면 DRAFT(픽업 시작 불가). 목표 < 종료 422. 겹침 409(겹치는 일정 id). 하루 3개 초과 422 |
| SET-03 | PUT /v1/schedules/{scheduleId} | 보호자 학원·일정 화면 | SET-02와 같음 → 200 |  |
| SET-04 | GET /v1/children/{childId}/schedules | 보호자 학원·일정 화면, 보호자 설정 완료 화면 | — → 일정 목록 |  |
| SET-06 | PUT /v1/families/me/call-mode | 보호자 합류 안내 방식 화면 | joinCallMode(AUTO / GUARDIAN\_CONFIRM) → settingsVersion | 가족 단위(Family.joinCallMode). 기본값 AUTO라 처음 설정 흐름에서는 부르지 않고, 바꿀 때만 호출. GUARDIAN·CO\_GUARDIAN만 |
| SET-07 | GET /v1/children/{childId}/setup-summary | 보호자 설정 완료 화면 | — → 자녀·일정·픽업 장소·합류 안내 방식, missing\[\], settingsVersion |  |
| SET-08 | POST /v1/children/{childId}/setup-completion | 보호자 설정 완료 화면 | settingsVersion → syncedToVehicle | 다른 기기가 먼저 바꿨으면 409. 누락 있으면 422 SETUP\_INCOMPLETE |
| SET-09 | GET /v1/vehicles/me/weekly-pickups?weekStart= | 차량 오늘의 픽업 화면 | — → 7일 × 일정(최대 3개), readiness(startable, missing\[\]) | missing = 휴대전화 연결 / 픽업 장소 |
| SET-10 | GET /v1/vehicles/me/settings-summary | 차량 설정 요약 화면 | — → 보호자 이름(마스킹), 자녀 별명, 합류 안내 방식 |  |

### 4.6 픽업 장소 (place)

| ID | Method · 경로 | 쓰는 화면 | 요청 → 응답 | 규칙·오류 |
| --- | --- | --- | --- | --- |
| SPOT-01 | GET /v1/families/me/pickup-spots?academyId=&near=lat,lng&radiusM= | 보호자 픽업 장소 화면, 차량 픽업 장소 화면, 차량 예외·복구 화면, 보호자 예외·권한 관리 화면 | — → 보호자가 저장한 장소(이름·정차 지점·만남 지점·메모·기본 여부) | 학원별로 묶어 반환. near·radiusM을 주면 지도 화면 범위 안의 장소만 공간 인덱스로 조회 |
| SPOT-02 | POST /v1/families/me/pickup-spots | 보호자 픽업 장소 화면 | academyId, name, stopPoint, meetingPoint, memo, isDefault → 201 spotId | 지도에서 찍은 좌표를 WGS84 위도·경도로 받아 PostGIS geography로 저장. 가족 단위로 저장해 형제가 같은 학원이면 같이 씀. 가족·학원별 첫 장소는 자동으로 기본 장소. 학원에서 500m를 넘는 좌표는 422 SPOT\_TOO\_FAR(SPOT-03 수정도 같음) |
| SPOT-03 | PATCH · DELETE /v1/pickup-spots/{spotId} | 보호자 픽업 장소 화면 | 바꿀 필드 → 200 / 204 | 진행 중 세션이 쓰는 장소는 삭제 409 SPOT\_IN\_USE. 기본 장소를 바꾸면 이전 기본은 해제 |

### 4.7 픽업 세션 (pickup)

| ID | Method · 경로 | 쓰는 화면 | 요청 → 응답 | 규칙·오류 |
| --- | --- | --- | --- | --- |
| SES-01 | GET /v1/pickup-sessions/active | 차량 오늘의 픽업 화면, 보호자 픽업 진행 현황 화면, 자녀 실내 대기 화면 | — → 진행 중 세션 요약 또는 204 | 토큰 주체 기준으로 찾음 |
| SES-02 | POST /v1/pickup-sessions | 차량 오늘의 픽업 화면 | scheduleIds\[\](같이 태울 자녀들의 일정), date → 201 새 세션 / 200 진행 중 세션 | 같은 날·같은 학원·같은 시간대 일정만 한 세션으로 묶고 자녀마다 세션 자녀 행을 만듦(아니면 422). 기본 장소가 세션 장소로 들어감. 오늘이 아니면 422 NOT\_TODAY. 저장된 장소가 없으면 422 SETUP\_INCOMPLETE |
| SES-03 | GET /v1/pickup-sessions/{sessionId} | 차량 오늘의 픽업\~예외·복구 화면, 보호자 픽업 진행 현황 화면, 보호자 예외·권한 관리 화면, 자녀 화면 전체, 차량 상태 질문 음성 | — → 세션 뷰(아래 예시) | 자녀별 상태는 children\[\]에 담김. 자녀 화면에도 도착 예정 시각(eta.stopEtaAt)을 상시 표시 |
| SES-04 | PUT /v1/pickup-sessions/{sessionId}/route | 차량 픽업 장소 화면 | spotId, navRouteId, stateVersion → spotVersion, routeVersion | 보호자가 저장한 장소만 가능. 내비 SDK 성공 뒤에만 호출하고, 이때부터 자녀 화면에 장소 노출. 성공하면 자녀에게 출발 알림(도착 예정 시각 포함) 푸시, 세션당 1번 |
| SES-05 | POST /v1/pickup-sessions/{sessionId}/spot-changes | 차량 예외·복구 화면, 보호자 픽업 진행 현황 화면, 보호자 예외·권한 관리 화면 | spotId, stateVersion, spotVersion → 200 | 저장된 다른 장소로 변경. 차량은 SPOT\_CHANGED 이벤트를 받아 재안내 후 SES-04로 확정. 호출 발송 후면 먼저 CALL-03으로 철회. 자녀가 이동 중이면 409 CHILD\_ALREADY\_MOVING |
| SES-06 | PUT · DELETE /v1/pickup-sessions/{sessionId}/hold | 차량 예외·복구 화면, 보호자 예외·권한 관리 화면 | stateVersion, reason → 200 | 픽업 보류 / 해제 |
| SES-07 | POST /v1/pickup-sessions/{sessionId}/cancellation | 차량 예외·복구 화면, 보호자 예외·권한 관리 화면 | stateVersion, reason → 200 | 관련 호출 자동 철회 |
| SES-08 | GET /v1/pickup-sessions/{sessionId}/stream | 차량 앱 (모바일 앱은 FCM으로 받음) | SSE → 세션 이벤트 | `Last-Event-ID`로 이어 받기 |
| SES-09 | GET /v1/pickup-sessions/{sessionId}/events?afterSeq= | 차량·보호자 앱·자녀 앱 | — → 이벤트 목록 | 재접속·앱 복귀 때 빠진 이벤트 보충 |
| SES-10 | GET /v1/pickup-sessions/{sessionId}/contacts | 차량 예외·복구 화면, 보호자 픽업 진행 현황 화면, 보호자 예외·권한 관리 화면, 자녀 안전 대기 화면 | — → 이름·전화번호 | 가족 멤버의 User.phone과 학원 대표번호(Academy.phone). 다이얼패드 없음 |
| SES-11 | POST /v1/pickup-sessions/{sessionId}/completion | 차량 픽업 완료 화면 | mode(IMMEDIATE / MANUAL), notBoardedChildUserIds\[\], stateVersion → 200 | MANUAL은 도어 신호 없는 차량(ERR-14)·정차 중만. 10초 자동 완료는 서버 타이머. 오늘 안 타는 아이는 notBoardedChildUserIds에 넣어 미탑승(NOT\_BOARDED)으로 두고 완료. 타야 하는데 안 보이면 SES-13 예외 |
| SES-12 | POST /v1/pickup-sessions/{sessionId}/boarding-rejection | 차량 픽업 완료 화면 | stateVersion → 200 | 취소 창(10초) 안에서만. 지나면 409. 자녀가 여러 명이면 childUserId로 대상 지정 |
| SES-13 | PUT /v1/pickup-sessions/{sessionId}/incident | 차량 픽업 완료 화면 | type(CHILD\_NOT\_ON\_BOARD), childUserId → 200 | 차량 예외·복구 화면과 보호자 예외·권한 관리 화면의 예외 흐름 시작. 픽업 세션의 incident 컬럼에 기록, 이미 열린 예외가 있으면 409. 이력은 세션 이벤트(INCIDENT\_OPENED) |

### 4.8 차량·자녀 신호 (pickup)

| ID | Method · 경로 | 쓰는 화면 | 요청 → 응답 | 규칙·오류 |
| --- | --- | --- | --- | --- |
| SIG-01 | POST …/vehicle-telemetry | 차량 앱 백그라운드 | observedAt, navEtaSeconds, remainingMeters, 위치, 진행 방향, 속도, 기어 → 202 | 15초마다. 원본은 저장하지 않고 픽업 세션의 etaAt·remainingMeters·etaUpdatedAt만 덮어씀. 들어올 때마다 「navEtaSeconds ≤ 180 또는 remainingMeters ≤ 700」이고 가드를 통과하면 합류 호출 발송 |
| SIG-02 | POST …/vehicle-events | 차량 앱 백그라운드 | eventId, type(STOPPED / DEPARTED / STOP\_FAILED / DOOR\_CYCLE), occurredAt, 위치, 도어 → 202 | eventId를 Idempotency-Key로 써서 중복 무시. 원본은 저장하지 않고 결과만 반영(정차 → vehiclePhase·actualStopAt, 탑승 → 세션 자녀 boardedAt). DOOR\_CYCLE은 운전석 제외·정차 중·호출 후일 때만 탑승 후보 |
| SIG-03 | POST …/prep-time-extensions | 자녀 실내 대기 화면 | minutes(5 / 10 / 15) → 200 extraPrepMinutes(누적) | 호출 전 자녀 실내 대기 화면에서만 누를 수 있고, 호출 발송 뒤면 409. 누적 15분을 넘으면 422 PREP\_EXTENSION\_LIMIT. 세션 자녀의 extraPrepMinutes에 더하고 차량에 SSE PREP\_TIME\_EXTENDED(차량 픽업 진행 화면 표시 + 차량 준비 시간 추가 음성 안내), 보호자 픽업 진행 현황 화면에는 「○○ +10분」 표시만(보호자는 누를 수 없음). 합류 호출은 callNotBefore(요청 시각 + 누적 추가 분) 전에는 보내지 않음(가드 G5). 차량 화면에 호출 예정 시각 표시 |
| SIG-04 | POST …/child-locations | 자녀 합류 안내 화면, 자녀 안전 대기 화면 | 위치 1점 → 202 | 위치 공유 동의 필수. 합류 호출을 받은 순간부터 10초마다 보내고, 탑승 완료·호출 철회·픽업 취소 중 하나가 일어나면 멈춤. 호출 전(실내 대기)에는 보내지 않음 |
| SIG-05 | POST …/help-requests | 자녀 실내 대기 화면, 자녀 합류 안내 화면, 자녀 안전 대기 화면 | type(NEED\_HELP / CANT\_FIND\_CAR / CONTACT\_PARENT) → 201 nextAllowedAt | 같은 아이의 마지막 HELP\_REQUESTED 후 1분 안이면 429 + nextAllowedAt(마지막 시각 + 60초). 1분 제한은 아이별. 별도 테이블 없이 세션 이벤트(payload: childUserId, type)로 기록. 보호자는 FCM 알림 + 픽업 진행 현황 화면 배너, 차량은 SSE로 받아 음성 안내 + 작은 배너(운전 중이라 화면 전환 없음). 문구: NEED\_HELP 「도움이 필요해요」, CANT\_FIND\_CAR 「차를 못 찾고 있어요」, CONTACT\_PARENT 「연락해 달라고 해요」. 연타는 세션 행 잠금 안에서 확인 후 기록 |

### 4.9 합류 호출 (pickup)

| ID | Method · 경로 | 쓰는 화면 | 요청 → 응답 | 규칙·오류 |
| --- | --- | --- | --- | --- |
| CALL-01 | PUT · DELETE …/call-hold | 차량 픽업 진행 화면, 보호자 픽업 진행 현황 화면, 차량 보류 음성 안내, 차량 상태 질문 음성 | stateVersion → 200 | 발송 전 보류 / 해제. 해제하면 트리거·가드 다시 판단 |
| CALL-02 | POST …/calls | 차량 정차·호출 화면 | stateVersion → 201 callId | 보호자 확인형에서 「합류 안내 보내기」. 자동 모드는 서버가 직접 생성. 가드 미충족 422 CALL\_GUARD\_UNMET, 이미 발송 409 |
| CALL-03 | POST /v1/calls/{callId}/revocation | 차량 정차·호출 화면, 보호자 픽업 진행 현황 화면, 보호자 예외·권한 관리 화면 | stateVersion, reason → 200 | 발송 후 「호출 보류」·「호출 철회」. 자녀는 안전 대기 화면으로, 조건 재충족 시 재호출 |
| CALL-04 | POST /v1/calls/{callId}/acknowledgement | 자녀 합류 안내 화면 | — → 200 | 「확인했어요」. 열람 ≠ 이동 시작. 아이별로 세션 자녀의 acknowledgedAt에 기록 |

### 4.10 알림 (notification)

| ID | Method · 경로 | 쓰는 화면 | 요청 → 응답 | 규칙·오류 |
| --- | --- | --- | --- | --- |
| NOTI-01 | POST /v1/notifications/{notificationId}/delivery-receipt | 자녀 기기 | receivedAt → 204 | 푸시를 받으면 앱이 자동 호출. 차량 정차·호출 화면 「수신」 칩의 근거 |
| NOTI-02 | GET …/notifications | 보호자 픽업 진행 현황 화면 | — → 알림별 종류·본문·발송·수신·열람 시각 |  |
| NOTI-03 | POST /v1/notifications/{notificationId}/resend | 보호자 픽업 진행 현황 화면 | — → 202 | 같은 알림 재전송은 1분 간격 |

### 4.11 결과·기록 (pickup)

| ID | Method · 경로 | 쓰는 화면 | 요청 → 응답 | 규칙·오류 |
| --- | --- | --- | --- | --- |
| OUT-01 | GET /v1/children/{childId}/pickup-history?month= | 보호자 이용 결과 화면 | — → 날짜별 세션 목록 |  |
| OUT-02 | GET …/outcome | 보호자 이용 결과 화면 | — → 완료 시각, 장소, 탑승 인식 방식, 예상·실제 정차, 차이, 정정 상태 |  |
| OUT-03 | PUT …/outcome/correction | 보호자 이용 결과 화면 | correctedStopAt, reasonCode, note → 200 | actualStopAt 원본은 그대로, 픽업 결과의 정정 컬럼에만 기록. 정정된 건은 분석·학습에서 제외 |
| OUT-04 | PUT /v1/guardians/me/data-settings | 보호자 이용 결과 화면 | learningConsent → retentionDays(90) | OFF 즉시 파기 대기 |

`…`는 `/v1/pickup-sessions/{sessionId}`를 줄인 것입니다.

### 4.12 핵심 응답 예시

**SES-03 세션 뷰** (차량 기준, 차량 픽업 진행 화면 시점). 차량 픽업 진행·정차·완료·예외 화면은 이 하나로 그립니다.

```json
{
  "sessionId": 1024,
  "stateVersion": 15,
  "status": { "session": "ACTIVE", "vehicle": "APPROACHING", "call": "NONE", "spot": "CONFIRMED" },
  "spot": { "spotId": 21, "name": "해솔영어학원 정문 앞", "spotVersion": 4, "locked": false },
  "eta": { "stopEtaAt": "2026-11-05T18:12:00+09:00", "remainingSeconds": 720, "updatedAt": "2026-11-05T18:00:05+09:00", "stale": false },
  "call": {
    "mode": "AUTO", "callId": null, "held": false,
    "scheduledAt": "2026-11-05T18:09:00+09:00",
    "guard": { "met": true, "unmet": [] },
    "progress": { "acceptedAt": null, "sentAt": null, "deliveredAt": null, "acknowledgedAt": null }
  },
  "children": [
    { "childUserId": 31, "name": "민준", "phase": "WAITING", "extraPrepMinutes": 10, "acknowledgedAt": null, "boardedAt": null },
    { "childUserId": 32, "name": "서윤", "phase": "WAITING", "extraPrepMinutes": 0, "acknowledgedAt": null, "boardedAt": null }
  ],
  "route": { "routeVersion": 2, "remainingMeters": 4200 },
  "boarding": null,
  "incident": null
}
```

**SES-08 실시간 이벤트** 한 건(차량 SSE 기준). 보호자·자녀 앱은 같은 data를 FCM 데이터 메시지로 받습니다. 화면은 `stateVersion`이 지금보다 작으면 버리고, `seq`가 건너뛰면 SES-09로 채웁니다.

```text
event: CALL_SENT
id: 42
data: {"eventId":9001,"seq":42,"stateVersion":16,"occurredAt":"2026-11-05T18:09:02+09:00","payload":{"callId":3301,"trigger":"ETA_3MIN"}}
```

실시간 이벤트는 19가지입니다. 차량은 SSE(SES-08)로, 보호자·자녀 앱(모바일)은 FCM 데이터 메시지로 받습니다. 「DB 저장」이 X인 두 개는 세션 이벤트에 남기지 않고 바로 보냅니다. 놓쳐도 곧 다음 값이 오고, 다시 연결하거나 앱을 열면 SES-03으로 최신값을 받습니다.

| 묶음 | 이벤트 | 언제 | 받는 쪽 | DB 저장 |
| --- | --- | --- | --- | --- |
| 세션 | SESSION\_STARTED | SES-02 픽업 시작 | 차량 · 보호자 · 자녀 | O |
|  | SESSION\_COMPLETED | 세션 자녀 모두 탑승 | 차량 · 보호자 · 자녀 | O |
|  | SESSION\_CANCELLED | SES-07 취소 | 차량 · 보호자 · 자녀 | O |
|  | INCIDENT\_OPENED | SES-13 탑승 예외 | 차량 · 보호자 · 자녀 | O |
| 장소 | SPOT\_CONFIRMED | SES-04 경로 확정(이때부터 자녀에게 장소 보임) | 차량 · 보호자 · 자녀 | O |
|  | SPOT\_CHANGED | SES-05 장소 변경 | 차량 · 보호자 · 자녀 | O |
|  | DEPARTURE\_NOTICE\_SENT | 출발 알림 푸시 발송(세션당 1번) | 차량 · 보호자 | O |
| 차량 | ETA\_UPDATED | SIG-01이 들어올 때(15초마다) | 차량(SSE)만. 모바일은 SES-03 조회로 봄 | X |
|  | VEHICLE\_LOCATION | SIG-01의 위치 | 없음(푸시 안 함). 자녀 합류 안내 화면은 SES-03 조회로 차량 위치를 봄 | X |
|  | VEHICLE\_STOPPED | SIG-02 STOPPED | 차량 · 보호자 · 자녀 | O |
|  | VEHICLE\_DEPARTED | SIG-02 DEPARTED | 차량 · 보호자 · 자녀 | O |
|  | BOARDING\_DETECTED | SIG-02 DOOR\_CYCLE로 탑승 후보(10초 취소 창 시작) | 차량 · 보호자 | O |
| 호출 | CALL\_TRIGGERED | 호출 조건 충족, Call 생성(ACCEPTED) | 차량 · 보호자 | O |
|  | CALL\_SENT | 합류 안내 FCM 발송 | 차량 · 보호자 | O |
|  | CALL\_DELIVERED | NOTI-01 수신 확인 | 차량 · 보호자 | O |
|  | CALL\_ACKNOWLEDGED | CALL-04 「확인했어요」 | 차량 · 보호자 | O |
|  | CALL\_REVOKED | CALL-03 철회, 취소 때 자동 철회 | 차량 · 보호자 · 자녀 | O |
| 자녀 | PREP\_TIME\_EXTENDED | SIG-03 준비 시간 추가 | 차량(차량 픽업 진행 화면 · 차량 준비 시간 추가 음성 안내) · 보호자 | O |
|  | HELP\_REQUESTED | SIG-05 도움 요청 | 차량 · 보호자 | O |

### 4.13 실시간 알림 흐름

차량 앱은 ETA·위치를 서버로 보내기만 하고, 「언제 아이에게 알릴지」는 서버가 판단해 보호자·자녀 앱(모바일)에는 FCM, 차량에는 SSE로 보냅니다. 모바일 앱은 앱이 열려 있을 때도 SSE를 쓰지 않고, 차량은 FCM을 받을 필요가 없습니다. 단, FCM은 상태가 바뀌는 이벤트(출발·호출·정차·완료 등)에만 쓰고, 15초마다 바뀌는 ETA·차량 위치는 FCM으로 보내지 않습니다(FCM은 잦은 발송을 묶거나 버림). 모바일 앱은 화면이 켜진 동안 SES-03을 15초마다 조회해서 ETA·위치를 갱신합니다.

**[그림 → 텍스트] 차량은 보내기만 하고, 판단과 알림 발송은 서버가 한다**

1. 차량 앱(Pleos): NaviHelper SDK에서 ETA·남은 거리·위치를 읽음. 판단은 하지 않고 서버로 보내기만 함.
   → SIG-01 · HTTPS POST · 15초마다 → pickup 서버
2. pickup 서버:
   - ① ETA·남은 거리를 세션 컬럼에 덮어씀(원본 저장 안 함)
   - ② 길안내 시작(SES-04) 때 자녀에게 출발 알림
   - ③ ETA ≤ 3분 또는 ≤ 700m → 가드 G1~G5 검사(준비 시간 포함)
   - ④ 통과하면 Call·Notification 저장 후 이벤트 발행
   - pickup 서버 → 차량 앱: SSE(SES-08) — 화면 전환, 음성 안내
   → Spring 이벤트 → notification
3. notification: 커밋 뒤 이벤트를 받아 FCM으로 발송(@Async). 미확인이면 재발송, 보호자 화면에 「수신 미확인」.
   → FCM 푸시 → 자녀 앱, 보호자 앱
4. 자녀 앱(모바일): 출발 알림 · 합류 안내 · 철회. 상태 변화는 FCM, ETA는 15초마다 조회.
   - 자녀 앱 → pickup 서버: NOTI-01 수신, CALL-04 열람
5. 보호자 앱(모바일): 진행 상태 · 지연 · 예외. 공동 보호자·대리보호자 포함.

판단을 서버에 둔 이유는 가드 G1\~G5가 세션·장소·경로·ETA·준비 시간을 모두 봐야 하고, Call 생성과 Notification 행 기록을 한 트랜잭션으로 묶어야 중복 발송과 누락을 막을 수 있기 때문입니다.

**알림 발송 방식 (Spring 이벤트)**

알림은 별도 메시지 큐나 Outbox 테이블 없이 Spring 내부의 ApplicationEventPublisher로 보냅니다. 서버가 하나라 같은 JVM 안의 이벤트로 충분합니다.

1. 서비스가 트랜잭션 안에서 Call·Notification 행을 저장하고, ApplicationEventPublisher로 이벤트(예: CallSentEvent)를 발행합니다.
2. 받는 쪽은 @TransactionalEventListener(phase = AFTER\_COMMIT)와 @Async로 커밋이 끝난 뒤 별도 스레드에서 처리합니다. 롤백되면 아무것도 나가지 않고, FCM이 느려도 API 응답은 기다리지 않습니다.
3. 같은 이벤트를 두 리스너가 받습니다. notification 모듈은 보호자·자녀 앱에 FCM을, pickup 모듈은 차량에 SSE를 보냅니다.
4. 발송 결과는 Notification 행의 sentAt에 남깁니다. 메모리 이벤트라 서버가 발송 도중 꺼지면 사라질 수 있으므로, 재발송 스케줄러가 1분마다 sentAt 또는 deliveredAt이 비어 있는 행을 찾아 다시 보냅니다(최대 2번).

**차량 전송 주기**

| 상황 | 전송 | 이유 |
| --- | --- | --- |
| 픽업 진행 중(세션 ACTIVE) | SIG-01, 15초마다(고정) | 700m는 시속 30km로도 1분 넘게 걸려 15초 간격이면 놓치지 않음 |
| 경로 변경 · 정차 · 출발 · 도어 | SIG-02, 즉시 | 상태가 바뀐 순간이라 기다릴 이유가 없음 |

처음에는 15초로 고정합니다. 남은 거리에 따라 30·15·5초로 바꾸는 방식은 8장 추후 개발 사항입니다. Pleos 앱 성능 기준(CPU·배터리·폴링 최소화)에 맞는지는 확인이 필요합니다.

**서버 기준값**

| 기준 | 값 | 동작 |
| --- | --- | --- |
| ETA 유효시간 | 1분 | 차량 신호(SIG-01)가 1분 동안 없으면 ETA를 믿지 않음(eta.stale = true). 호출을 보내지 않고 화면에 「시간 확인 중」 |
| 가드 재평가 주기 | 차량 신호마다(15초) | 차량 신호(SIG-01)가 올 때마다 호출 조건(가드 G1\~G5)을 다시 검사. 따로 도는 타이머는 없으므로 callNotBefore가 지난 뒤 최대 15초 늦게 호출될 수 있음(허용) |
| 허용 실외 대기시간 | 5분 | 합류 호출 뒤 5분이 지나도 차가 정차하지 않으면 보호자에게 FCM 알림 |
| 지연 판정 | 10분 | 목표 픽업 시간보다 10분 넘게 늦으면 보호자 화면에 「지연」 표시 |

값은 application.yml에 두어 코드 수정 없이 바꿀 수 있게 합니다. 차량 전송 간격이 15초에서 바뀌면 ETA 유효시간은 간격의 4배로 함께 바꿉니다(예: 30초 간격이면 2분).

**실패했을 때**

- 차량 전송이 끊기면(ETA\_STALE) 호출을 보내지 않고 자녀는 실내 대기를 유지합니다(가드 G4). 전송이 돌아오면 다시 판단합니다.
- FCM 발송이 실패하거나 아이가 「확인했어요」(CALL-04)를 누르지 않으면 1분마다 최대 2번 더 보내고, 보호자 화면에 「수신 미확인」과 직접 연락 버튼을 띄웁니다. 재발송은 같은 Call에 Notification 행을 하나 더 쓰는 것이라 테이블이 늘지 않습니다.
- 자녀가 앱을 열면 푸시와 관계없이 SES-03으로 최신 상태를 다시 읽어, 받지 못한 알림이 있어도 화면은 맞게 보입니다.
- 차량 앱이 내비 뒤(백그라운드)에 있어도 SIG-01은 계속 보내야 합니다. Pleos가 Android 기반이라 Foreground Service(위치 타입)로 보내는 것으로 가정하고, 시연 전에 에뮬레이터나 실차에서 확인합니다(7.1).

### 4.14 API 상세 명세

4.2\~4.11 표의 API를 하나씩 풀어 쓴 명세입니다. 표와 다르면 이 절이 기준입니다.

**읽는 법**

- 맨 위 표: Method · URL, 누가 부르는지, 어느 화면에서 쓰는지, Idempotency-Key가 필요한지
- Path Parameter: URL 안의 `{값}` · Query Parameter: URL 뒤 `?이름=값` · Request Body: JSON 본문
- Response: 성공 코드, 응답 필드, 예시. 시각은 모두 한국 시간 `+09:00`으로 내려갑니다(4.1)
- Error: 그 API에만 있는 오류. 아래 공통 오류는 모든 API에 해당합니다
- 필드 이름의 `a.b`는 객체 a 안의 b, `a[].b`는 배열 a의 각 항목 안의 b입니다
- 인증이 필요한 API는 모두 `Authorization: Bearer {accessToken}` 헤더를 보냅니다. ID 값은 모두 숫자(BIGINT)입니다

**공통 오류**

| HTTP | code | 언제 |
| --- | --- | --- |
| 400 | INVALID\_REQUEST | JSON 형식이 틀림 |
| 401 | UNAUTHORIZED | 토큰이 없거나 만료·폐기됨 |
| 403 | FORBIDDEN | 토큰의 role로는 할 수 없는 동작 |
| 404 | NOT\_FOUND | 대상이 없거나 다른 가족의 것 |
| 409 | STATE\_VERSION\_CONFLICT | 보낸 stateVersion이 서버와 다름. 응답에 최신 세션 뷰가 함께 감 |
| 409 | IDEMPOTENCY\_KEY\_REUSED | 같은 Idempotency-Key로 다른 내용을 보냄 |
| 422 | VALIDATION\_FAILED | 필드 검증 실패. fields에 어느 필드가 왜 틀렸는지 담음 |
| 429 | TOO\_MANY\_REQUESTS | 횟수 제한. retryAfterSeconds에 다시 시도할 수 있는 초 |

```json
{ "error": { "code": "VALIDATION_FAILED", "message": "입력값을 확인해 주세요", "retryAfterSeconds": null, "fields": [{ "field": "name", "reason": "1~20자여야 합니다" }] } }
```

**공통 응답: 세션 뷰 (SessionView)**

세션을 조회하거나 바꾸는 API(SES-02\~07, SES-11\~13, CALL-01·03)는 성공하면 이 모양을 돌려줍니다. 예시는 4.12에 있습니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer |  |
| stateVersion | integer | 바뀔 때마다 +1. 다음 요청에 그대로 보냄 |
| status.session | string | ACTIVE · ON\_HOLD · COMPLETED · CANCELLED |
| status.vehicle | string | ROUTING · APPROACHING · STOPPED · DEPARTED |
| status.spot | string | SELECTED · ROUTING · CONFIRMED · INVALIDATED |
| status.call | string | NONE · ACCEPTED · SENT · DELIVERED · ACKNOWLEDGED · REVOKED |
| spot.spotId | integer | 장소 확정 전 자녀에게는 null |
| spot.name | string |  |
| spot.spotVersion | integer | 장소 변경(SES-05)에 그대로 보냄 |
| spot.locked | boolean | 자녀가 이동 중이라 장소를 바꿀 수 없으면 true |
| eta.stopEtaAt | string(datetime) | 도착 예정 시각 |
| eta.remainingSeconds | integer |  |
| eta.updatedAt | string(datetime) |  |
| eta.stale | boolean | 60초 넘게 갱신이 없으면 true(「시간 확인 중」) |
| call.mode | string | AUTO · GUARDIAN\_CONFIRM |
| call.callId | integer | 없으면 null |
| call.held | boolean | 발송 전 보류 중인지 |
| call.scheduledAt | string(datetime) | 합류 안내 예정 시각 |
| call.guard.met | boolean | 가드 G1\~G5를 모두 통과했는지 |
| call.guard.unmet | string\[\] | 통과하지 못한 가드 |
| call.progress.acceptedAt · sentAt · deliveredAt · acknowledgedAt | string(datetime) | 호출 단계별 시각 |
| children\[\].childUserId | integer |  |
| children\[\].name | string |  |
| children\[\].phase | string | WAITING · CALLED · MEETING · NOT\_BOARDED |
| children\[\].extraPrepMinutes | integer | 0\~15 |
| children\[\].callNotBefore | string(datetime) | 준비 시간을 더했으면 호출 가능 시각, 없으면 null |
| children\[\].location.lat · lng · at | number · number · string(datetime) | 자녀 최신 위치. 호출 전·탑승 후·동의 없음이면 location = null. 차량·보호자에게만 |
| children\[\].acknowledgedAt | string(datetime) | 「확인했어요」 시각 |
| children\[\].boardedAt | string(datetime) | 탑승 시각 |
| route.routeVersion | integer |  |
| route.remainingMeters | integer |  |
| boarding.detectedAt · cancelableUntil | string(datetime) | 탑승 인식 시각과 10초 취소 창 마감. 없으면 boarding = null |
| boarding.door | string | 인식한 문 |
| incident.type · childUserId · openedAt |  | 열린 예외. 없으면 incident = null |

**인증·기기 (auth)**

#### AUTH-01 토큰 갱신

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/auth/refresh` |
| 호출 | 모든 앱 (refresh 토큰으로 인증) |
| 사용 화면 | 모든 화면(토큰 만료 시 자동) |
| Idempotency-Key | 불필요 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| refreshToken | string | O |  |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| accessToken | string | 새 access 토큰 |
| refreshToken | string | 새 refresh 토큰. 이전 것은 폐기(회전) |
| accessTokenExpiresAt | string(datetime) | access 토큰 만료 시각 |

```json
{ "accessToken": "eyJ…", "refreshToken": "eyJ…", "accessTokenExpiresAt": "2026-11-05T21:30:00+09:00" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 401 | REFRESH\_TOKEN\_INVALID | 만료·폐기된 토큰. 보호자는 QR 재연결, 자녀는 재초대로 복구 |

#### AUTH-02 차량 기기 등록

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/vehicle-devices` |
| 호출 | 차량 앱 (인증 없음) |
| 사용 화면 | 차량 연결 화면(최초 실행) |
| Idempotency-Key | 필요 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| vin | string | O | 17자, 영대문자·숫자(I·O·Q 제외) |
| model | string | O | 1\~50자 |

**Response** `201`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| deviceId | integer | 등록된 기기 |
| vehicleId | integer | 차량. 아직 보호자와 연결되지 않은 VIN만 등록됨 |
| deviceToken | string | 연결 전 전용 토큰. LINK-01·02만 호출 가능 |

```json
{ "deviceId": 301, "vehicleId": 77, "deviceToken": "eyJ…" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | VEHICLE\_ALREADY\_LINKED | 이미 보호자가 연결된 VIN. 그 차량의 기기 토큰으로만 LINK-01 가능 |

VIN은 앞유리 아래에서 누구나 볼 수 있습니다. 이 거절이 없으면 VIN만 알아도 AUTH-02 → LINK-01 → LINK-04(RELINK)로 기존 보호자 계정을 가져갈 수 있습니다. 차량 앱을 다시 설치해 기기 토큰을 잃은 경우는 보호자가 LINK-05로 연결 해제한 뒤 NEW로 다시 연결합니다(승인 기반 복구는 8장 추후 개발 사항).

#### AUTH-03 푸시 토큰 갱신

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PUT /v1/devices/me/push-token` |
| 호출 | 보호자·자녀 앱, 차량 앱 |
| 사용 화면 | 앱 실행 시, Firebase가 새 토큰을 줄 때 |
| Idempotency-Key | 불필요 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| platform | string | O | ANDROID · IOS · VEHICLE |
| pushToken | string | O | 1\~255자. Firebase SDK가 만든 값 |

**Response** `204`: 본문 없음

#### AUTH-04 차량 로그아웃

| 항목 | 내용 |
| --- | --- |
| Method · URL | `DELETE /v1/vehicle-devices/me/session` |
| 호출 | 차량 앱 |
| 사용 화면 | 차량 설정 요약 화면 |
| Idempotency-Key | 불필요 |

**Response** `204`: 본문 없음

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | ACTIVE\_PICKUP\_EXISTS | 진행 중인 픽업이 있음 |

**차량 연결 (auth, LINK-05만 user)**

#### LINK-01 연결 코드 발급

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/vehicle-link-codes` |
| 호출 | 차량 앱 (연결 전 토큰 가능) |
| 사용 화면 | 차량 연결 화면 |
| Idempotency-Key | 불필요 |

**Request Body**: 없음

**Response** `201`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| codeId | integer | 코드 id. LINK-02 폴링에 씀 |
| code | string | 8자 연결 코드 |
| qrPayload | string | QR에 담을 문자열 |
| expiresAt | string(datetime) | 발급 후 5분 |
| reissuesLeft | integer | 남은 재발급 횟수 |

```json
{ "codeId": 5012, "code": "K7P2QX9M", "qrPayload": "pickup://link?code=K7P2QX9M", "expiresAt": "2026-11-05T21:10:00+09:00", "reissuesLeft": 4 }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 429 | LINK\_CODE\_REISSUE\_LIMIT | 연속 5번 넘게 발급 |

새 코드를 받으면 이전 코드는 바로 폐기됩니다.

#### LINK-02 연결 상태 조회 (폴링)

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/vehicle-link-codes/{codeId}/status` |
| 호출 | 차량 앱 (AUTH-02로 받은 연결 전 기기 토큰) |
| 사용 화면 | 차량 연결 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| codeId | integer | LINK-01에서 받은 코드 id |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| status | string | WAITING(아직 승인 전) · LINKED(새 연결) · RELINKED(다시 로그인) · EXPIRED(코드 만료) |
| vehicleId | integer | LINKED·RELINKED일 때 |
| familyId | integer | LINKED·RELINKED일 때 |
| accessToken | string | 차량 토큰. LINKED·RELINKED일 때 한 번만 |
| refreshToken | string | 같음 |
| pollAfterSeconds | integer | WAITING일 때 다음 호출까지 기다릴 초(기본 2) |

```json
{ "status": "LINKED", "vehicleId": 77, "familyId": 12, "accessToken": "eyJ…", "refreshToken": "rt_…", "pollAfterSeconds": null }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 403 | NOT\_CODE\_OWNER | 코드를 발급한 기기가 아님 |
| 404 | LINK\_CODE\_INVALID | 없는 코드이거나 토큰을 이미 내려주고 폐기된 코드 |
| 429 | TOO\_MANY\_REQUESTS | 1초보다 자주 호출 |

차량 앱은 QR이 떠 있는 동안 2초마다 부릅니다(코드 유효 5분 동안 최대 150번). LINKED·RELINKED를 받으면 토큰을 저장하고 1초 뒤 차량 오늘의 픽업 화면으로, EXPIRED면 LINK-01로 새 코드를 받습니다. 토큰을 내려준 뒤 코드는 바로 폐기합니다.

#### LINK-03 연결 코드 확인

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/vehicle-link-codes/by-code/{code}` |
| 호출 | 보호자 앱 (인증 없음, 로그인 전) |
| 사용 화면 | 보호자 QR 스캔 화면, 보호자 연결 승인 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| code | string | 8자. QR에서 읽었거나 직접 입력한 값 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| status | string | NEW(처음 연결) · RELINK(이미 가족에 연결된 차량 = 계정 복구) |
| vehicleModel | string | 차종 |
| plateNumberMasked | string | 번호판, 뒷자리 가림 |
| expiresAt | string(datetime) | 코드 만료 시각 |

```json
{ "status": "NEW", "vehicleModel": "아이오닉 5", "plateNumberMasked": "12가 ****", "expiresAt": "2026-11-05T21:10:00+09:00" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 404 | LINK\_CODE\_INVALID | 없는 코드 |
| 410 | LINK\_CODE\_EXPIRED | 만료·사용된 코드 |
| 429 | LINK\_CODE\_ATTEMPT\_LIMIT | 같은 기기에서 5번 실패. 1분 뒤 다시 |

status는 연결 중인 차량(unlinkedAt이 비어 있는 행)만 보고 정합니다. 이 API는 연결을 만들지 않습니다.

#### LINK-04 연결 승인 (가입·로그인)

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/vehicle-links` |
| 호출 | 보호자 앱 (인증 없음) |
| 사용 화면 | 보호자 연결 승인 화면 |
| Idempotency-Key | 필요 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| code | string | O | 8자 |
| mode | string | O | NEW · RELINK. LINK-03 결과와 같아야 함 |
| consents | array | O | 동의 목록. 필수 TERMS · PRIVACY |
| consents\[\].type | string | O | TERMS · PRIVACY · MARKETING |
| consents\[\].agreed | boolean | O |  |
| platform | string | O | ANDROID · IOS |
| pushToken | string | - | 있으면 기기(Device)에 바로 저장 |

**Response** `201`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| vehicleId | integer |  |
| familyId | integer |  |
| userId | integer | 보호자(GUARDIAN) |
| accessToken | string |  |
| refreshToken | string |  |

```json
{ "vehicleId": 77, "familyId": 12, "userId": 101, "accessToken": "eyJ…", "refreshToken": "eyJ…" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 404 | LINK\_CODE\_INVALID | 없는 코드 |
| 410 | LINK\_CODE\_EXPIRED | 만료·사용된 코드 |
| 409 | LINK\_MODE\_MISMATCH | mode가 실제 상태와 다름 |
| 422 | CONSENT\_REQUIRED | 필수 동의 누락 |
| 422 | VEHICLE\_MOVING | 차량이 주행 중 |

NEW는 가족·보호자를 만들고 차량을 잇습니다. RELINK는 기존 보호자에게 새 토큰을 주고 이전 기기 토큰을 폐기한 뒤, 이전 기기에 「다른 기기에서 로그인했어요」 푸시를 보냅니다. RELINK 코드는 이미 연결된 차량 기기만 만들 수 있으므로(AUTH-02의 409 VEHICLE\_ALREADY\_LINKED) 차 안에서 QR을 찍는 사람만 RELINK할 수 있습니다. 「차에 타는 것 자체가 인증」이라는 전제는 이 거절이 있어야 성립합니다. 성공하면 코드는 바로 사용 처리됩니다.

#### LINK-05 차량 연결 해제

| 항목 | 내용 |
| --- | --- |
| Method · URL | `DELETE /v1/vehicle-links/{vehicleId}` |
| 호출 | 주 보호자(GUARDIAN) |
| 사용 화면 | 차량 설정 요약 화면, 보호자 가족 프로필 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| vehicleId | integer |  |

**Response** `204`: 본문 없음

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | ACTIVE\_PICKUP\_EXISTS | 진행 중인 픽업이 있음 |

행을 지우지 않고 unlinkedAt만 채웁니다. 같은 차를 다시 연결하면 NEW로 새 vehicleId가 생깁니다.

**가족 멤버 (user, 초대 FAM-04\~06은 auth)**

#### FAM-01 가족 조회

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/families/me` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 가족 프로필 화면, 보호자 설정 완료 화면 |
| Idempotency-Key | 불필요 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| familyId | integer |  |
| joinCallMode | string | AUTO · GUARDIAN\_CONFIRM |
| vehicle | object | 연결된 차량. 없으면 null |
| vehicle.vehicleId | integer |  |
| vehicle.model | string |  |
| vehicle.plateNumberMasked | string |  |
| members\[\].userId | integer |  |
| members\[\].role | string | GUARDIAN · CO\_GUARDIAN · DELEGATE · TEACHER · CHILD |
| members\[\].name | string |  |
| members\[\].phone | string | 없으면 null |
| members\[\].deviceStatus | string | CONNECTED · INVITE\_PENDING · INVITE\_EXPIRED · NONE(기기 없음) |

```json
{
  "familyId": 12,
  "joinCallMode": "AUTO",
  "vehicle": { "vehicleId": 77, "model": "아이오닉 5", "plateNumberMasked": "12가 ****" },
  "members": [
    { "userId": 101, "role": "GUARDIAN", "name": "김보호", "phone": "+821012345678", "deviceStatus": "CONNECTED" },
    { "userId": 131, "role": "CHILD", "name": "민준", "phone": null, "deviceStatus": "INVITE_PENDING" }
  ]
}
```

#### FAM-02 멤버 등록

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/families/me/members` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 가족 프로필 화면 |
| Idempotency-Key | 필요 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| role | string | O | CHILD · CO\_GUARDIAN · DELEGATE · TEACHER |
| name | string | O | 1\~20자, 이모지·제어문자 불가 |
| phone | string | - | 국제 형식(+8210…). 자녀 외에는 필수 |

**Response** `201`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| userId | integer | 새 멤버 |

```json
{ "userId": 131 }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 422 | MEMBER\_LIMIT\_EXCEEDED | 자녀 외 멤버가 이미 5명 |
| 403 | FORBIDDEN | 공동 보호자가 공동 보호자를 추가하려 함 |

#### FAM-03 멤버 수정

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PATCH /v1/families/me/members/{userId}` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 가족 프로필 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| userId | integer | 수정할 멤버 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| name | string | - | FAM-02와 같음 |
| phone | string | - | FAM-02와 같음 |
| role | string | - | 주 보호자만 바꿀 수 있음. 주 보호자 자신의 role은 못 바꿈 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| userId | integer |  |
| role | string | GUARDIAN · CO\_GUARDIAN · DELEGATE · TEACHER · CHILD |
| name | string |  |
| phone | string | 없으면 null |
| deviceStatus | string | CONNECTED · INVITE\_PENDING · INVITE\_EXPIRED · NONE(기기 없음) |

```json
{ "userId": 131, "role": "CHILD", "name": "민준", "phone": null, "deviceStatus": "CONNECTED" }
```

보낸 필드만 바뀝니다.

#### FAM-04 초대 링크 발급

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/families/me/members/{userId}/invites` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 가족 프로필 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| userId | integer | 초대할 멤버 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| replaceDevice | boolean | - | 기본 false. 이미 연결된 기기를 바꿀 때 true |

**Response** `201`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| inviteUrl | string | 초대 링크 |
| expiresAt | string(datetime) | 발급 후 24시간 |

```json
{ "inviteUrl": "https://app.example/invite/9f3c…", "expiresAt": "2026-11-06T21:05:00+09:00" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | DEVICE\_ALREADY\_CONNECTED | 기기가 연결돼 있는데 replaceDevice = false |

1회용이고, 다시 발급하면 이전 링크는 폐기됩니다.

#### FAM-05 초대 확인

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/invites/{token}` |
| 호출 | 자녀·선생님·대리보호자 앱 (인증 없음, 로그인 전) |
| 사용 화면 | 자녀 가족 연결 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| token | string | 초대 링크의 토큰 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| inviterName | string | 초대한 보호자 |
| memberName | string | 초대받은 멤버 |
| role | string |  |
| requiredConsents | string\[\] | 이 role에 필요한 동의. 자녀는 TERMS · PRIVACY · LOCATION · PUSH |
| expiresAt | string(datetime) |  |

```json
{ "inviterName": "김보호", "memberName": "민준", "role": "CHILD", "requiredConsents": ["TERMS", "PRIVACY", "LOCATION", "PUSH"], "expiresAt": "2026-11-06T21:05:00+09:00" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | INVITE\_ALREADY\_USED | 이미 수락한 링크 |
| 410 | INVITE\_EXPIRED | 만료된 링크 |

#### FAM-06 초대 수락

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/invites/{token}/acceptance` |
| 호출 | 자녀·선생님·대리보호자 앱 (인증 없음) |
| 사용 화면 | 자녀 가족 연결 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| token | string |  |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| consents | array | O | FAM-05의 requiredConsents를 모두 agreed = true로 |
| consents\[\].type | string | O |  |
| consents\[\].agreed | boolean | O |  |
| platform | string | O | ANDROID · IOS |
| pushToken | string | - | 있으면 기기에 바로 저장 |

**Response** `201`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| userId | integer |  |
| familyId | integer |  |
| role | string |  |
| accessToken | string |  |
| refreshToken | string |  |

```json
{ "userId": 131, "familyId": 12, "role": "CHILD", "accessToken": "eyJ…", "refreshToken": "eyJ…" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | INVITE\_ALREADY\_USED | 이미 수락한 링크 |
| 410 | INVITE\_EXPIRED | 만료된 링크 |
| 422 | CONSENT\_REQUIRED | 필수 동의 누락 |

#### FAM-07 멤버 해제

| 항목 | 내용 |
| --- | --- |
| Method · URL | `DELETE /v1/families/me/members/{userId}` |
| 호출 | 주 보호자 |
| 사용 화면 | 보호자 가족 프로필 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| userId | integer |  |

**Response** `204`: 본문 없음

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | MEMBER\_IN\_ACTIVE\_PICKUP | 진행 중인 픽업에 묶인 멤버 |
| 422 | CANNOT\_REMOVE\_GUARDIAN | 주 보호자 자신을 해제하려 함 |

그 멤버의 기기와 토큰도 함께 폐기됩니다.

**일정·설정 (schedule, SET-06만 user)**

#### SET-01 학원 검색

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/places/academies` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 학원·일정 화면 |
| Idempotency-Key | 불필요 |

**Query Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| query | string | O | 2\~50자 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| items\[\].externalPlaceId | string | 외부 지도 API의 장소 id |
| items\[\].name | string |  |
| items\[\].address | string |  |
| items\[\].location | object | { lat, lng } |

```json
{ "items": [{ "externalPlaceId": "kakao:12345", "name": "해솔영어학원", "address": "서울 강남구 …", "location": { "lat": 37.501234, "lng": 127.039876 } }] }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 502 | MAP\_API\_UNAVAILABLE | 외부 지도 API 오류 |

결과는 저장하지 않고 그대로 중계합니다. 학원(Academy)은 SET-02로 일정을 저장할 때 만들어집니다.

#### SET-02 일정 등록

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/children/{childId}/schedules` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 학원·일정 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| childId | integer | 자녀(role = CHILD)의 userId |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| academy | object | O | 학원 정보 |
| academy.externalPlaceId | string | O | SET-01에서 고른 학원 |
| academy.name | string | O |  |
| academy.address | string | O |  |
| academy.location | object | O | { lat, lng } |
| academy.entrance | object | - | 출입구 { lat, lng }. 없으면 일정이 DRAFT |
| academy.phone | string | - | 대표번호 |
| weekdays | string\[\] | O | MON\~SUN, 1개 이상 |
| classEndTime | string(HH:MM) | O | 한국 시간 |
| targetPickup | integer | O | 수업 종료 후 몇 분 뒤인지. 0 = 종료 즉시, 5 = 5분 후, 직접 = 0\~60 |

**Response** `201`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| scheduleId | integer |  |
| academyId | integer |  |
| status | string | READY · DRAFT(출입구 없음, 픽업 시작 불가) |

```json
{ "scheduleId": 801, "academyId": 55, "status": "READY" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | SCHEDULE\_OVERLAP | 같은 아이의 겹치는 일정이 있음. overlapScheduleIds 함께 |
| 422 | DAILY\_SCHEDULE\_LIMIT | 하루 일정이 3개를 넘음 |

#### SET-03 일정 수정

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PUT /v1/schedules/{scheduleId}` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 학원·일정 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| scheduleId | integer |  |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| academy | object | O | 학원 정보 |
| academy.externalPlaceId | string | O | SET-01에서 고른 학원 |
| academy.name | string | O |  |
| academy.address | string | O |  |
| academy.location | object | O | { lat, lng } |
| academy.entrance | object | - | 출입구 { lat, lng }. 없으면 일정이 DRAFT |
| academy.phone | string | - | 대표번호 |
| weekdays | string\[\] | O | MON\~SUN, 1개 이상 |
| classEndTime | string(HH:MM) | O | 한국 시간 |
| targetPickup | integer | O | 수업 종료 후 몇 분 뒤인지. 0 = 종료 즉시, 5 = 5분 후, 직접 = 0\~60 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| scheduleId | integer |  |
| academyId | integer |  |
| status | string | READY · DRAFT |

```json
{ "scheduleId": 801, "academyId": 55, "status": "READY" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | SCHEDULE\_OVERLAP | SET-02와 같음 |
| 422 | DAILY\_SCHEDULE\_LIMIT | SET-02와 같음 |

진행 중인 세션에는 적용되지 않고 다음 세션부터 적용됩니다(세션 시작 때 스냅샷).

#### SET-04 일정 목록

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/children/{childId}/schedules` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 학원·일정 화면, 보호자 설정 완료 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| childId | integer | 자녀(role = CHILD)의 userId |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| items\[\].scheduleId | integer |  |
| items\[\].academy.academyId | integer |  |
| items\[\].academy.name | string |  |
| items\[\].weekdays | string\[\] |  |
| items\[\].classEndTime | string(HH:MM) |  |
| items\[\].targetPickup | integer | 종료 후 분 |
| items\[\].status | string | READY · DRAFT |

```json
{ "items": [{ "scheduleId": 801, "academy": { "academyId": 55, "name": "해솔영어학원" }, "weekdays": ["MON", "WED"], "classEndTime": "18:00", "targetPickup": 5, "status": "READY" }] }
```

#### SET-06 합류 안내 방식 변경

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PUT /v1/families/me/call-mode` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 합류 안내 방식 화면 |
| Idempotency-Key | 불필요 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| joinCallMode | string | O | AUTO · GUARDIAN\_CONFIRM |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| joinCallMode | string |  |
| settingsVersion | integer | 바뀐 설정 버전 |

```json
{ "joinCallMode": "GUARDIAN_CONFIRM", "settingsVersion": 8 }
```

기본값이 AUTO라 처음 설정할 때는 부르지 않고, 바꿀 때만 부릅니다. 다음 세션부터 적용됩니다.

#### SET-07 설정 요약

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/children/{childId}/setup-summary` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 설정 완료 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| childId | integer | 자녀(role = CHILD)의 userId |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| child.userId | integer |  |
| child.name | string |  |
| child.deviceStatus | string | FAM-01과 같음 |
| schedules\[\].scheduleId | integer |  |
| schedules\[\].academyName | string |  |
| schedules\[\].status | string |  |
| pickupSpots\[\].spotId | integer |  |
| pickupSpots\[\].name | string |  |
| pickupSpots\[\].isDefault | boolean |  |
| joinCallMode | string |  |
| missing | string\[\] | CHILD\_DEVICE · SCHEDULE · PICKUP\_SPOT. 비어 있어야 완료 가능 |
| settingsVersion | integer | SET-08에 그대로 보냄 |

```json
{
  "child": { "userId": 131, "name": "민준", "deviceStatus": "CONNECTED" },
  "schedules": [{ "scheduleId": 801, "academyName": "해솔영어학원", "status": "READY" }],
  "pickupSpots": [{ "spotId": 21, "name": "정문 앞", "isDefault": true }],
  "joinCallMode": "AUTO",
  "missing": [],
  "settingsVersion": 8
}
```

#### SET-08 설정 완료·차량 동기화

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/children/{childId}/setup-completion` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 설정 완료 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| childId | integer | 자녀(role = CHILD)의 userId |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| settingsVersion | integer | O | SET-07에서 받은 값 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| syncedToVehicle | boolean | 차량에 반영됐는지 |

```json
{ "syncedToVehicle": true }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | SETTINGS\_VERSION\_CONFLICT | 다른 기기가 먼저 설정을 바꿈. SET-07을 다시 부름 |
| 422 | SETUP\_INCOMPLETE | missing이 남아 있음 |

#### SET-09 차량용 주간 일정

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/vehicles/me/weekly-pickups` |
| 호출 | 차량 앱 |
| 사용 화면 | 차량 오늘의 픽업 화면 |
| Idempotency-Key | 불필요 |

**Query Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| weekStart | string(date) | - | 그 주 월요일. 없으면 이번 주(한국 날짜) |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| weekStart | string(date) |  |
| days\[\].date | string(date) |  |
| days\[\].pickups\[\].scheduleId | integer |  |
| days\[\].pickups\[\].childName | string |  |
| days\[\].pickups\[\].academyName | string |  |
| days\[\].pickups\[\].classEndTime | string(HH:MM) |  |
| days\[\].pickups\[\].targetPickupTime | string(HH:MM) | 종료 시각 + targetPickup |
| readiness.startable | boolean | 오늘 픽업을 시작할 수 있는지 |
| readiness.missing | string\[\] | CHILD\_DEVICE · PICKUP\_SPOT |

```json
{
  "weekStart": "2026-11-02",
  "days": [{ "date": "2026-11-05", "pickups": [{ "scheduleId": 801, "childName": "민준", "academyName": "해솔영어학원", "classEndTime": "18:00", "targetPickupTime": "18:05" }] }],
  "readiness": { "startable": true, "missing": [] }
}
```

하루 최대 3개입니다.

#### SET-10 차량용 설정 요약

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/vehicles/me/settings-summary` |
| 호출 | 차량 앱 |
| 사용 화면 | 차량 설정 요약 화면 |
| Idempotency-Key | 불필요 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| guardianNameMasked | string | 가운데 글자 가림 |
| children\[\].userId | integer |  |
| children\[\].name | string |  |
| joinCallMode | string |  |

```json
{ "guardianNameMasked": "김*호", "children": [{ "userId": 131, "name": "민준" }], "joinCallMode": "AUTO" }
```

**픽업 장소 (place)**

#### SPOT-01 저장된 장소 조회

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/families/me/pickup-spots` |
| 호출 | 보호자·공동 보호자·대리보호자, 차량 앱 |
| 사용 화면 | 보호자 픽업 장소 화면, 차량 픽업 장소 화면, 차량 예외·복구 화면, 보호자 예외·권한 관리 화면 |
| Idempotency-Key | 불필요 |

**Query Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| academyId | integer | - | 그 학원 장소만 |
| near | string | - | lat,lng. radiusM과 함께 |
| radiusM | integer | - | 50\~3000. near와 함께 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| academies\[\].academyId | integer |  |
| academies\[\].academyName | string |  |
| academies\[\].spots\[\].spotId | integer |  |
| academies\[\].spots\[\].name | string |  |
| academies\[\].spots\[\].stopPoint | object | 정차 지점 { lat, lng } |
| academies\[\].spots\[\].meetingPoint | object | 만남 지점 { lat, lng } |
| academies\[\].spots\[\].memo | string | 없으면 null |
| academies\[\].spots\[\].isDefault | boolean | 학원의 기본 장소인지 |
| academies\[\].spots\[\].walkMinutes | integer | 진행 중 세션이 있을 때만. 자녀 현재 위치에서 걸어서 몇 분 |

```json
{
  "academies": [{
    "academyId": 55, "academyName": "해솔영어학원",
    "spots": [{ "spotId": 21, "name": "정문 앞", "stopPoint": { "lat": 37.501234, "lng": 127.039876 }, "meetingPoint": { "lat": 37.501301, "lng": 127.039901 }, "memo": "편의점 앞", "isDefault": true, "walkMinutes": null }]
  }]
}
```

#### SPOT-02 장소 추가

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/families/me/pickup-spots` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 픽업 장소 화면 |
| Idempotency-Key | 필요 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| academyId | integer | O |  |
| name | string | O | 1\~50자 |
| stopPoint | object | O | { lat, lng }, 소수점 6자리 |
| meetingPoint | object | O | 같음 |
| memo | string | - | 0\~200자 |
| isDefault | boolean | - | 기본 false. 그 학원의 첫 장소면 자동 true |

**Response** `201`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| spotId | integer |  |
| isDefault | boolean |  |

```json
{ "spotId": 21, "isDefault": true }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 422 | SPOT\_TOO\_FAR | 정차 지점이나 만남 지점이 학원에서 500m를 넘음(ST\_DWithin으로 검사) |

#### SPOT-03 장소 수정

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PATCH /v1/pickup-spots/{spotId}` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 픽업 장소 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| spotId | integer |  |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| name | string | - | 1\~50자 |
| stopPoint | object | - | { lat, lng }, 소수점 6자리 |
| meetingPoint | object | - | 같음 |
| memo | string | - | 0\~200자 |
| isDefault | boolean | - | 기본 false. 그 학원의 첫 장소면 자동 true |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| spotId | integer |  |
| name | string |  |
| stopPoint | object | 정차 지점 { lat, lng } |
| meetingPoint | object | 만남 지점 { lat, lng } |
| memo | string | 없으면 null |
| isDefault | boolean | 학원의 기본 장소인지 |

```json
{ "spotId": 21, "name": "정문 앞", "stopPoint": { "lat": 37.501234, "lng": 127.039876 }, "meetingPoint": { "lat": 37.501301, "lng": 127.039901 }, "memo": null, "isDefault": true }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 422 | SPOT\_TOO\_FAR | 바꾼 좌표가 학원에서 500m를 넘음 |

보낸 필드만 바뀝니다. isDefault = true로 바꾸면 같은 학원의 이전 기본 장소는 해제됩니다.

#### SPOT-03 장소 삭제

| 항목 | 내용 |
| --- | --- |
| Method · URL | `DELETE /v1/pickup-spots/{spotId}` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 픽업 장소 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| spotId | integer |  |

**Response** `204`: 본문 없음

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | SPOT\_IN\_USE | 진행 중 세션이 쓰는 장소 |
| 422 | DEFAULT\_SPOT\_REQUIRED | 같은 학원에 다른 장소가 남아 있는데 기본 장소를 지우려 함. 먼저 다른 장소를 기본으로 지정 |

**픽업 세션 (pickup)**

#### SES-01 진행 중 세션 찾기

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/pickup-sessions/active` |
| 호출 | 모든 앱 |
| 사용 화면 | 차량 오늘의 픽업 화면, 보호자 픽업 진행 현황 화면, 자녀 실내 대기 화면 |
| Idempotency-Key | 불필요 |

**Response** `200 / 204`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer |  |
| status | string | ACTIVE · ON\_HOLD |
| stateVersion | integer |  |

```json
{ "sessionId": 1024, "status": "ACTIVE", "stateVersion": 15 }
```

진행 중 세션이 없으면 204(본문 없음). 차량은 그 차량의 세션, 사용자는 자기 가족 세션(자녀는 자기가 포함된 세션)을 찾습니다.

#### SES-02 픽업 시작

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions` |
| 호출 | 차량 앱 |
| 사용 화면 | 차량 오늘의 픽업 화면 |
| Idempotency-Key | 필요 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| scheduleIds | integer\[\] | O | 같이 태울 자녀들의 일정. 1개 이상 |
| date | string(date) | O | 한국 날짜로 오늘 |

**Response** `201 / 200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 422 | NOT\_TODAY | date가 오늘이 아님 |
| 422 | SCHEDULES\_NOT\_GROUPABLE | 같은 날·같은 학원·같은 시간대가 아닌 일정이 섞임 |
| 422 | SETUP\_INCOMPLETE | 기본 장소 또는 자녀 기기가 없음. missing 함께 |
| 409 | ANOTHER\_SESSION\_ACTIVE | 이 차량에 다른 진행 중 세션이 있음 |

새로 만들면 201, 이미 진행 중인 같은 세션이면 200으로 그 세션을 돌려줍니다.

#### SES-03 세션 조회

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/pickup-sessions/{sessionId}` |
| 호출 | 그 세션의 차량, 가족 사용자 |
| 사용 화면 | 차량 오늘의 픽업\~예외·복구 화면, 보호자 픽업 진행 현황·예외 화면, 자녀 화면 전체, 차량 상태 질문 음성 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

주체마다 보이는 필드가 다릅니다. 자녀에게는 자기 children 항목만, 장소는 SES-04 이후에만 보이고, 도착 예정 시각(eta.stopEtaAt)은 항상 보입니다.

#### SES-04 경로 확정

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PUT /v1/pickup-sessions/{sessionId}/route` |
| 호출 | 차량 앱 |
| 사용 화면 | 차량 픽업 장소 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| spotId | integer | O | 가족이 저장한 장소 |
| navRouteId | string | O | 내비 SDK가 준 경로 id |
| stateVersion | integer | O |  |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 422 | SPOT\_NOT\_SAVED | 가족이 저장하지 않은 장소 |

내비 길안내가 성공한 뒤에만 부릅니다. 처음 확정이면 자녀에게 출발 알림(도착 예정 시각 포함)을 보냅니다(세션당 1번).

#### SES-05 장소 변경

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions/{sessionId}/spot-changes` |
| 호출 | 차량 앱, 보호자·공동 보호자 |
| 사용 화면 | 차량 예외·복구 화면, 보호자 픽업 진행 현황 화면, 보호자 예외·권한 관리 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| spotId | integer | O | 저장된 다른 장소 |
| stateVersion | integer | O |  |
| spotVersion | integer | O | 화면이 보고 있던 장소 버전 |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | CALL\_ALREADY\_SENT | 합류 안내를 이미 보냄. CALL-03으로 먼저 철회 |
| 409 | CHILD\_ALREADY\_MOVING | 자녀가 이미 만남 장소로 이동 중 |

spotStatus가 SELECTED로 돌아가고, 차량은 SPOT\_CHANGED를 받아 다시 길안내한 뒤 SES-04를 부릅니다.

#### SES-06 픽업 보류

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PUT /v1/pickup-sessions/{sessionId}/hold` |
| 호출 | 차량 앱, 보호자·공동 보호자·대리보호자 |
| 사용 화면 | 차량 예외·복구 화면, 보호자 예외·권한 관리 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| stateVersion | integer | O |  |
| reason | string | - | 0\~100자 |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

sessionStatus가 ON\_HOLD가 됩니다.

#### SES-06 보류 해제

| 항목 | 내용 |
| --- | --- |
| Method · URL | `DELETE /v1/pickup-sessions/{sessionId}/hold` |
| 호출 | 차량 앱, 보호자·공동 보호자·대리보호자 |
| 사용 화면 | 차량 예외·복구 화면, 보호자 예외·권한 관리 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Query Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| stateVersion | integer | O | DELETE라 본문 대신 쿼리로 보냄 |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

sessionStatus가 ACTIVE로 돌아갑니다.

#### SES-07 픽업 취소

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions/{sessionId}/cancellation` |
| 호출 | 차량 앱, 보호자·공동 보호자 |
| 사용 화면 | 차량 예외·복구 화면, 보호자 예외·권한 관리 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| stateVersion | integer | O |  |
| reason | string | - | 0\~100자 |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | SESSION\_ALREADY\_FINISHED | 이미 완료·취소된 세션 |

보낸 합류 안내는 자동 철회되고 자녀 위치 수집도 멈춥니다.

#### SES-08 실시간 스트림 (SSE, 차량 전용)

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/pickup-sessions/{sessionId}/stream` |
| 호출 | 차량 앱 |
| 사용 화면 | 차량 화면 전체와 음성 안내 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Header**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| Last-Event-ID | integer | - | 다시 연결할 때 마지막으로 받은 seq. 그 뒤부터 이어서 보냄 |

**Response** `200 (text/event-stream)`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| event | string | 이벤트 종류(4.12 표) |
| id | integer | seq |
| data.eventId | integer |  |
| data.seq | integer | 세션 안 순번 |
| data.stateVersion | integer | 이 이벤트 시점의 세션 버전 |
| data.occurredAt | string(datetime) |  |
| data.payload | object | 이벤트마다 다름 |

```text
event: CALL_SENT
id: 42
data: {"eventId":9001,"seq":42,"stateVersion":16,"occurredAt":"2026-11-05T18:09:02+09:00","payload":{"callId":3301,"trigger":"ETA_3MIN"}}
```

30초마다 빈 주석(: ping)을 보내 연결을 유지합니다. 차량 앱은 stateVersion이 지금보다 작은 이벤트는 버리고, seq가 건너뛰면 SES-09로 채웁니다. 보호자·자녀 앱(모바일)은 SSE를 쓰지 않고 같은 이벤트를 FCM 데이터 메시지로 받습니다.

#### SES-09 놓친 이벤트 조회

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/pickup-sessions/{sessionId}/events` |
| 호출 | 그 세션의 차량, 가족 사용자 |
| 사용 화면 | 차량 앱 SSE 재연결 시, 모바일 앱이 FCM을 놓쳤거나 앱으로 돌아왔을 때 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Query Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| afterSeq | integer | O | 이 seq보다 큰 것만 |
| limit | integer | - | 기본 100, 최대 100 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| items\[\].eventId | integer |  |
| items\[\].seq | integer |  |
| items\[\].type | string |  |
| items\[\].stateVersion | integer |  |
| items\[\].occurredAt | string(datetime) |  |
| items\[\].payload | object |  |
| hasMore | boolean | 더 있으면 마지막 seq로 다시 부름 |

```json
{ "items": [{ "eventId": 9001, "seq": 42, "type": "CALL_SENT", "stateVersion": 16, "occurredAt": "2026-11-05T18:09:02+09:00", "payload": { "callId": 3301 } }], "hasMore": false }
```

DB에 저장하지 않는 ETA\_UPDATED·VEHICLE\_LOCATION은 나오지 않습니다.

#### SES-10 연락처

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/pickup-sessions/{sessionId}/contacts` |
| 호출 | 그 세션의 차량, 가족 사용자 |
| 사용 화면 | 차량 예외·복구 화면, 보호자 픽업 진행 현황·예외 화면, 자녀 안전 대기 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| items\[\].name | string |  |
| items\[\].role | string | GUARDIAN · CO\_GUARDIAN · DELEGATE · TEACHER · ACADEMY |
| items\[\].phone | string |  |

```json
{ "items": [{ "name": "김보호", "role": "GUARDIAN", "phone": "+821012345678" }, { "name": "해솔영어학원", "role": "ACADEMY", "phone": "+8225551234" }] }
```

#### SES-11 탑승 완료

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions/{sessionId}/completion` |
| 호출 | 차량 앱 |
| 사용 화면 | 차량 픽업 완료 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| mode | string | O | IMMEDIATE(도어 인식 차량에서 10초를 기다리지 않고 바로 완료) · MANUAL(도어 신호 없는 차량). 버튼은 둘 다 「탑승 확인 및 완료」 |
| stateVersion | integer | O |  |
| notBoardedChildUserIds | integer\[\] | - | 오늘 타지 않는 자녀. 이 아이들은 미탑승(NOT\_BOARDED)으로 두고 완료. 이유는 받지 않음 |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 422 | VEHICLE\_NOT\_STOPPED | MANUAL인데 정차 중이 아님 |
| 422 | CHILDREN\_NOT\_ALL\_BOARDED | 탑승하지도 않고 미탑승으로 지정되지도 않은 자녀가 있음 |

10초 자동 완료는 세션 자녀가 모두 탔을 때만 서버 타이머가 같은 처리를 합니다. 형제 중 일부만 타고 출발할 때는 운전자가 못 탄 아이를 notBoardedChildUserIds에 넣어 완료합니다. 미탑승은 새 테이블 없이 SessionChild.childPhase = NOT\_BOARDED로만 남깁니다.

#### SES-12 탑승 취소

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions/{sessionId}/boarding-rejection` |
| 호출 | 차량 앱 |
| 사용 화면 | 차량 픽업 완료 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| childUserId | integer | - | 자녀가 여러 명이면 필수 |
| stateVersion | integer | O |  |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | REJECTION\_WINDOW\_CLOSED | 탑승 인식 후 10초가 지남. SES-13 사용 |

그 아이의 boardedAt을 비웁니다.

#### SES-13 탑승 예외

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PUT /v1/pickup-sessions/{sessionId}/incident` |
| 호출 | 차량 앱 |
| 사용 화면 | 차량 픽업 완료 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| type | string | O | CHILD\_NOT\_ON\_BOARD |
| childUserId | integer | O |  |
| stateVersion | integer | O |  |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | INCIDENT\_ALREADY\_OPEN | 이미 열린 예외가 있음 |

세션이 ON\_HOLD가 되고 incident가 채워집니다. 차량은 예외·복구 화면, 보호자는 예외·권한 관리 화면, 자녀는 안전 대기 화면으로 갑니다.

**차량·자녀 신호 (pickup)**

#### SIG-01 차량 ETA·위치

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry` |
| 호출 | 차량 앱(백그라운드) |
| 사용 화면 | 화면 없음, 15초마다 자동 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| observedAt | string(datetime) | O | 측정 시각 |
| navEtaSeconds | integer | O | 내비가 준 남은 시간(초), 0 이상 |
| remainingMeters | integer | O | 남은 거리(m), 0 이상 |
| location | object | O | { lat, lng } |
| headingDeg | integer | - | 0\~359 |
| speedKmh | number | - | 0 이상 |
| gear | string | - | P · R · N · D |

**Response** `202`: 본문 없음

observedAt이 마지막 값보다 오래되면 무시합니다. 세션의 etaAt·remainingMeters·etaUpdatedAt만 덮어쓰고, 들어올 때마다 합류 호출 조건(ETA ≤ 180초 또는 ≤ 700m + 가드 G1\~G5)을 확인합니다. 자녀가 준비 시간을 더했으면(callNotBefore) 그 시각 전에는 조건이 맞아도 보내지 않습니다.

#### SIG-02 차량 이벤트

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions/{sessionId}/vehicle-events` |
| 호출 | 차량 앱(백그라운드) |
| 사용 화면 | 화면 없음, 상태가 바뀔 때 즉시 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| eventId | string | O | 차량 앱이 만든 UUID. 같은 값이면 무시(Idempotency-Key 역할) |
| type | string | O | STOPPED · DEPARTED · STOP\_FAILED · DOOR\_CYCLE |
| occurredAt | string(datetime) | O |  |
| location | object | O | { lat, lng } |
| door | string | - | DOOR\_CYCLE일 때 필수. FRONT\_RIGHT · REAR\_LEFT · REAR\_RIGHT |

**Response** `202`: 본문 없음

DOOR\_CYCLE은 정차 중이고 합류 안내를 보낸 뒤일 때만 탑승 후보가 되고, 10초 취소 창이 시작됩니다.

#### SIG-03 준비 시간 추가

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions/{sessionId}/prep-time-extensions` |
| 호출 | 자녀 앱 |
| 사용 화면 | 자녀 실내 대기 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| minutes | integer | O | 5 · 10 · 15 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| extraPrepMinutes | integer | 지금까지 더한 합계(최대 15) |
| callNotBefore | string(datetime) | 이 시각 전에는 합류 호출을 보내지 않음 |

```json
{ "extraPrepMinutes": 10, "callNotBefore": "2026-11-05T18:14:00+09:00" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 422 | PREP\_EXTENSION\_LIMIT | 합계가 15분을 넘음 |
| 409 | CALL\_ALREADY\_SENT | 합류 안내를 이미 받음 |

호출 전 실내 대기 화면에서만 누를 수 있습니다. 차량에 PREP\_TIME\_EXTENDED를 보내 픽업 진행 화면 표시와 준비 시간 추가 음성 안내를 하고, 보호자 앱에는 세션 뷰의 extraPrepMinutes로 「○○ +10분」을 보여주기만 합니다. 서버는 세션 자녀의 callNotBefore를 「지금 + 누적 추가 분」으로 저장하고, 3분·700m 조건이 맞아도 그 시각 전에는 합류 호출을 보내지 않습니다(가드 G5). 예: 도착 예정 18:12, 18:09에 +5분이면 callNotBefore = 18:14. 차량 화면에는 「민준 준비 중 · 18:14부터 호출」처럼 보여 주고, 그동안 차량은 정차해서 기다립니다.

#### SIG-04 자녀 위치

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions/{sessionId}/child-locations` |
| 호출 | 자녀 앱 |
| 사용 화면 | 자녀 합류 안내 화면, 자녀 안전 대기 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| at | string(datetime) | O | 측정 시각 |
| lat | number | O |  |
| lng | number | O |  |
| accuracyM | integer | - | 오차(m) |

**Response** `202`: 본문 없음

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 403 | LOCATION\_CONSENT\_REQUIRED | 위치 공유 동의가 없음 |
| 409 | SESSION\_ALREADY\_FINISHED | 세션이 끝나 수집 중단 |

합류 호출을 받은 순간부터 10초마다 보냅니다. 서버는 이력을 저장하지 않고 세션 자녀의 lastLat·lastLng·lastLocatedAt만 덮어쓰며(at이 저장된 값보다 오래되면 무시), 차량·보호자 화면은 세션 뷰의 children\[\].location으로 봅니다. 탑승 완료·호출 철회·픽업 취소 중 하나가 일어나면 앱이 전송을 멈추고 서버는 세 컴럼을 비우며, 그 뒤에 온 요청은 409로 돌려줍니다. 호출 전(실내 대기)에는 보내지 않습니다.

#### SIG-05 도움 요청

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions/{sessionId}/help-requests` |
| 호출 | 자녀 앱 |
| 사용 화면 | 자녀 실내 대기 화면, 자녀 합류 안내 화면, 자녀 안전 대기 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| type | string | O | NEED\_HELP · CANT\_FIND\_CAR · CONTACT\_PARENT |

**Response** `201`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| nextAllowedAt | string(datetime) | 다음에 요청할 수 있는 시각(지금 + 60초) |

```json
{ "nextAllowedAt": "2026-11-05T18:11:00+09:00" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 429 | HELP\_REQUEST\_TOO\_SOON | 같은 아이가 1분 안에 다시 요청. nextAllowedAt 함께 |

세션 이벤트 HELP\_REQUESTED로 남기고 보호자 앱에는 FCM 푸시, 차량에는 SSE로 보냅니다.

**합류 호출 (pickup)**

#### CALL-01 호출 보류

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PUT /v1/pickup-sessions/{sessionId}/call-hold` |
| 호출 | 차량 앱, 보호자·공동 보호자·대리보호자 |
| 사용 화면 | 차량 픽업 진행 화면, 보호자 픽업 진행 현황 화면, 차량 보류 음성 안내 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| stateVersion | integer | O |  |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | CALL\_ALREADY\_SENT | 이미 보냄. CALL-03 사용 |

발송 전에만 쓸 수 있고, call.held가 true가 됩니다.

#### CALL-01 보류 해제

| 항목 | 내용 |
| --- | --- |
| Method · URL | `DELETE /v1/pickup-sessions/{sessionId}/call-hold` |
| 호출 | 차량 앱, 보호자·공동 보호자·대리보호자 |
| 사용 화면 | 차량 픽업 진행 화면, 보호자 픽업 진행 현황 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Query Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| stateVersion | integer | O | DELETE라 쿼리로 보냄 |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

해제하면 바로 호출 조건을 다시 확인합니다.

#### CALL-02 합류 안내 보내기 (보호자 확인형)

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/pickup-sessions/{sessionId}/calls` |
| 호출 | 차량 앱 |
| 사용 화면 | 차량 정차·호출 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| stateVersion | integer | O |  |

**Response** `201`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| callId | integer |  |
| status | string | SENT |

```json
{ "callId": 3301, "status": "SENT" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 422 | CALL\_GUARD\_UNMET | 가드 미충족. unmet에 어떤 가드인지 |
| 409 | CALL\_ALREADY\_SENT | 이미 보냄 |
| 409 | NOT\_GUARDIAN\_CONFIRM\_MODE | 가족의 합류 안내 방식이 AUTO |

#### CALL-03 호출 철회

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/calls/{callId}/revocation` |
| 호출 | 차량 앱, 보호자·공동 보호자·대리보호자 |
| 사용 화면 | 차량 정차·호출 화면, 보호자 픽업 진행 현황 화면, 보호자 예외·권한 관리 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| callId | integer |  |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| stateVersion | integer | O |  |
| reason | string | - | 0\~100자 |

**Response** `200`: 세션 뷰(SessionView). 필드는 위 「공통 응답: 세션 뷰」, 예시는 4.12

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | CALL\_ALREADY\_REVOKED | 이미 철회됨 |

자녀는 안전 대기 화면으로 가고 childPhase는 WAITING으로 돌아갑니다. 조건이 다시 맞으면 prevCallId를 단 새 호출이 나갑니다.

#### CALL-04 「확인했어요」

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/calls/{callId}/acknowledgement` |
| 호출 | 자녀 앱 |
| 사용 화면 | 자녀 합류 안내 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| callId | integer |  |

**Request Body**: 없음

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| callId | integer |  |
| status | string | 형제 모두 확인하면 ACKNOWLEDGED |
| acknowledgedAt | string(datetime) | 이 아이가 확인한 시각 |

```json
{ "callId": 3301, "status": "ACKNOWLEDGED", "acknowledgedAt": "2026-11-05T18:09:20+09:00" }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 409 | CALL\_ALREADY\_REVOKED | 철회된 호출 |

여러 번 눌러도 첫 시각을 유지합니다. 확인 전까지는 서버가 1분마다 최대 2번 더 FCM으로 다시 보내고, 보호자 화면에는 「수신 미확인」이 표시됩니다.

**알림 (notification)**

#### NOTI-01 수신 확인

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/notifications/{notificationId}/delivery-receipt` |
| 호출 | 알림을 받은 기기의 앱 |
| 사용 화면 | 화면 없음, FCM 푸시를 받으면 자동 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| notificationId | integer | FCM 데이터에 담겨 옴 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| receivedAt | string(datetime) | O | 기기가 푸시를 받은 시각 |

**Response** `204`: 본문 없음

여러 번 와도 첫 시각만 남깁니다.

#### NOTI-02 알림 이력

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/pickup-sessions/{sessionId}/notifications` |
| 호출 | 보호자·공동 보호자·대리보호자 |
| 사용 화면 | 보호자 픽업 진행 현황 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| items\[\].notificationId | integer |  |
| items\[\].type | string | SESSION\_STARTED · DEPARTURE · CALL · PREP\_EXTENDED · HELP 등 |
| items\[\].receiverName | string | 받는 사람 |
| items\[\].title | string | 푸시 제목 |
| items\[\].sentAt | string(datetime) |  |
| items\[\].deliveredAt | string(datetime) | 없으면 null = 「수신 미확인」 |
| items\[\].openedAt | string(datetime) |  |
| items\[\].attempt | integer | 같은 내용을 몇 번째 보냈는지 |

```json
{ "items": [{ "notificationId": 7001, "type": "CALL", "receiverName": "민준", "title": "이제 나와요", "sentAt": "2026-11-05T18:09:02+09:00", "deliveredAt": "2026-11-05T18:09:04+09:00", "openedAt": null, "attempt": 1 }] }
```

#### NOTI-03 다시 보내기

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /v1/notifications/{notificationId}/resend` |
| 호출 | 보호자·공동 보호자·대리보호자 |
| 사용 화면 | 보호자 픽업 진행 현황 화면 |
| Idempotency-Key | 필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| notificationId | integer |  |

**Request Body**: 없음

**Response** `202`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| notificationId | integer | 새로 보낸 알림 |

```json
{ "notificationId": 7002 }
```

**Error**

| HTTP | code | 언제 |
| --- | --- | --- |
| 429 | RESEND\_TOO\_SOON | 같은 알림을 1분 안에 다시 보냄 |

같은 내용을 FCM으로 다시 보내고 attempt가 1 늘어납니다.

**결과·기록 (pickup)**

#### OUT-01 픽업 이력

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/children/{childId}/pickup-history` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 이용 결과 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| childId | integer | 자녀(role = CHILD)의 userId |

**Query Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| month | string(YYYY-MM) | O | 한국 기준 달 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| items\[\].date | string(date) |  |
| items\[\].sessionId | integer |  |
| items\[\].status | string | COMPLETED · CANCELLED |
| items\[\].completedAt | string(datetime) |  |

```json
{ "items": [{ "date": "2026-11-05", "sessionId": 1024, "status": "COMPLETED", "completedAt": "2026-11-05T18:12:40+09:00" }] }
```

#### OUT-02 픽업 결과

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /v1/pickup-sessions/{sessionId}/outcome` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 이용 결과 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer |  |
| completedAt | string(datetime) |  |
| spotName | string |  |
| boardingMethod | string | DOOR · IMMEDIATE · MANUAL |
| predictedStopAt | string(datetime) | 예상 정차 |
| actualStopAt | string(datetime) | 실제 정차(원본) |
| diffSeconds | integer | 실제 − 예상(초) |
| correction.status | string | 없으면 null |
| correction.correctedStopAt | string(datetime) |  |

```json
{ "sessionId": 1024, "completedAt": "2026-11-05T18:12:40+09:00", "spotName": "정문 앞", "boardingMethod": "DOOR", "predictedStopAt": "2026-11-05T18:11:00+09:00", "actualStopAt": "2026-11-05T18:11:50+09:00", "diffSeconds": 50, "correction": { "status": null, "correctedStopAt": null } }
```

#### OUT-03 결과 정정 요청

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PUT /v1/pickup-sessions/{sessionId}/outcome/correction` |
| 호출 | 보호자·공동 보호자 |
| 사용 화면 | 보호자 이용 결과 화면 |
| Idempotency-Key | 불필요 |

**Path Parameter**

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer | 세션 id |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| correctedStopAt | string(datetime) | O | 세션 날짜 안의 시각 |
| reasonCode | string | O | WRONG\_STOP\_TIME · OTHER |
| note | string | - | 0\~200자 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| sessionId | integer |  |
| completedAt | string(datetime) |  |
| spotName | string |  |
| boardingMethod | string | DOOR · IMMEDIATE · MANUAL |
| predictedStopAt | string(datetime) | 예상 정차 |
| actualStopAt | string(datetime) | 실제 정차(원본) |
| diffSeconds | integer | 실제 − 예상(초) |
| correction.status | string | 없으면 null |
| correction.correctedStopAt | string(datetime) |  |

```json
{ "sessionId": 1024, "completedAt": "2026-11-05T18:12:40+09:00", "spotName": "정문 앞", "boardingMethod": "DOOR", "predictedStopAt": "2026-11-05T18:11:00+09:00", "actualStopAt": "2026-11-05T18:11:50+09:00", "diffSeconds": 50, "correction": { "status": "REQUESTED", "correctedStopAt": "2026-11-05T18:11:30+09:00" } }
```

actualStopAt 원본은 바뀌지 않습니다.

#### OUT-04 데이터 설정

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PUT /v1/guardians/me/data-settings` |
| 호출 | 주 보호자 |
| 사용 화면 | 보호자 이용 결과 화면 |
| Idempotency-Key | 불필요 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| learningConsent | boolean | O | 픽업 기록을 분석에 쓰는 데 동의 |

**Response** `200`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| learningConsent | boolean |  |
| retentionDays | integer | 기록 보관 기간(일) |

```json
{ "learningConsent": false, "retentionDays": 90 }
```

false로 바꾸면 분석용 기록을 바로 파기 대기로 돌립니다.

## 5. 화면별 API 매핑

시트의 API 목록은 보지 않고, 화면 명세서와 Figma Description만으로 화면마다 「무엇을 보여줘야 하나 · 사용자가 무엇을 누르나 · 화면 밖에서 무엇이 들어와야 하나」를 뽑아 API를 정했습니다. 표의 ID는 앞의 API 명세에 있는 ID입니다.

### 5.1 차량 IVI 화면

| 화면 | 보여야 하는 데이터 | 사용자 동작 → API | 진입·백그라운드·실시간 |
| --- | --- | --- | --- |
| 서비스 시작·연결 | QR(8자 코드), 남은 시간, 연결 단계(코드 발급 → 보호자 승인 → 완료) | 화면 진입, 만료 10초 전 자동 재발급, 「새 연결 코드 받기」 → LINK-01 | 앱 최초 실행 시 AUTH-02(차량 기기 등록). 연결 완료·만료는 LINK-02를 2초마다 폴링해서 확인 |
| 오늘의 픽업 | 오늘 요약 문장, 주간 7일 타일 + 일정 수 점, 일정 카드(자녀·수업·종료·목표 픽업), 시작 가능 여부와 빠진 항목 | 「{목표 시간} 픽업 시작」 → SES-02 (같은 시간대·같은 학원 형제 일정은 scheduleIds\[\]로 함께) | 진입 시 SES-01(진행 중 세션이 있으면 픽업 진행·예측 화면으로), SET-09 |
| 오늘 픽업 장소 | 보호자가 지정한 기본 장소(장소명·학원명·도착 예상·기준 시각·메모), 저장된 다른 장소, 미니맵의 정차 지점·만남 지점 | 다른 저장 장소 탭은 화면 안 전환. 「이 장소로 길안내」 → 내비 SDK 성공 후 SES-04 | 진입 시 SES-03 + SPOT-01 |
| 픽업 진행·예측 | 정차 예상 시각(내비 ETA)·남은 분·「시간 확인 중」 여부(eta.stale), 확정 장소·spotVersion, 합류 안내 예정 시각·대기 사유, 아이별 추가 준비 시간(+N분), 남은 거리 | 「자동 호출 보류」 / 「보류 해제」 → CALL-01 | SES-03 + SES-08. 차량 앱은 SIG-01로 ETA·위치를 15초마다 전송 |
| 정차·호출 상태 | 발송 근거·시각, 승인·발송·수신·열람 단계, 자녀 열람·이동, 정차 확인 상태 | 「합류 안내 보내기」(보호자 확인형) → CALL-02. 「호출 보류」 → CALL-03. 「탑승 확인으로」는 화면 이동 | SES-08. 차량 앱은 SIG-02로 정차·출발 전송 |
| 픽업 완료 | 합류 → 열람 → 탑승 3단계 시각, 인식한 도어, 자동 완료까지 남은 초 | 「탑승 확인 및 완료」 → SES-11. 「아이가 타지 않았어요」 → 취소 창 안이면 SES-12, 완료 후·주행 중이면 SES-13 | 차량 앱이 SIG-02(DOOR\_CYCLE) 전송 |
| 예외·복구 | 원인 제목·부제, 아이 안전 상태, 저장된 다른 장소(보행 시간), 저장된 연락처 | 「대안 확인」 → SES-05(장소 변경) → 내비 성공 후 SES-04(확정). 호출 발송 후면 CALL-03 철회가 먼저. 「직접 연락」 → SES-10. 「보류·취소」 → SES-06 / SES-07 | 진입 시 SPOT-01 |
| 설정 요약 | 보호자 이름(마스킹)·자녀 별명, 합류 안내 방식 현재값 | 「로그아웃·계정 전환」 → AUTH-04. 차량 매각 시 연결 해제 → LINK-05 | 진입 시 SET-10 |

### 5.2 보호자 앱 화면

| 화면 | 보여야 하는 데이터 | 사용자 동작 → API | 진입·백그라운드·실시간 |
| --- | --- | --- | --- |
| QR 스캔·연결 요청 | 코드 확인 결과(차량 모델·남은 시간, 또는 만료·확인 불가·이미 연결) | QR 인식, 코드 8자 입력 → LINK-03 | 로그인 전 호출 |
| 로그인·차량 연결 | 차량 이미지·모델·번호판·남은 시간, 동의 3개(필수 2·선택 1) | 「연결 승인」 → LINK-04(보호자 계정 생성 + 토큰 발급, 별도 로그인 없음), FCM 토큰도 같은 요청에 포함 | QR 스캔 화면에서 받은 LINK-03 결과로 차량 정보 표시 |
| 가족 프로필 | 자녀 별명, 보호자·인계자와 권한, 자녀 기기 상태(수락 대기·연결 완료·만료), 멤버별 전화번호 | 자녀·선생님·대리보호자 추가 → FAM-02(role 지정), 수정 → FAM-03. 「기기 초대 보내기」·「다시 초대하기」 → FAM-04. 멤버 해제 → FAM-07 | 진입 시 FAM-01 |
| 학원·픽업 일정 | 학원·주소·출입구, 반복 요일, 수업 종료, 목표 픽업(종료 즉시·5분 후·직접), 겹치는 일정 | 학원 검색 → SET-01. 「저장」 → SET-02 / SET-03 | 진입 시 SET-04 |
| 픽업 장소 승인 | 학원 주변 지도, 핀으로 찍은 정차 지점·만남 지점, 장소 이름·메모, 저장된 장소 목록, 기본 장소 | 지도에서 장소 추가 → SPOT-02. 수정·삭제·기본 지정 → SPOT-03 | 진입 시 SPOT-01 |
| 합류 안내 방식 (자동화·안전 정책) | 합류 안내 방식(자동 / 보호자 확인형). 기본값 자동 | 변경 → SET-06 | 처음 설정 흐름에서는 건너뜀(픽업 장소 승인 → 설정 완료). 설정에서 들어와 바꿈 |
| 설정 완료·미리보기 | 자녀·일정·장소·합류 안내 방식 요약, 누락 항목 | 「완료·차량 동기화」 → SET-08 | 진입 시 SET-07 |
| 픽업 진행 현황 | 세션 헤더·갱신 시각, 장소·정차 예상·남은 시간, 자녀 타임라인과 단계별 수신 여부 | 당겨서 새로고침 → SES-03. 단계 탭 → NOTI-02. 재전송 → NOTI-03. 「보류」 → CALL-01(발송 전) / CALL-03(발송 후). 「연락」 → SES-10. 장소 변경 → SES-05 | 푸시·홈 카드 진입 시 SES-01, 화면이 켜진 동안 SES-03 15초마다 조회, 상태 변화는 FCM |
| 예외·권한 관리 | 예외 원인·시각·사유, 저장된 다른 장소와 보행 시간, 연락처 | 「대체 장소 확정」 → SES-05. 「직접 연락」 → SES-10. 「호출 철회」 → CALL-03 | 진입 시 SPOT-01, 화면이 켜진 동안 SES-03 15초마다 조회, 상태 변화는 FCM |
| 이용 결과·데이터 | 결과 요약(날짜·탑승 시각·장소), 예상 vs 실제 정차·차이, 정정 상태, 데이터 설정 | 캘린더 날짜 → OUT-01. 정정 요청 → OUT-03. 데이터 토글 → OUT-04 | 진입 시 OUT-02 |

### 5.3 자녀 앱 화면

| 화면 | 보여야 하는 데이터 | 사용자 동작 → API | 진입·백그라운드·실시간 |
| --- | --- | --- | --- |
| 가족 연결 | 초대한 보호자 이름, 동의 항목 | 「연결하기」 → FAM-06(FCM 토큰 포함) | 진입 시 FAM-05(로그인 전) |
| 실내 대기 | 실내 대기 안내, 부모님 도착 예정 시각(상시), 오늘 만날 곳(경로 확정 후에만), 차량 상태 문구 | 「준비 시간 추가」 +5 / 10 / 15분 → SIG-03. 「도움이 필요해요」 → SIG-05 | 진입 시 SES-01, 화면이 켜진 동안 SES-03 15초마다 조회(ETA), 상태 변화는 FCM. 출발 알림 푸시 수신 시 NOTI-01 |
| 합류 안내 | 도착 예정, 만나는 곳, 차량·자녀 실시간 위치, 동행 안내 | 「확인했어요」 → CALL-04. 「차를 못 찾겠어요」 → SIG-05 | 푸시 수신 시 NOTI-01. 위치는 SIG-04로 전송, 차량 위치는 SES-03 15초 조회로 받음 |
| 안전 대기·도움 요청 | 안전 대기 안내, 최신 상태·갱신 시각 | 「부모님께 연락하기」 → SIG-05 + SES-10(전화) | 상태 변화 FCM + SES-03 조회로 자동 갱신(확인 버튼 없음) |
| 픽업 완료 | 탑승 완료, 오늘 결과, 다음 픽업 | — | SES-03(완료 세션에 nextPickup 포함) |

### 5.4 차량 음성 안내

| 화면 | 보여야 하는 데이터 | 사용자 동작 → API | 진입·백그라운드·실시간 |
| --- | --- | --- | --- |
| 기본 음성 안내 (진행·보류·정차·예외·완료·상태 질문) | 발화 문구와 같은 상태 배너 | 「보류」 → CALL-01. 「지금 픽업 상태 어때?」 → SES-03 | 발화 트리거는 SES-08 이벤트. eventId로 같은 사건 1회 발화 |
| 준비 시간 추가 음성 안내 | 「○○가 준비 시간을 10분 늘렸어요」 화면 배너·음성 | — | SES-08 PREP\_TIME\_EXTENDED. eventId로 1회 발화 |

## 6. 기획·디자인 반영 사항

Figma `Wireframe` 페이지의 화면 프레임 60개(V 35 · U 8 · P 11 · C 6)에 있는 Description은 시트 화면 명세서 문구와 같습니다. 차이는 Figma 화면 레이어와 Description 사이, 그리고 시트 안에서 나옵니다. 「결정」 열이 이 문서의 API가 따른 기준입니다.

### 6.1 Figma 화면과 Description이 다른 곳

| # | 화면 | 차이 | 결정 |
| --- | --- | --- | --- |
| 1 | 차량 정차·호출 화면 (정차) | Figma는 「합류 안내 보내기」·「호출 보류」, Description은 「탑승 확인으로」·「호출 보류」 | **Figma 기준.** 「합류 안내 보내기」는 보호자 확인형일 때 CALL-02. 「호출 보류」는 발송 전 CALL-01, 발송 후 CALL-03. 탑승 확인은 도어 인식으로 차량 픽업 완료 화면에 자동 전환 |
| 2 | 차량 정차·호출 화면 (정차) | 칩이 Figma는 「승인·발송·수신·열람」, Description은 「발송·수신·열람·이동 중」 | **Figma 기준.** 승인 = ACCEPTED, 발송 = SENT, 수신 = DELIVERED(NOTI-01), 열람 = ACKNOWLEDGED(CALL-04). 자녀 이동은 칩이 아닌 별도 줄(SIG-04) |
| 3 | 차량 픽업 완료 화면 (정차) | Figma는 「탑승 확인 및 완료」·「아이가 보이지 않아요」, Description은 「지금 완료(10초 후 자동)」·「아이가 타지 않았어요」 | 완료 버튼은 Figma의 「탑승 확인 및 완료」 하나로 통일. SES-11 mode = IMMEDIATE(도어 자동 인식 차량) / MANUAL(도어 신호 없는 차량)은 서버 안에서만 구분 |
| 4 | 차량 픽업 완료 화면 (주행) | Figma는 「탑승 확인 및 완료」가 활성, Description은 비활성 「완료됨」 | 주행 중 완료 조작 불가(QA-28). SES-11 MANUAL은 정차 중만 허용 |
| 5 | 차량 오늘의 픽업 화면 (정차 Light·Portrait) | 픽업 시작 버튼이 placeholder 「새 연결 코드 받기」로 남음 | 디자인 수정만 |
| 6 | 보호자·자녀 화면 전체 | 버튼 대부분이 placeholder 「연결 코드 직접 입력」 | API 매핑은 Description 문구 기준 |
| 7 | 차량 픽업 진행 화면 (정차 Dark) | 「예측 안정」 칩과 "spotVersion 4" 노출 | 자체 예측 모델이 없어 지금은 eta.stale(60초 넘게 갱신 없음)로 「시간 확인 중」만 표시. 「예측 안정」 칩은 8장 추후 개발 사항 |
| 8 | 샘플 데이터 | 「해솔영어학원」과 「해솔수학학원」이 섞임 | 없음 |

&#32;

### 6.2 시트에서 고칠 곳

| # | 위치 | 내용 |
| --- | --- | --- |
| 1 | 화면 명세서 차량 픽업 진행 화면, 차량 정차·호출 화면, 차량 픽업 완료 화면의 「Landscape 추가 표시」 열 | `#ERROR!`. "+"로 시작한 문구가 수식으로 해석돼 사라짐. 셀 앞에 작은따옴표를 붙여 복구 |
| 2 | 화면 명세서의 차량 예외·완료·상태 질문 음성 안내 | 차량 예외 음성 안내는 이름이 「예외 안내」인데 진입 경로·발화·예외 열이 픽업 완료(EVT-14) 내용. 차량 완료 음성 안내와 차량 상태 질문 음성의 시각 대응은 차량 예외·복구 화면의 예외 카드 복사본 |
| 3 | 화면 명세서 보호자 가족 프로필 화면 「확인 Btn」 | 보호자 연결 승인 화면의 연결 승인 정책(차량 주행 상태 재검증)을 그대로 복사한 내용 |
| 4 | 이벤트·상태 EVT-07 | VehicleStopped 발생 지점이 「차량 정차·호출 화면」. 실제 출처는 차량 신호(SIG-02) |
| 5 | 핵심 기능 F1, 이벤트·상태 ERR-01·02, 가드 G2 | 장소 지정 방식 변경으로 SG-01\~12 자동 검증, 추천 비용 계산, 호출 직전 SG 재검증이 빠짐 |
| 6 | API 명세서 | 화면에 필요한 API가 많이 빠져 있음. 아래 「시트 API 명세서와 대조」 참고 |

## 7. 확인이 필요한 것

설계는 모두 정해 두었고, 여기에는 가정으로 둔 것만 남겼습니다. 직접 확인하면 이 목록에서 지웁니다.

### 7.1 시연 전에 직접 확인할 것

- [ ] **차량 앱 백그라운드 전송 (가정: 가능)**: Pleos는 Android 기반이라, Foreground Service(위치 타입)로 돌리면 내비가 화면 앞에 있어도 SIG-01을 15초마다 보내고 SSE(SES-08) 연결도 유지할 수 있다고 보고 설계함. 시연 전에 Pleos 에뮬레이터나 실차에서 ① 내비를 앞에 띄운 채 15초 전송이 계속되는지 ② SSE가 끊기지 않는지만 확인. 간격이 더 길면 전송 간격과 ETA 유효시간(간격의 4배)을 설정에서 늘리고, 아예 안 되면 차 안의 보호자 폰이 위치를 대신 보내고 ETA는 서버가 지도 API로 계산
- [ ] **11/4 기획 수정안 확정 여부 (기획팀에 확인)**: 확정이면 두 가지를 반영. ① 학원 출입구를 기본 픽업 장소로 쓰고 보호자가 직접 찍는 장소는 대안 0\~2개 → SPOT-02의 「첫 장소가 기본」과 SES-02의 「저장 장소 없으면 422」를 「출입구 좌표가 있으면 시작 가능」으로 바꿈. ② 세션이 없어도 자녀 앱에 오늘 일정·기본 장소·도움 버튼을 보여주는 화면(C01) → 자녀용 오늘 일정 조회 API 1개 추가, SES-01의 204는 그대로

## 8. 추후 개발 사항

개발 기간 안에는 만들지 않고, 개발이 끝난 뒤 추가로 진행할 항목입니다.

| 항목 | 내용 | 개발 기간에는 |
| --- | --- | --- |
| 자체 도착 예측 모델 | 내비 ETA를 보정해 정차 시각을 범위(p10 · p50 · p90)와 품질로 예측. 도착 예측(EtaPrediction) 테이블 추가. 학습 데이터는 픽업 결과의 predictedStopAt · actualStopAt | 차량 내비 ETA를 그대로 사용 |
| 「예측 안정」 칩 | 차량 픽업 진행 화면에 예측 품질 표시 | eta.stale로 「시간 확인 중」만 표시 |
| 전송 주기 단계 조절 | 남은 거리에 따라 SIG-01을 30 · 15 · 5초로 바꿔 배터리 · 트래픽 절약 | 15초 고정 |
| 차량 기기 복구 | 차량 앱 재설치로 기기 토큰을 잃었을 때 보호자 승인으로 다시 등록 | 보호자가 LINK-05 해제 후 NEW로 재연결 |
| 보호자 폰 분실 복구 | 차 없이 본인 확인으로 계정 복구 | 차 안에서 QR RELINK만 가능 |
| 서버 2대 이상 | 스케줄러 중복 실행 방지(ShedLock), Idempotency 결과 공유 | 서버 1대, DB 테이블 |
| 자녀별 탑승 확인 | 형제 중 일부만 탔을 때 아이별로 확인. 지금은 문이 한 번 열리면 세션 자녀가 모두 탄 것으로 봄 | 문 1회 = 전원 탑승 |
| 자녀 위치 이력 | 경로 이력 저장·보관 기간·삭제 스케줄러 | 최신 1점만 |

## 9. 다이어그램

앞 장의 설계를 그림으로 모았습니다. 그림과 본문이 다르면 본문이 기준이고, 설계가 바뀌면 그림도 같이 고칩니다.

### 9.1 사용자와 기능 (유스케이스)

&#91;embedded content: 유스케이스 · 사용자 5종 × 기능 묶음 5개\]

누가 어떤 기능을 쓰는지 한눈에 봅니다. 세부 권한은 2.1 역할과 권한 표가 기준입니다.

### 9.2 서버 아키텍처

&#91;embedded content: 서버 아키텍처 · 앱 3종 → 서버 1개 → DB·지도·FCM\]

서버 하나로 시작합니다. 차량은 SSE로 받고, 보호자·자녀 앱(모바일)은 상태 변화만 FCM으로 받고, ETA·차량 위치는 화면이 켜진 동안 SES-03을 15초마다 조회합니다. 알림 발송은 별도 메시지 큐 없이 Spring 내부 이벤트(ApplicationEventPublisher)로 처리합니다.

### 9.3 도메인 아키텍처

&#91;embedded content: 도메인 아키텍처 · 모듈 6개 × 헥사고날 계층\]

도메인 사이의 의존 방향은 2장 그림을, 모듈 안의 헥사고날 구조는 이 그림과 2.2 패키지 트리를 봅니다.

### 9.4 유저 플로우

&#91;embedded content: 유저 플로우 1 · 처음 쓸 때 (가입 → 초대 → 설정)\]

보호자가 설정을 끝내기 전에는 차량 오늘의 픽업 화면에서 픽업을 시작할 수 없습니다(SET-09의 readiness).

&#91;embedded content: 유저 플로우 2 · 오늘의 픽업 (시작 → 합류 → 완료)\]

같은 가로줄은 같은 시점입니다. 형제가 같이 타면 자녀 앱 칸의 흐름이 아이마다 따로 진행됩니다.

### 9.5 플로우차트

**[그림 → 텍스트] 합류 호출은 ETA 3분 또는 700m에 닿고 가드를 모두 통과할 때 한 번만 나간다**

SIG-01이 들어올 때마다(15초) 서버가 이 순서로 판단한다.

1. SIG-01 수신 (navEtaSeconds · remainingMeters · 위치)
2. 픽업 세션에 최신값 덮어쓰기 (etaAt · remainingMeters · etaUpdatedAt). ETA_UPDATED는 차량 SSE만 (모바일은 15초 조회)
3. 세션 ACTIVE이고 호출 보류(CALL-01) 아님? — 아니오 → 이번에는 보내지 않음(다음 SIG-01에서 다시 판단)
4. 아직 보낸 호출이 없음? — 아니오 → 중복 발송 안 함 (CALL-02로 보내도 409)
5. ETA ≤ 180초 또는 남은 거리 ≤ 700m? — 아니오 → 이번에는 보내지 않음(다음 SIG-01에서 다시 판단)
6. 가드 G1~G5 모두 통과? (세션·장소·경로·ETA·준비 시간) — 아니오 → 보내지 않고 사유 표시 (세션 뷰 call.guard.unmet, 차량 픽업 진행 화면 「대기 사유」)
7. 합류 호출 모드 = AUTO? — GUARDIAN_CONFIRM → 보호자 확인형: 정차·호출 화면에 「합류 안내 보내기」, 누르면 CALL-02로 생성 → 8단계로
8. 한 트랜잭션으로 저장: Call(ACCEPTED) · Notification 행 · 세션 이벤트 CALL_TRIGGERED
9. 발송기가 FCM으로 자녀 폰에 합류 안내 → Call SENT · 차량 SSE·보호자 FCM

- 철회(CALL-03)된 뒤 조건이 다시 맞으면 새 Call을 만들고 prevCallId로 이전 호출을 가리킨다
- 60초 넘게 SIG-01이 안 오면 ETA를 믿지 않음(stale) → 가드의 「ETA 유효」에서 걸러진다

SIG-01이 들어올 때마다 위에서부터 차례로 확인하고, 하나라도 걸리면 이번에는 보내지 않습니다.

**[그림 → 텍스트] 세션 자녀가 모두 타거나 미탑승으로 지정되면 픽업이 끝난다**

도어 신호로 자동 인식하는 흐름과, 운전자가 버튼으로 끝내는 흐름(SES-11)이 있다.

1. SIG-02 DOOR_CYCLE 수신 (차문 열림 → 닫힘)
2. 정차 중 · 운전석 제외 · 합류 호출 뒤? — 아니오 → 탑승 후보 아님, 무시(상태 변화 없음)
3. 10초 취소 창 시작(서버 타이머). SSE BOARDING_DETECTED → 10초 취소 창
4. 창 안에 「아이가 타지 않았어요」(SES-12, childUserId)?
   - 아니오 → 세션 자녀 모두 boardedAt 기록 → 5단계
   - 예 → 그 아이만 빼고 boardedAt(나머지 세션 자녀는 탑승 처리) → SES-13 예외 열기(CHILD_NOT_ON_BOARD): 차량 예외 · 보호자 예외 · 자녀 안전 대기로
5. 세션 COMPLETED: 픽업 결과 기록 · 위치 수집 중단 · 차량·자녀 완료 화면 · 완료 음성 · 이용 결과

운전자 버튼(SES-11): IMMEDIATE 「탑승 확인 및 완료」, MANUAL 「탑승 확인 및 완료」 → 세션 자녀 탑승 처리 후 5단계. MANUAL은 도어 신호 없는 차량 · 정차 중만.

- 도어 신호는 어느 아이가 탔는지 모르므로, 기본은 「세션 자녀 모두 탑승」이고 운전자가 못 탄 아이만 고른다
- 오늘 안 타는 아이는 SES-11에서 미탑승(NOT_BOARDED)으로 지정하고 완료. 타야 하는데 안 보이면 SES-13 예외

형제 중 일부만 타면 못 탄 아이를 미탑승(NOT\_BOARDED)으로 지정하고 완료합니다(SES-11). 타야 하는데 안 보이면 SES-13 예외로 갑니다.

### 9.6 상태 다이어그램

**[그림 → 텍스트] 세션은 차 기준으로 한 번 움직이고, 아이 단계는 세션 자녀마다 따로 움직인다**

화살표 글자 = 바꾸는 API·신호. 3장 상태값 문단과 같은 이름.

A. 픽업 세션 sessionStatus (세션 하나에 하나 · 차 한 번 운행 기준)

| 현재 | 다음 | 바꾸는 것 |
| --- | --- | --- |
| (시작) | ACTIVE | SES-02 |
| ACTIVE | ON_HOLD | 보류 SES-06 |
| ON_HOLD | ACTIVE | 해제 SES-06 |
| ACTIVE | COMPLETED(끝) | 탑승 완료 SES-11 · 10초 타이머 |
| ACTIVE · ON_HOLD | CANCELLED(끝) | 취소 SES-07 (ACTIVE·ON_HOLD 어디서든) |

B. 세션 자녀 childPhase (아이마다 하나 · 형제가 같이 타면 아이별로 따로 움직임)

| 현재 | 다음 | 바꾸는 것 |
| --- | --- | --- |
| WAITING (실내 대기 화면) | CALLED (합류 안내 화면) | 호출 발송 |
| CALLED | MEETING (만남 장소) | CALL-04 확인 |
| MEETING | 탑승 boardedAt | 탑승 확인 |
| CALLED | WAITING | 호출 철회 CALL-03 → 안전 대기 후 다시 대기 |

준비 시간 추가(SIG-03)는 상태를 바꾸지 않고 extraPrepMinutes만 늘림 · 호출 시점도 그대로.

C. 나머지 상태값 (한 방향으로만 흐름 · INVALIDATED = 끝나는 예외 상태)

- vehiclePhase: ROUTING → APPROACHING → (SIG-02 STOPPED) STOPPED → (SIG-02 DEPARTED) DEPARTED
- spotStatus: SELECTED → ROUTING → (SES-04 성공) CONFIRMED → (사용 불가) INVALIDATED
- Call.status: ACCEPTED → (FCM 발송) SENT → (NOTI-01) DELIVERED → (CALL-04) ACKNOWLEDGED. 발송 뒤 언제든 CALL-03 철회 → REVOKED (새 호출은 prevCallId로 연결)

상태값 이름은 3장 상태값 문단과 같습니다. 화살표 글자는 그 상태를 바꾸는 API·신호입니다.

### 9.7 시퀀스 다이어그램

실선은 요청, 점선은 응답이나 서버가 먼저 보내는 것입니다. 번호 순서대로 읽으면 됩니다.

&#91;embedded content: 시퀀스 1 · 차량 QR 연결·가입 (LINK-01\~04)\]

&#91;embedded content: 시퀀스 2 · 가족 초대·수락 (FAM-02 · FAM-04\~06)\]

&#91;embedded content: 시퀀스 3 · 픽업 시작 → 합류 호출 (SES-02 · SIG-01 · CALL-04)\]

&#91;embedded content: 시퀀스 4 · 정차 → 탑승 완료 (SIG-02 · SES-11\~13)\]

## 부록 A. 시트 API 명세서와 대조

화면에서 직접 설계한 뒤 시트의 API-01\~25와 맞춰 봤습니다. 시트 API 25개 중 15개는 1:1로 대응되고, 7개는 화면 동작에 맞게 여러 개로 나눴고, 3개는 없앴습니다. 직접 설계한 57개 중 23개는 시트에 없던 API입니다.

| 시트 API | 직접 설계한 API | 바뀐 점 |
| --- | --- | --- |
| API-01 세션 시작 | SES-02 | 같음. 기본 장소가 세션 장소로 들어감 |
| API-02 후보 장소 조회 | SPOT-01 | 후보 조회 → 보호자가 저장한 장소 조회 |
| API-03 장소 승인 저장 | SPOT-02, SPOT-03 | 승인 → 지도에서 직접 추가·수정·삭제 |
| API-04 최적 장소 추천 | 없앰 | 보호자 지정 방식으로 추천 엔진 제거 |
| API-05 경로 적용 확정 | SES-04 | 같음 |
| API-06 ETA 관측값 | SIG-01 | ETA에 위치·거리·진행 방향을 합쳐 한 번에 전송 |
| API-07 시간 예측 조회 | 없앰 | SES-03의 eta 필드와 SES-08 이벤트로 통합 |
| API-08 자녀 이벤트 | SIG-04 | 위치 전송만 대응. 준비 완료는 기획에 없음 |
| API-09 차량 이벤트 | SIG-02 | 같음 |
| API-10 합류 호출 생성 | CALL-02 | 보호자 확인형 수동 발송만. 자동 발송은 서버 내부 |
| API-11 호출 열람 확인 | CALL-04 | 같음 |
| API-12 도움 요청 | SIG-05 | 요청 종류(type) 추가 |
| API-13 보류·취소·철회 | CALL-01, CALL-03, SES-06, SES-07, SES-12 | action 하나로 묶인 것을 권한·조건이 다른 5개로 분리 |
| API-14 탑승 완료 | SES-11 | IMMEDIATE / MANUAL 구분 |
| API-15 세션 조회 | SES-03 (+ SES-01) | 주체별 화면용 뷰, 진행 중 세션 찾기 추가 |
| API-16 결과 조회 | OUT-02 | 같음 |
| API-17 기록 정정 | OUT-03 | PATCH → PUT 정정(원본 컬럼 유지) |
| API-18 가족 연결 | LINK-04, FAM-06 | 차량 연결과 자녀 초대 수락 분리. 둘 다 토큰 발급(로그인 대신) |
| API-19 자녀·인계 정책 저장 | SET-06, FAM-02, FAM-03 | 보호자 가족 프로필 화면과 합류 안내 방식 화면별로 분리 |
| API-20 학원·일정 저장 | SET-02, SET-03 | 생성 POST와 수정 PUT 분리 |
| API-21 설정 조회 | SET-07, SET-10, FAM-01 | 보호자 설정 완료 화면, 차량 설정 요약 화면, 보호자 가족 프로필 화면별 조회로 분리 |
| API-22 대체 장소 확정 | SES-05 | 저장된 다른 장소로 변경 |
| API-23 이벤트 재동기화 | SES-09 | 같음 |
| API-24 호출 가드 조회 | 없앰 | SES-03의 call.guard로 통합 |
| API-25 연결 코드 확인 | LINK-03 | 같음 |

**시트에 없던 23개**: AUTH-01\~04(토큰 갱신·차량 기기·푸시 토큰·로그아웃), LINK-01·02·05(QR 발급·연결 상태 조회·차량 연결 해제), FAM-04·05·07(멤버 초대·초대 확인·멤버 해제), SET-01·04·08·09(학원 검색·일정 목록·설정 완료·차량 오늘의 픽업 화면 주간 일정), SIG-03(준비 시간 추가), SES-08·10·13(실시간 스트림·연락처·탑승 예외), NOTI-01\~03(수신 확인·발송 이력·재발송), OUT-01·04(이력·데이터 설정).
