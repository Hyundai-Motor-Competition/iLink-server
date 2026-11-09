# 08. 픽업 (pickup)

픽업 한 건의 시작~완료: 경로 확정, 합류 호출, 정차 · 탑승, 예외, 결과. 세션 시작 때 schedule · place 값을 스냅샷으로 복사해, 픽업 도중 설정이 바뀌어도 진행 중인 픽업은 영향받지 않는다.

| 항목 | 내용 |
|---|---|
| 범위 | 엔티티 PickupSession · SessionChild · Call · SessionEvent · PickupOutcome. API SES-01~13(세션), SIG-01~05(차량 · 자녀 신호), CALL-01~04(합류 호출), OUT-01~04(결과 · 기록) — 엔드포인트 28개. 세션 뷰, 실시간 이벤트 19종 · SSE, 합류 호출 판정(트리거 · 가드 G1~G5), 10초 자동 완료 스케줄러. 다른 모듈용 in 포트(진행 중 픽업 확인, 호출 진행 기록) |
| 의존 | 03_공유모듈-shared, 04_사용자-user(가족 · 자녀 · 차량 · 합류 안내 방식 · 전화번호), 05_인증-auth(자녀 기기 · 위치 동의), 06_일정-schedule(일정 · 학원 대표번호), 07_장소-place(기본 장소 · 저장 장소). notification의 in 포트(Notification 행 기록, 같은 트랜잭션)는 out 포트 + 테스트 대역 |
| 진행 | 계층 병렬 |

## 구조

```text
pickup
├─ adapter
│  ├─ in
│  │  ├─ web          PickupSessionController(SES-01~07 · 09~13), SignalController(SIG-01~05), CallController(CALL-01~04), OutcomeController(OUT-01~04), doc 인터페이스(컨트롤러마다 1개), 요청 · 응답 DTO(record)
│  │  ├─ sse          SessionStreamController (SES-08, 차량 전용)
│  │  └─ scheduler    10초 자동 완료 작업
│  └─ out
│     ├─ persistence  JPA 엔티티 5개, Spring Data 저장소, 도메인 ↔ 엔티티 변환
│     └─ event        도메인 이벤트 발행(ApplicationEventPublisher), SSE 송출 리스너(AFTER_COMMIT)
├─ application
│  ├─ port/in         StartPickupUseCase, ConfirmRouteUseCase, ReceiveTelemetryUseCase … (아래 「시그니처」)
│  ├─ port/out        LoadSessionPort, SaveSessionPort, PublishSessionEventPort …
│  └─ service         StartPickupService … (in 포트 구현, 트랜잭션 경계)
└─ domain             PickupSession, SessionChild, Call, CallTrigger, Guard, SessionView 조립 규칙
```

## 테이블

픽업 세션 (PickupSession)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| sessionId | BIGINT | PK | |
| vehicleId | BIGINT | FK → Vehicle | |
| familyId | BIGINT | FK → Family | 세션 시작 때 차량 토큰의 familyId 스냅샷. 가족 기준 조회 · 알림 대상 찾기 [사람 결정 2026-11-09] |
| spotId | BIGINT | FK → PickupSpot | |
| spotName | VARCHAR(50) | | 세션 시작 · SES-05 때 PickupSpot에서 복사(스냅샷) |
| stopPoint | geography(Point,4326) | | 정차 지점 스냅샷 |
| meetingPoint | geography(Point,4326) | | 만남 지점 스냅샷 |
| date | DATE | INDEX(vehicleId, date) | 픽업 날짜(한국) |
| sessionStatus | VARCHAR(20) | | ACTIVE · ON_HOLD · COMPLETED · CANCELLED |
| vehiclePhase | VARCHAR(20) | | ROUTING · APPROACHING · STOPPED · DEPARTED |
| spotStatus | VARCHAR(20) | | SELECTED · ROUTING · CONFIRMED · INVALIDATED |
| stateVersion | INT | 기본 0 | 바뀔 때마다 +1 |
| spotVersion | INT | 기본 0 | |
| routeVersion | INT | 기본 0 | |
| routeSpotVersion | INT | NULL | SES-04 경로 확정 때의 spotVersion. G3 「경로 일치」 판정용 [팀장 결정 2026-11-09] |
| callHeld | BOOLEAN | 기본 false | CALL-01 발송 전 보류. DB에 두어 재시작해도 유지 |
| etaAt | TIMESTAMPTZ | NULL | 내비 ETA, 최신값만 |
| remainingMeters | INT | NULL | |
| etaUpdatedAt | TIMESTAMPTZ | NULL | 60초 넘게 갱신 없으면 stale |
| boardingDetectedAt | TIMESTAMPTZ | NULL | SIG-02 DOOR_CYCLE로 탑승 후보가 된 시각 |
| boardingCancelableUntil | TIMESTAMPTZ | NULL | 10초 취소 창 마감. 자동 완료 스케줄러가 이 시각이 지난 세션을 처리 |
| boardingDoor | VARCHAR(20) | NULL | 인식한 문 |
| incidentType | VARCHAR(30) | NULL | 예: CHILD_NOT_ON_BOARD |
| incidentCause | VARCHAR(100) | NULL | |
| incidentOpenedAt | TIMESTAMPTZ | NULL | |
| incidentResolvedAt | TIMESTAMPTZ | NULL | |

세션 자녀 (SessionChild)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| sessionChildId | BIGINT | PK | |
| sessionId | BIGINT | FK → PickupSession, UNIQUE(sessionId, childUserId) | |
| childUserId | BIGINT | FK → User | |
| scheduleId | BIGINT | FK → PickupSchedule | |
| childPhase | VARCHAR(20) | | WAITING · CALLED · MEETING · NOT_BOARDED |
| extraPrepMinutes | SMALLINT | 기본 0 | 0~15 |
| callNotBefore | TIMESTAMPTZ | NULL | SIG-03 때 지금 + 누적 추가 분. 이 시각 전에는 호출 안 함(G5) |
| lastLat | NUMERIC(9,6) | NULL | SIG-04 최신 위치. 이력 없이 덮어씀 |
| lastLng | NUMERIC(9,6) | NULL | |
| lastLocatedAt | TIMESTAMPTZ | NULL | 탑승 완료 · 철회 · 취소 때 세 컬럼 모두 NULL |
| acknowledgedAt | TIMESTAMPTZ | NULL | 「확인했어요」 |
| boardedAt | TIMESTAMPTZ | NULL | 탑승 |

세션 이벤트 (SessionEvent)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| eventId | BIGINT | PK | |
| sessionId | BIGINT | FK → PickupSession, UNIQUE(sessionId, seq) | |
| seq | BIGINT | | 세션 안 순번(SES-09 afterSeq) |
| type | VARCHAR(40) | | 예: CALL_SENT |
| stateVersion | INT | | 이벤트 시점의 세션 버전 |
| occurredAt | TIMESTAMPTZ | | |
| payload | JSONB | NULL | |

합류 호출 (Call) — 테이블 이름 pickup_call(CALL은 SQL 예약어, users와 같은 관례)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| callId | BIGINT | PK | |
| sessionId | BIGINT | FK → PickupSession | |
| prevCallId | BIGINT | FK → Call, NULL | 재호출이면 이전 호출 |
| triggerType | VARCHAR(20) | | ETA_3MIN · DIST_700M · MANUAL |
| status | VARCHAR(20) | | ACCEPTED · SENT · DELIVERED · ACKNOWLEDGED · REVOKED |
| spotVersion | INT | | 호출 시점의 장소 버전 |
| sentAt | TIMESTAMPTZ | NULL | |
| deliveredAt | TIMESTAMPTZ | NULL | NOTI-01 수신. 세션 뷰 call.progress [팀장 결정 2026-11-09] |
| acknowledgedAt | TIMESTAMPTZ | NULL | 형제 모두 확인한 시각. 세션 뷰 call.progress [팀장 결정 2026-11-09]. acceptedAt은 created_at |
| revokedAt | TIMESTAMPTZ | NULL | |
| revokeReason | VARCHAR(100) | NULL | |

픽업 결과 (PickupOutcome)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| sessionId | BIGINT | PK, FK → PickupSession | 세션과 1:1 |
| completedAt | TIMESTAMPTZ | | |
| boardingMethod | VARCHAR(20) | | DOOR · IMMEDIATE · MANUAL |
| predictedStopAt | TIMESTAMPTZ | NULL | |
| actualStopAt | TIMESTAMPTZ | NULL | 원본, 바꾸지 않음 |
| correctedStopAt | TIMESTAMPTZ | NULL | 정정값 |
| correctionReason | VARCHAR(30) | NULL | |
| correctionStatus | VARCHAR(20) | NULL | |
| correctedBy | BIGINT | FK → User, NULL | |
| correctionNote | VARCHAR(200) | NULL | OUT-03 note [사람 결정 2026-11-09] |

보호자 데이터 설정 (GuardianDataSetting) [사람 결정 2026-11-09]

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| userId | BIGINT | PK, FK → User | GUARDIAN. 행이 없으면 learningConsent false로 본다 |
| learningConsent | BOOLEAN | 기본 false | OUT-04 |

상태값 (누가 바꾸는지에 따라 5개 컬럼으로 나눔)

| 컬럼 | 값 | 뜻 | 바뀌는 계기 |
|---|---|---|---|
| sessionStatus | ACTIVE | 픽업 진행 중 | SES-02 |
| | ON_HOLD | 멈춤 | SES-06 보류, SES-13 예외 |
| | COMPLETED | 완료 | 세션 자녀가 모두 탑승 또는 미탑승 |
| | CANCELLED | 취소 | SES-07 |
| vehiclePhase | ROUTING | 장소로 이동 중 | 세션 시작 |
| | APPROACHING | 가까워짐 | SIG-01 ETA · 거리 기준 |
| | STOPPED | 정차 | SIG-02 STOPPED |
| | DEPARTED | 출발 | SIG-02 DEPARTED |
| spotStatus | SELECTED | 장소 선택됨 | 기본 장소 적용, SES-05 |
| | ROUTING | 경로 계산 중 | 내비 경로 요청 |
| | CONFIRMED | 경로 확정, 자녀에게 공개 | SES-04 |
| | INVALIDATED | 쓸 수 없게 됨 → 다시 선택 | 장소 사용 불가 |
| childPhase | WAITING | 실내 대기 | 세션 시작, CALL-03 철회 |
| | CALLED | 합류 호출 받음 | 호출 발송 |
| | MEETING | 차로 이동 중 | CALL-04 확인 |
| | NOT_BOARDED | 오늘 타지 않음(이유 없음) | SES-11 notBoardedChildUserIds |
| Call.status | ACCEPTED | 생성, 발송 전 | 호출 조건 통과 |
| | SENT | FCM 발송함 | 발송 |
| | DELIVERED | 폰에 도착 | NOTI-01 |
| | ACKNOWLEDGED | 아이가 확인 | CALL-04(형제 모두) |
| | REVOKED | 철회 | CALL-03, SES-07 자동 철회 |

## API 명세

`…`를 쓰지 않고 전체 경로로 적는다. 「SessionView」는 아래 세션 뷰 응답이다.

| ID | Method · 경로 | 호출 | Idempotency-Key | 요청 → 응답 | 오류 |
|---|---|---|---|---|---|
| SES-01 | GET /v1/pickup-sessions/active | 모든 앱 | 불필요 | — → 200 sessionId · status(ACTIVE · ON_HOLD) · stateVersion, 없으면 204 | 공통 |
| SES-02 | POST /v1/pickup-sessions | 차량 앱 | 필요 | scheduleIds[](1개 이상), date(한국 날짜) → 201 새 세션 · 200 진행 중인 같은 세션, SessionView | 422 NOT_TODAY, 422 SCHEDULES_NOT_GROUPABLE, 422 SETUP_INCOMPLETE(missing 함께), 409 ANOTHER_SESSION_ACTIVE |
| SES-03 | GET /v1/pickup-sessions/{sessionId} | 그 세션의 차량, 가족 사용자 | 불필요 | — → 200 SessionView(주체별 필드) | 공통 |
| SES-04 | PUT /v1/pickup-sessions/{sessionId}/route | 차량 앱 | 필요 | spotId, navRouteId, stateVersion → 200 SessionView | 422 SPOT_NOT_SAVED |
| SES-05 | POST /v1/pickup-sessions/{sessionId}/spot-changes | 차량 앱, GUARDIAN · CO_GUARDIAN | 필요 | spotId, stateVersion, spotVersion → 200 SessionView | 409 CALL_ALREADY_SENT, 409 CHILD_ALREADY_MOVING |
| SES-06 | PUT /v1/pickup-sessions/{sessionId}/hold | 차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE | 필요 | stateVersion, reason(0~100자) → 200 SessionView | 공통 |
| SES-06 | DELETE /v1/pickup-sessions/{sessionId}/hold | 차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE | 필요 | query stateVersion → 200 SessionView | 공통 |
| SES-07 | POST /v1/pickup-sessions/{sessionId}/cancellation | 차량 앱, GUARDIAN · CO_GUARDIAN | 필요 | stateVersion, reason(0~100자) → 200 SessionView | 409 SESSION_ALREADY_FINISHED |
| SES-08 | GET /v1/pickup-sessions/{sessionId}/stream | 차량 앱 | 불필요 | header Last-Event-ID(선택) → 200 text/event-stream | 공통 |
| SES-09 | GET /v1/pickup-sessions/{sessionId}/events | 그 세션의 차량, 가족 사용자 | 불필요 | query afterSeq(필수), limit(기본 100, 최대 100) → 200 items[](eventId · seq · type · stateVersion · occurredAt · payload), hasMore | 공통 |
| SES-10 | GET /v1/pickup-sessions/{sessionId}/contacts | 그 세션의 차량, 가족 사용자 | 불필요 | — → 200 items[](name · role(GUARDIAN · CO_GUARDIAN · DELEGATE · TEACHER · ACADEMY) · phone) | 공통 |
| SES-11 | POST /v1/pickup-sessions/{sessionId}/completion | 차량 앱 | 필요 | mode(IMMEDIATE · MANUAL), stateVersion, notBoardedChildUserIds[](선택) → 200 SessionView | 422 VEHICLE_NOT_STOPPED, 422 CHILDREN_NOT_ALL_BOARDED |
| SES-12 | POST /v1/pickup-sessions/{sessionId}/boarding-rejection | 차량 앱 | 필요 | childUserId(자녀가 여러 명이면 필수), stateVersion → 200 SessionView | 409 REJECTION_WINDOW_CLOSED |
| SES-13 | PUT /v1/pickup-sessions/{sessionId}/incident | 차량 앱 | 필요 | type(CHILD_NOT_ON_BOARD), childUserId, stateVersion → 200 SessionView | 409 INCIDENT_ALREADY_OPEN |
| SIG-01 | POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry | 차량 앱(백그라운드, 15초마다) | 불필요 | observedAt, navEtaSeconds(0 이상), remainingMeters(0 이상), location{lat, lng}, headingDeg(0~359, 선택), speedKmh(0 이상, 선택), gear(P · R · N · D, 선택) → 202 | 공통 |
| SIG-02 | POST /v1/pickup-sessions/{sessionId}/vehicle-events | 차량 앱(백그라운드, 즉시) | 불필요(eventId가 대신함) | eventId(UUID), type(STOPPED · DEPARTED · STOP_FAILED · DOOR_CYCLE), occurredAt, location, door(DOOR_CYCLE이면 필수: FRONT_RIGHT · REAR_LEFT · REAR_RIGHT) → 202 | 공통 |
| SIG-03 | POST /v1/pickup-sessions/{sessionId}/prep-time-extensions | 자녀 앱 | 필요 | minutes(5 · 10 · 15) → 200 extraPrepMinutes(누적), callNotBefore | 422 PREP_EXTENSION_LIMIT, 409 CALL_ALREADY_SENT |
| SIG-04 | POST /v1/pickup-sessions/{sessionId}/child-locations | 자녀 앱 | 불필요 | at, lat, lng, accuracyM(선택) → 202 | 403 LOCATION_CONSENT_REQUIRED, 409 SESSION_ALREADY_FINISHED, 409 CHILD_NOT_CALLED |
| SIG-05 | POST /v1/pickup-sessions/{sessionId}/help-requests | 자녀 앱 | 필요 | type(NEED_HELP · CANT_FIND_CAR · CONTACT_PARENT) → 201 nextAllowedAt | 429 HELP_REQUEST_TOO_SOON(nextAllowedAt 함께) |
| CALL-01 | PUT /v1/pickup-sessions/{sessionId}/call-hold | 차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE | 필요 | stateVersion → 200 SessionView | 409 CALL_ALREADY_SENT |
| CALL-01 | DELETE /v1/pickup-sessions/{sessionId}/call-hold | 차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE | 필요 | query stateVersion → 200 SessionView | 공통 |
| CALL-02 | POST /v1/pickup-sessions/{sessionId}/calls | 차량 앱 | 필요 | stateVersion → 201 callId, status | 422 CALL_GUARD_UNMET(unmet 함께), 409 CALL_ALREADY_SENT, 409 NOT_GUARDIAN_CONFIRM_MODE |
| CALL-03 | POST /v1/calls/{callId}/revocation | 차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE | 필요 | stateVersion, reason(0~100자) → 200 SessionView | 409 CALL_ALREADY_REVOKED |
| CALL-04 | POST /v1/calls/{callId}/acknowledgement | 자녀 앱 | 필요 | — → 200 callId, status, acknowledgedAt | 409 CALL_ALREADY_REVOKED |
| OUT-01 | GET /v1/children/{childId}/pickup-history | GUARDIAN · CO_GUARDIAN | 불필요 | query month(YYYY-MM, 한국) → 200 items[](date · sessionId · status(COMPLETED · CANCELLED) · completedAt) | 공통 |
| OUT-02 | GET /v1/pickup-sessions/{sessionId}/outcome | GUARDIAN · CO_GUARDIAN | 불필요 | — → 200 sessionId, completedAt, spotName, boardingMethod, predictedStopAt, actualStopAt, diffSeconds, correction{status, correctedStopAt} | 공통 |
| OUT-03 | PUT /v1/pickup-sessions/{sessionId}/outcome/correction | GUARDIAN · CO_GUARDIAN | 불필요 | correctedStopAt(세션 날짜 안), reasonCode(WRONG_STOP_TIME · OTHER), note(0~200자) → 200 OUT-02와 같은 모양 | 공통 |
| OUT-04 | PUT /v1/guardians/me/data-settings | GUARDIAN | 불필요 | learningConsent → 200 learningConsent, retentionDays(90) | 공통 |

### 세션 뷰 (SessionView)

SES-02~07, SES-11~13, CALL-01 · 03이 성공하면 돌려주고 SES-03이 조회한다. 409 STATE_VERSION_CONFLICT에도 실린다.

| 필드 | 타입 | 설명 |
|---|---|---|
| sessionId | integer | |
| stateVersion | integer | 다음 요청에 그대로 보냄 |
| status.session · vehicle · spot | string | sessionStatus · vehiclePhase · spotStatus |
| status.call | string | NONE · ACCEPTED · SENT · DELIVERED · ACKNOWLEDGED · REVOKED |
| spot.spotId · name · spotVersion | integer · string · integer | 장소 확정 전 자녀에게는 null |
| spot.locked | boolean | 자녀가 이동 중(MEETING)이라 장소를 바꿀 수 없으면 true |
| eta.stopEtaAt · remainingSeconds · updatedAt | datetime · integer · datetime | 도착 예정 시각. 자녀에게도 항상 보임 |
| eta.stale | boolean | 60초 넘게 갱신이 없으면 true(「시간 확인 중」) |
| call.mode | string | AUTO · GUARDIAN_CONFIRM |
| call.callId · held · scheduledAt | integer · boolean · datetime | 없으면 callId null |
| call.guard.met · unmet[] | boolean · string[] | G1~G5 통과 여부와 통과하지 못한 가드 |
| call.progress.acceptedAt · sentAt · deliveredAt · acknowledgedAt | datetime | 호출 단계별 시각 |
| children[].childUserId · name · phase · extraPrepMinutes · callNotBefore · acknowledgedAt · boardedAt | | 자녀별 상태 |
| children[].location.lat · lng · at | number · number · datetime | 호출 전 · 탑승 후 · 동의 없음이면 null. 차량 · 보호자에게만 |
| route.routeVersion · remainingMeters | integer | |
| boarding.detectedAt · cancelableUntil · door | | 없으면 boarding = null |
| incident.type · childUserId · openedAt | | 없으면 incident = null |

```json
{
  "sessionId": 1024, "stateVersion": 15,
  "status": { "session": "ACTIVE", "vehicle": "APPROACHING", "call": "NONE", "spot": "CONFIRMED" },
  "spot": { "spotId": 21, "name": "해솔영어학원 정문 앞", "spotVersion": 4, "locked": false },
  "eta": { "stopEtaAt": "2026-11-05T18:12:00+09:00", "remainingSeconds": 720, "updatedAt": "2026-11-05T18:00:05+09:00", "stale": false },
  "call": { "mode": "AUTO", "callId": null, "held": false, "scheduledAt": "2026-11-05T18:09:00+09:00", "guard": { "met": true, "unmet": [] }, "progress": { "acceptedAt": null, "sentAt": null, "deliveredAt": null, "acknowledgedAt": null } },
  "children": [
    { "childUserId": 31, "name": "민준", "phase": "WAITING", "extraPrepMinutes": 10, "acknowledgedAt": null, "boardedAt": null },
    { "childUserId": 32, "name": "서윤", "phase": "WAITING", "extraPrepMinutes": 0, "acknowledgedAt": null, "boardedAt": null }
  ],
  "route": { "routeVersion": 2, "remainingMeters": 4200 }, "boarding": null, "incident": null
}
```

### SSE 이벤트 한 건 (SES-08)

```text
event: CALL_SENT
id: 42
data: {"eventId":9001,"seq":42,"stateVersion":16,"occurredAt":"2026-11-05T18:09:02+09:00","payload":{"callId":3301,"trigger":"ETA_3MIN"}}
```

## 핵심 로직

### 상태 전이 (소스 9.6)
- sessionStatus: (시작) → ACTIVE(SES-02). ACTIVE ↔ ON_HOLD(SES-06 보류 · 해제). ACTIVE → COMPLETED(SES-11, 10초 타이머). ACTIVE · ON_HOLD → CANCELLED(SES-07). COMPLETED · CANCELLED는 끝.
- childPhase(자녀마다 따로): WAITING → CALLED(호출 발송) → MEETING(CALL-04) → 탑승(boardedAt). CALLED → WAITING(CALL-03 철회 → 안전 대기 후 다시 대기). SIG-03은 상태를 바꾸지 않고 extraPrepMinutes만 늘린다.
- vehiclePhase: ROUTING → APPROACHING → STOPPED(SIG-02 STOPPED) → DEPARTED(SIG-02 DEPARTED). 한 방향.
- spotStatus: SELECTED → ROUTING → CONFIRMED(SES-04) → INVALIDATED(사용 불가). SES-05는 SELECTED로 되돌린다.
- Call.status: ACCEPTED → SENT(FCM 발송) → DELIVERED(NOTI-01) → ACKNOWLEDGED(CALL-04). 발송 뒤 언제든 CALL-03 → REVOKED. 새 호출은 prevCallId로 잇는다.

### 버전 · 순번 · 동시 처리
- 세션을 바꾸는 요청은 stateVersion을 보낸다. 서버 값과 다르면 409 STATE_VERSION_CONFLICT와 최신 세션 뷰.
- 바뀔 때마다 stateVersion +1. 장소가 바뀌면 spotVersion +1, 경로가 확정되면 routeVersion +1.
- DB에 남기는 이벤트는 SessionEvent에 세션 안 순번 seq(1부터 1씩)와 그때의 stateVersion을 함께 저장한다.
- 같은 세션을 바꾸는 요청은 세션 행 잠금 안에서 처리한다(SIG-05 연타 확인은 소스에 명시).

### 세션 시작 (SES-02)
- date가 한국 날짜로 오늘이 아니면 422 NOT_TODAY.
- 같은 날 · 같은 학원 · 같은 시간대 일정만 한 세션으로 묶는다. 아니면 422 SCHEDULES_NOT_GROUPABLE.
- 기본 장소 또는 자녀 기기가 없으면 422 SETUP_INCOMPLETE(missing 함께). DRAFT 일정(출입구 없음)도 시작할 수 없다.
- 이 차량에 다른 진행 중 세션이 있으면 409 ANOTHER_SESSION_ACTIVE. 진행 중인 같은 세션이면 200으로 그 세션을 돌려준다.
- 새 세션: ACTIVE · ROUTING · SELECTED, 기본 장소의 spotId · spotName · stopPoint · meetingPoint를 스냅샷으로 복사, 자녀마다 SessionChild(WAITING). 이벤트 SESSION_STARTED.

### 조회 (SES-01 · 03 · 09 · 10)
- SES-01: 차량은 그 차량의 세션, 사용자는 자기 가족 세션, 자녀는 자기가 포함된 세션. 없으면 204.
- SES-03: 주체마다 보이는 필드가 다르다. 자녀에게는 자기 children 항목만, 장소는 SES-04 이후에만, eta.stopEtaAt은 항상. children[].location은 차량 · 보호자에게만.
- status.call은 가장 최근 Call의 status, 없으면 NONE. eta.stale은 etaUpdatedAt이 60초를 넘게 지났는지. spot.locked는 MEETING 자녀가 있는지.
- SES-09: afterSeq보다 큰 seq를 오름차순으로 limit개. 더 있으면 hasMore = true. DB에 저장하지 않는 ETA_UPDATED · VEHICLE_LOCATION은 나오지 않는다.
- SES-10: 가족 멤버의 User.phone과 학원 대표번호(Academy.phone, role ACADEMY). 다이얼패드 없음.

### 경로 확정 · 장소 변경 (SES-04 · 05)
- SES-04: 가족이 저장한 장소만(아니면 422 SPOT_NOT_SAVED). 내비 SDK 성공 뒤에만 부른다. spotStatus CONFIRMED, routeVersion +1, 이때부터 자녀 화면에 장소가 보인다. 이벤트 SPOT_CONFIRMED.
- 처음 확정이면 자녀에게 출발 알림(도착 예정 시각 포함, Notification type DEPARTURE)을 보낸다. 세션당 1번이고 이벤트 DEPARTURE_NOTICE_SENT로 남긴다.
- SES-05: 저장된 다른 장소로 바꾼다. 합류 안내를 이미 보냈으면 409 CALL_ALREADY_SENT(CALL-03으로 먼저 철회), 자녀가 이동 중이면 409 CHILD_ALREADY_MOVING. spotStatus SELECTED, spotVersion +1, 스냅샷 갱신, 이벤트 SPOT_CHANGED. 차량은 다시 길안내한 뒤 SES-04를 부른다.

### 보류 · 취소 (SES-06 · 07)
- SES-06 PUT은 ON_HOLD, DELETE는 ACTIVE로 되돌린다.
- SES-07: 이미 완료 · 취소면 409 SESSION_ALREADY_FINISHED. 보낸 합류 안내는 자동 철회(CALL_REVOKED)하고 자녀 위치를 비운다. 이벤트 SESSION_CANCELLED.

### 차량 신호 (SIG-01 · 02)
- SIG-01: observedAt이 마지막 값보다 오래되면 무시한다. 원본은 저장하지 않고 etaAt(= observedAt + navEtaSeconds) · remainingMeters · etaUpdatedAt만 덮어쓴다. ETA_UPDATED는 저장하지 않고 차량 SSE로만 보낸다. 그다음 「합류 호출 판정」을 돌린다.
- SIG-02: eventId가 같으면 무시한다. 원본은 저장하지 않고 결과만 반영한다.
  - STOPPED → vehiclePhase STOPPED, 이벤트 VEHICLE_STOPPED(occurredAt이 실제 정차 시각).
  - DEPARTED → vehiclePhase DEPARTED, 이벤트 VEHICLE_DEPARTED.
  - DOOR_CYCLE → 「탑승 · 완료」 참고.

### 합류 호출 판정 (소스 9.5 플로우차트 1)
SIG-01이 들어올 때마다(15초) 이 순서로 확인하고 하나라도 걸리면 이번에는 보내지 않는다. 따로 도는 타이머는 없어 callNotBefore가 지난 뒤 최대 15초 늦게 호출될 수 있다(허용).

1. 세션이 ACTIVE이고 callHeld가 false인가.
2. 철회되지 않은 호출이 아직 없는가. 있으면 중복 발송하지 않는다.
3. CallTrigger: navEtaSeconds ≤ 180이면 ETA_3MIN, remainingMeters ≤ 700이면 DIST_700M.
4. Guard G1~G5를 모두 통과하는가. 아니면 보내지 않고 사유를 세션 뷰 call.guard.unmet에 보인다.

| 가드 | 통과 조건 |
|---|---|
| G1 세션 유효 | sessionStatus = ACTIVE |
| G2 장소 확정 | spotStatus = CONFIRMED |
| G3 경로 일치 | 확정된 경로가 지금 장소 버전의 것(현재 가정, 확인 필요) |
| G4 ETA 유효 | etaUpdatedAt이 60초 안 |
| G5 준비 시간 지남 | 모든 세션 자녀의 callNotBefore가 비었거나 지났음 |

5. 가족의 합류 안내 방식이 AUTO면 6으로. GUARDIAN_CONFIRM이면 차량 정차 · 호출 화면에 「합류 안내 보내기」를 띄우고 CALL-02를 기다린다.
6. 한 트랜잭션으로 Call(ACCEPTED, triggerType, 지금 spotVersion) · Notification 행(type CALL, 세션 자녀 기기) · 이벤트 CALL_TRIGGERED를 저장한다. 철회된 호출 뒤면 prevCallId를 단다.
7. 커밋 뒤 notification이 FCM으로 자녀 폰에 보내고, pickup in 포트로 발송을 알리면 Call SENT · sentAt, 세션 자녀 CALLED, 이벤트 CALL_SENT.

### 호출 API (CALL-01~04)
- CALL-01 PUT: 발송 전에만 callHeld = true. 이미 보냈으면 409 CALL_ALREADY_SENT(CALL-03 사용). DELETE: callHeld = false로 바꾸고 저장된 최신 ETA로 바로 판정을 다시 돌린다.
- CALL-02(보호자 확인형 「합류 안내 보내기」): 가족이 AUTO면 409 NOT_GUARDIAN_CONFIRM_MODE, 이미 보냈으면 409 CALL_ALREADY_SENT, 가드 미충족이면 422 CALL_GUARD_UNMET(unmet 함께). 통과하면 triggerType MANUAL로 판정 6~7단계와 같게 처리한다.
- CALL-03: 이미 철회면 409 CALL_ALREADY_REVOKED. Call REVOKED · revokedAt · revokeReason, 세션 자녀는 WAITING으로 돌아가고 위치를 비운다. 이벤트 CALL_REVOKED. 자녀는 안전 대기 화면으로 가고, 조건이 다시 맞으면 prevCallId를 단 새 호출이 나간다.
- CALL-04: 아이별로 SessionChild.acknowledgedAt에 기록하고 여러 번 눌러도 첫 시각을 유지한다. 그 자녀는 MEETING. 형제가 모두 확인하면 Call ACKNOWLEDGED, 이벤트 CALL_ACKNOWLEDGED. 철회된 호출이면 409 CALL_ALREADY_REVOKED.
- NOTI-01 수신 확인이 오면 notification이 pickup in 포트로 알리고 Call DELIVERED, 이벤트 CALL_DELIVERED.

### 자녀 신호 (SIG-03 · 04 · 05)
- SIG-03: 호출 전 실내 대기에서만. 합류 안내를 이미 받았으면 409 CALL_ALREADY_SENT. 누적이 15분을 넘으면 422 PREP_EXTENSION_LIMIT. extraPrepMinutes에 더하고 callNotBefore = 지금 + 누적 분(예: 18:09에 +5분 → 18:14). 이벤트 PREP_TIME_EXTENDED(차량 SSE · 보호자, Notification type PREP_EXTENDED). childPhase는 그대로.
- SIG-04: 위치 공유 동의가 없으면 403 LOCATION_CONSENT_REQUIRED. 합류 호출을 받은 자녀(CALLED · MEETING)만 받는다. at이 저장된 값보다 오래되면 무시하고 lastLat · lastLng · lastLocatedAt만 덮어쓴다. 탑승 완료 · 호출 철회 · 픽업 취소 때 세 컬럼을 비우고, 그 뒤에 온 요청은 409(세션이 끝났으면 SESSION_ALREADY_FINISHED).
- SIG-05: 같은 아이의 마지막 HELP_REQUESTED 뒤 60초 안이면 429 HELP_REQUEST_TOO_SOON + nextAllowedAt(마지막 시각 + 60초). 1분 제한은 아이별. 세션 행 잠금 안에서 확인 후 이벤트 HELP_REQUESTED(payload childUserId · type)로 기록하고, 보호자에게 FCM(Notification type HELP) · 차량에 SSE. 응답 nextAllowedAt = 지금 + 60초. 문구: NEED_HELP 「도움이 필요해요」, CANT_FIND_CAR 「차를 못 찾고 있어요」, CONTACT_PARENT 「연락해 달라고 해요」.

### 탑승 · 완료 (소스 9.5 플로우차트 2)
1. SIG-02 DOOR_CYCLE: 정차 중(STOPPED) · 운전석 제외 · 합류 안내 발송 뒤일 때만 탑승 후보. 아니면 무시(상태 변화 없음).
2. 후보면 boardingDetectedAt · boardingCancelableUntil(= +10초) · boardingDoor를 채우고 세션 자녀 모두 boardedAt을 기록한다. 이벤트 BOARDING_DETECTED. 문 1회 = 전원 탑승(아이별 탑승 확인은 소스 8장 추후).
3. 창 안에 SES-12(「아이가 타지 않았어요」)가 오면 그 아이의 boardedAt만 비운다. 창이 지났으면 409 REJECTION_WINDOW_CLOSED(SES-13 사용).
4. 자동 완료 스케줄러: boardingCancelableUntil이 지난 ACTIVE 세션 중 세션 자녀가 모두 탄 세션만 완료한다(boardingMethod DOOR). 재시작해도 DB 시각 기준이라 사라지지 않는다.
5. SES-11: IMMEDIATE는 도어 인식 차량에서 10초를 기다리지 않고 바로 완료, MANUAL은 도어 신호 없는 차량 · 정차 중만(아니면 422 VEHICLE_NOT_STOPPED). MANUAL은 미탑승 지정 외 세션 자녀를 탑승 처리한다. notBoardedChildUserIds의 자녀는 NOT_BOARDED. 탑승하지도 미탑승 지정되지도 않은 자녀가 있으면 422 CHILDREN_NOT_ALL_BOARDED.
6. 완료: sessionStatus COMPLETED, PickupOutcome(completedAt · boardingMethod · predictedStopAt · actualStopAt) 기록, 자녀 위치 비움, 이벤트 SESSION_COMPLETED.
7. SES-13: 이미 열린 예외가 있으면 409 INCIDENT_ALREADY_OPEN. 세션 ON_HOLD, incidentType · incidentOpenedAt 기록, 이벤트 INCIDENT_OPENED. 차량은 예외 · 복구, 보호자는 예외 · 권한 관리, 자녀는 안전 대기 화면으로 간다.

### 실시간 이벤트 19종
차량은 SSE(SES-08), 보호자 · 자녀 앱은 FCM 데이터 메시지(notification이 보냄)로 받는다. 화면은 stateVersion이 지금보다 작으면 버리고 seq가 건너뛰면 SES-09로 채운다.

| 이벤트 | 언제 | 받는 쪽 | DB 저장 |
|---|---|---|---|
| SESSION_STARTED | SES-02 | 차량 · 보호자 · 자녀 | O |
| SESSION_COMPLETED | 세션 완료 | 차량 · 보호자 · 자녀 | O |
| SESSION_CANCELLED | SES-07 | 차량 · 보호자 · 자녀 | O |
| INCIDENT_OPENED | SES-13 | 차량 · 보호자 · 자녀 | O |
| SPOT_CONFIRMED | SES-04 | 차량 · 보호자 · 자녀 | O |
| SPOT_CHANGED | SES-05 | 차량 · 보호자 · 자녀 | O |
| DEPARTURE_NOTICE_SENT | 출발 알림(세션당 1번) | 차량 · 보호자 | O |
| ETA_UPDATED | SIG-01 | 차량(SSE)만 | X |
| VEHICLE_LOCATION | SIG-01 위치 | 없음(푸시 안 함) | X |
| VEHICLE_STOPPED | SIG-02 STOPPED | 차량 · 보호자 · 자녀 | O |
| VEHICLE_DEPARTED | SIG-02 DEPARTED | 차량 · 보호자 · 자녀 | O |
| BOARDING_DETECTED | SIG-02 DOOR_CYCLE 탑승 후보 | 차량 · 보호자 | O |
| CALL_TRIGGERED | Call 생성(ACCEPTED) | 차량 · 보호자 | O |
| CALL_SENT | 합류 안내 FCM 발송 | 차량 · 보호자 | O |
| CALL_DELIVERED | NOTI-01 | 차량 · 보호자 | O |
| CALL_ACKNOWLEDGED | CALL-04 | 차량 · 보호자 | O |
| CALL_REVOKED | CALL-03, 취소 때 자동 철회 | 차량 · 보호자 · 자녀 | O |
| PREP_TIME_EXTENDED | SIG-03 | 차량 · 보호자 | O |
| HELP_REQUESTED | SIG-05 | 차량 · 보호자 | O |

- 서비스는 트랜잭션 안에서 SessionEvent를 저장하고 PublishSessionEventPort로 스프링 이벤트를 발행한다. SSE 리스너(AFTER_COMMIT · @Async)가 그 세션의 차량 연결로 보낸다. 롤백되면 아무것도 나가지 않는다.
- SES-08: 그 세션의 차량만 연결한다. Last-Event-ID가 오면 그 seq 뒤의 저장 이벤트부터 이어 보낸다. 30초마다 빈 주석(`: ping`)으로 연결을 유지한다.

### 결과 · 기록 (OUT-01~04)
- OUT-01: 자녀가 포함된 세션 중 한국 기준 그 달의 COMPLETED · CANCELLED 세션을 날짜별로.
- OUT-02: diffSeconds = actualStopAt − predictedStopAt(초). 정정이 없으면 correction.status null.
- OUT-03: actualStopAt 원본은 그대로 두고 correctedStopAt · correctionReason · correctionStatus(REQUESTED) · correctedBy만 기록한다. 정정된 건은 분석 · 학습에서 뺀다. correctedStopAt은 세션 날짜 안이어야 한다.
- OUT-04: learningConsent를 바꾸고 retentionDays(90)를 돌려준다. false로 바꾸면 분석용 기록을 바로 파기 대기로 돌린다.

### 서버 기준값 (application.yml)

| 기준 | 값 | 동작 |
|---|---|---|
| ETA 유효시간 | 1분 | SIG-01이 1분 동안 없으면 eta.stale = true, 호출 안 함(G4). 전송 간격의 4배 |
| 호출 트리거 | 180초 · 700m | 판정 3단계 |
| 가드 재평가 주기 | SIG-01마다(15초) | 별도 타이머 없음 |
| 탑승 취소 창 | 10초 | SES-12 허용 시간, 자동 완료 시점 |
| 도움 요청 간격 | 60초 | SIG-05, 아이별 |
| 준비 시간 누적 한도 | 15분 | SIG-03 |
| 허용 실외 대기시간 | 5분 | 호출 뒤 5분이 지나도 정차하지 않으면 보호자에게 FCM |
| 지연 판정 | 10분 | 목표 픽업 시간보다 10분 넘게 늦으면 보호자 화면에 「지연」 |
| SSE ping | 30초 | SES-08 |

### 시그니처

```java
// in 포트 — web · sse · scheduler
Optional<ActiveSessionResult> findActive(AuthPrincipal p);           // FindActiveSessionUseCase
StartPickupResult start(StartPickupCommand c);                       // StartPickupUseCase (created 여부 + SessionView)
SessionView getSession(SessionQuery q);                              // GetSessionUseCase
SessionView confirmRoute(ConfirmRouteCommand c);                     // ConfirmRouteUseCase
SessionView changeSpot(ChangeSpotCommand c);                         // ChangeSpotUseCase
SessionView hold(HoldCommand c); SessionView releaseHold(HoldCommand c);            // HoldSessionUseCase
SessionView cancel(CancelSessionCommand c);                          // CancelSessionUseCase
SessionEventPage listEvents(long sessionId, long afterSeq, int limit, AuthPrincipal p);   // ListSessionEventsUseCase (SES-08 이어 받기 · SES-09)
List<ContactResult> getContacts(SessionQuery q);                     // GetContactsUseCase
SessionView complete(CompletePickupCommand c);                       // CompletePickupUseCase
SessionView rejectBoarding(RejectBoardingCommand c);                 // RejectBoardingUseCase
SessionView openIncident(OpenIncidentCommand c);                     // OpenIncidentUseCase
void receive(TelemetryCommand c);                                    // ReceiveTelemetryUseCase
void receive(VehicleEventCommand c);                                 // ReceiveVehicleEventUseCase
PrepExtensionResult extend(ExtendPrepTimeCommand c);                 // ExtendPrepTimeUseCase
void report(ChildLocationCommand c);                                 // ReportChildLocationUseCase
HelpRequestResult request(HelpRequestCommand c);                     // RequestHelpUseCase
SessionView holdCall(CallHoldCommand c); SessionView releaseCallHold(CallHoldCommand c);   // HoldCallUseCase
CallResult send(SendCallCommand c);                                  // SendCallUseCase
SessionView revoke(RevokeCallCommand c);                             // RevokeCallUseCase
AcknowledgeResult acknowledge(AcknowledgeCallCommand c);             // AcknowledgeCallUseCase
List<HistoryItem> history(HistoryQuery q);                           // GetPickupHistoryUseCase
OutcomeView getOutcome(SessionQuery q);                              // GetOutcomeUseCase
OutcomeView correct(CorrectOutcomeCommand c);                        // CorrectOutcomeUseCase
DataSettingsResult updateDataSettings(DataSettingsCommand c);        // UpdateDataSettingsUseCase
int completeExpiredBoardings(Instant now);                           // AutoCompleteBoardingUseCase

// in 포트 — 다른 모듈용
boolean hasActivePickup(long vehicleId); boolean isInActivePickup(long userId); boolean isSpotInUse(long spotId); Optional<LatLng> activeChildLocation(long familyId);   // ActivePickupQueryUseCase (auth · user · place)
void markCallSent(long callId, Instant sentAt); void markCallDelivered(long callId, Instant deliveredAt); boolean isCallAcknowledged(long callId);   // CallProgressUseCase (notification)

// out 포트
Optional<PickupSession> load(long sessionId); Optional<PickupSession> loadForUpdate(long sessionId); Optional<PickupSession> loadActiveByVehicle(long vehicleId); Optional<PickupSession> loadActiveByFamily(long familyId); Optional<PickupSession> loadActiveByChild(long childUserId); Optional<PickupSession> loadActiveBySpot(long spotId); List<PickupSession> loadBoardingExpired(Instant now);   // LoadSessionPort (loadActiveBySpot: isSpotInUse용, 팀장 결정 2026-11-09)
PickupSession save(PickupSession s);                                 // SaveSessionPort (SessionChild 포함)
Optional<Call> loadCall(long callId); Optional<Call> loadLiveCall(long sessionId); Optional<Call> loadLatestCall(long sessionId); Call saveCall(Call c);   // LoadCallPort · SaveCallPort (loadLatestCall: REVOKED 포함 가장 최근, prevCallId · status.call용)
SessionEvent append(long sessionId, String type, int stateVersion, Instant at, Map<String, Object> payload); SessionEventPage loadAfter(long sessionId, long afterSeq, int limit); Optional<SessionEvent> loadLastHelpRequest(long sessionId, long childUserId); Optional<SessionEvent> loadFirst(long sessionId, String type);   // SessionEventPort
void publish(SessionEventMessage m);                                 // PublishSessionEventPort (저장 이벤트 · ETA_UPDATED 모두)
boolean markProcessed(long sessionId, String vehicleEventId);        // ProcessedVehicleEventPort (SIG-02 eventId 중복 방지, sessionId는 ProcessedVehicleEvent 테이블용 — 팀장 결정 2026-11-09)
Optional<PickupOutcome> loadOutcome(long sessionId); PickupOutcome saveOutcome(PickupOutcome o);   // LoadOutcomePort · SaveOutcomePort
List<HistoryItem> loadHistory(long childUserId, LocalDate from, LocalDate to);   // LoadHistoryPort
List<Long> recordNotifications(NotificationRequest r);               // RecordNotificationPort → notification (같은 트랜잭션)
List<ScheduleInfo> loadSchedules(List<Long> scheduleIds);            // LoadSchedulePort → schedule
Optional<SpotInfo> loadDefaultSpot(long familyId, long academyId); Optional<SpotInfo> loadSpot(long spotId);   // LoadSpotPort → place
FamilyInfo loadFamily(long familyId); List<MemberInfo> loadMembers(long familyId); Optional<MemberInfo> loadMember(long userId);   // LoadFamilyPort → user
Map<Long, MemberDeviceStatus> loadDeviceStatuses(List<Long> userIds); boolean hasLocationConsent(long childUserId);   // LoadChildReadinessPort → auth (auth.domain.MemberDeviceStatus)
```

## 테스트 케이스
- [x] [기반] PickupSession.start는 ACTIVE · ROUTING · SELECTED · stateVersion 0인 세션과 자녀마다 WAITING인 SessionChild를 만든다
- [x] [기반] PickupSession.start는 장소의 spotId · name · stopPoint · meetingPoint를 세션 스냅샷으로 복사한다
- [x] [기반] 세션을 바꾸는 도메인 동작을 하면 stateVersion이 1 오른다
- [x] [기반] hold는 ACTIVE를 ON_HOLD로 바꾸고 releaseHold는 ON_HOLD를 ACTIVE로 되돌린다
- [x] [기반] cancel은 ACTIVE와 ON_HOLD 세션을 CANCELLED로 바꾼다
- [x] [기반] COMPLETED나 CANCELLED 세션에 cancel을 부르면 SESSION_ALREADY_FINISHED로 거절된다
- [x] [기반] confirmRoute는 spotStatus를 CONFIRMED로 바꾸고 routeVersion을 1 올린다
- [x] [기반] changeSpot은 spotStatus를 SELECTED로 되돌리고 spotVersion을 1 올리고 스냅샷을 새 장소 값으로 바꾼다
- [x] [기반] changeSpot은 MEETING 자녀가 있으면 CHILD_ALREADY_MOVING으로 거절된다
- [x] [기반] applyTelemetry는 etaAt을 observedAt + navEtaSeconds로, remainingMeters와 etaUpdatedAt을 새 값으로 덮어쓴다
- [x] [기반] applyTelemetry는 observedAt이 마지막 값보다 오래되면 아무것도 바꾸지 않는다
- [x] [기반] ETA는 etaUpdatedAt이 60초를 넘게 지나면 stale로 판정된다
- [x] [기반] CallTrigger는 navEtaSeconds 180 · remainingMeters 2000이면 ETA_3MIN을 돌려준다
- [x] [기반] CallTrigger는 navEtaSeconds 600 · remainingMeters 700이면 DIST_700M을 돌려준다
- [x] [기반] CallTrigger는 navEtaSeconds 181 · remainingMeters 701이면 트리거 없음을 돌려준다
- [x] [기반] Guard는 ON_HOLD 세션에서 G1을 unmet에 넣는다
- [x] [기반] Guard는 spotStatus가 CONFIRMED가 아니면 G2를 unmet에 넣는다
- [x] [기반] Guard는 확정 경로가 지금 장소 버전과 다르면 G3을 unmet에 넣는다
- [x] [기반] Guard는 ETA가 stale이면 G4를 unmet에 넣는다
- [x] [기반] Guard는 세션 자녀 중 한 명이라도 callNotBefore가 지나지 않았으면 G5를 unmet에 넣는다
- [x] [기반] Guard는 다섯 조건을 모두 만족하면 met true와 빈 unmet을 돌려준다
- [x] [기반] 새 Call은 status ACCEPTED와 triggerType · 그때의 spotVersion을 갖는다
- [x] [기반] 철회된 호출 뒤에 만든 Call은 prevCallId로 이전 호출을 가리킨다
- [x] [기반] Call.markSent는 status를 SENT로 바꾸고 sentAt을 채운다
- [x] [기반] Call.revoke는 status REVOKED · revokedAt · revokeReason을 채운다
- [x] [기반] 이미 REVOKED인 Call을 revoke하면 CALL_ALREADY_REVOKED로 거절된다
- [x] [기반] 자녀 둘인 세션에서 한 명만 확인하면 Call은 ACKNOWLEDGED가 아니고 둘 다 확인하면 ACKNOWLEDGED다
- [x] [기반] SessionChild.acknowledge를 두 번 부르면 첫 acknowledgedAt이 유지된다
- [x] [기반] SessionChild.acknowledge는 CALLED 자녀를 MEETING으로 바꾼다
- [x] [기반] extendPrep은 18:09에 5분을 더하면 extraPrepMinutes 5 · callNotBefore 18:14로 만든다
- [x] [기반] extendPrep은 18:05에 5분, 18:09에 5분을 더하면 extraPrepMinutes 10 · callNotBefore 18:19로 만든다
- [x] [기반] extendPrep은 누적이 15분을 넘으면 PREP_EXTENSION_LIMIT으로 거절된다
- [x] [기반] extendPrep은 childPhase를 바꾸지 않는다
- [x] [기반] detectBoarding은 STOPPED · 운전석 아닌 문 · 호출 발송 뒤면 boardingDetectedAt · boardingCancelableUntil(+10초) · boardingDoor와 모든 자녀의 boardedAt을 채운다
- [x] [기반] detectBoarding은 STOPPED가 아니면 아무것도 바꾸지 않는다
- [x] [기반] detectBoarding은 합류 안내 발송 전이면 아무것도 바꾸지 않는다
- [x] [기반] rejectBoarding은 취소 창 안이면 그 자녀의 boardedAt만 비운다
- [x] [기반] rejectBoarding은 boardingCancelableUntil이 지났으면 REJECTION_WINDOW_CLOSED로 거절된다
- [x] [기반] complete는 MANUAL인데 vehiclePhase가 STOPPED가 아니면 VEHICLE_NOT_STOPPED로 거절된다
- [x] [기반] complete는 notBoardedChildUserIds의 자녀를 NOT_BOARDED로 바꾸고 세션을 COMPLETED로 바꾼다
- [x] [기반] complete는 탑승하지도 미탑승 지정되지도 않은 자녀가 있으면 CHILDREN_NOT_ALL_BOARDED로 거절된다
- [x] [기반] complete는 모든 자녀의 lastLat · lastLng · lastLocatedAt을 비운다
- [x] [기반] openIncident는 세션을 ON_HOLD로 바꾸고 incidentType · incidentOpenedAt을 채운다
- [x] [기반] 열린 예외가 있는 세션에 openIncident를 부르면 INCIDENT_ALREADY_OPEN으로 거절된다
- [x] [기반] updateChildLocation은 at이 저장된 값보다 오래되면 위치를 바꾸지 않는다
- [x] [기반] 도움 요청 규칙은 같은 아이의 마지막 요청 뒤 60초 안이면 HELP_REQUEST_TOO_SOON과 마지막 시각 + 60초를 돌려준다
- [x] [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] [기반] JPA 엔티티 5개가 BaseTimeEntity를 상속하고 테이블 정의대로 매핑된다(PickupOutcome PK는 sessionId)
- [x] [기반] 테스트 시드로 차량 1대 · 자녀 2명 · 기본 장소가 있는 ACTIVE 세션 1개를 만든다
- [ ] [persistence] PickupSession을 SessionChild와 함께 저장하고 읽으면 스냅샷 좌표와 자녀 상태가 같다
- [ ] [persistence] 같은 세션에 같은 childUserId의 SessionChild를 두 번 저장하면 유니크 위반이 난다
- [ ] [persistence] loadActiveByVehicle은 그 차량의 ACTIVE · ON_HOLD 세션만 찾는다
- [ ] [persistence] loadActiveByChild는 그 자녀가 포함된 진행 중 세션을 찾는다
- [ ] [persistence] loadForUpdate로 잠근 세션을 다른 트랜잭션이 loadForUpdate하면 앞 트랜잭션이 끝날 때까지 기다린다
- [ ] [persistence] append는 세션 안 seq를 1부터 1씩 늘려 저장한다
- [ ] [persistence] 같은 세션에 같은 seq의 SessionEvent를 저장하면 유니크 위반이 난다
- [ ] [persistence] loadAfter는 afterSeq보다 큰 이벤트를 seq 오름차순으로 limit개 돌려주고 남으면 hasMore true다
- [ ] [persistence] loadLastHelpRequest는 payload의 childUserId로 그 아이의 마지막 HELP_REQUESTED를 찾는다
- [ ] [persistence] loadBoardingExpired는 boardingCancelableUntil이 지난 ACTIVE 세션만 돌려준다
- [ ] [persistence] loadLiveCall은 그 세션의 REVOKED가 아닌 가장 최근 Call을 돌려준다
- [ ] [persistence] PickupOutcome은 sessionId를 PK로 저장되고 같은 세션에 두 번째 행은 저장되지 않는다
- [ ] [persistence] loadHistory는 그 자녀가 포함된 세션 중 기간 안의 COMPLETED · CANCELLED 세션만 돌려준다
- [ ] [persistence] markProcessed는 처음 eventId에 true, 같은 eventId에 false를 돌려준다
- [ ] [서비스] 픽업 시작은 date가 한국 날짜로 오늘이 아니면 NOT_TODAY로 거절한다
- [ ] [서비스] 픽업 시작은 학원이 다른 일정이 섞이면 SCHEDULES_NOT_GROUPABLE로 거절한다
- [ ] [서비스] 픽업 시작은 시간대가 다른 일정이 섞이면 SCHEDULES_NOT_GROUPABLE로 거절한다
- [ ] [서비스] 픽업 시작은 기본 장소가 없으면 SETUP_INCOMPLETE와 missing PICKUP_SPOT으로 거절한다
- [ ] [서비스] 픽업 시작은 자녀 기기가 없으면 SETUP_INCOMPLETE와 missing CHILD_DEVICE로 거절한다
- [ ] [서비스] 픽업 시작은 DRAFT 일정이 있으면 SETUP_INCOMPLETE로 거절한다
- [ ] [서비스] 픽업 시작은 그 차량에 다른 진행 중 세션이 있으면 ANOTHER_SESSION_ACTIVE로 거절한다
- [ ] [서비스] 픽업 시작은 같은 일정의 진행 중 세션이 있으면 새로 만들지 않고 그 세션을 created false로 돌려준다
- [ ] [서비스] 픽업 시작은 SESSION_STARTED를 seq 1로 저장하고 SESSION_STARTED 알림 기록을 요청한다
- [ ] [서비스] 진행 중 세션 찾기는 차량 주체에 그 차량의 세션, 자녀 주체에 자기가 포함된 세션을 돌려준다
- [ ] [서비스] 진행 중 세션 찾기는 세션이 없으면 빈 값을 돌려준다
- [ ] [서비스] 세션 조회는 자녀 주체에게 자기 children 항목만 돌려준다
- [ ] [서비스] 세션 조회는 SES-04 전이면 자녀 주체에게 spot을 null로 주고 eta.stopEtaAt은 준다
- [ ] [서비스] 세션 조회는 자녀 주체에게 children[].location을 주지 않고 차량 · 보호자 주체에게는 준다
- [ ] [서비스] 세션 조회는 호출이 없으면 status.call NONE을 돌려준다
- [ ] [서비스] 세션 조회는 MEETING 자녀가 있으면 spot.locked true를 돌려준다
- [ ] [서비스] 다른 가족의 세션을 조회하면 NOT_FOUND로 거절한다
- [ ] [서비스] 세션을 바꾸는 요청의 stateVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절한다
- [ ] [서비스] 경로 확정은 가족이 저장하지 않은 spotId면 SPOT_NOT_SAVED로 거절한다
- [ ] [서비스] 첫 경로 확정은 자녀에게 DEPARTURE 알림 기록을 요청하고 DEPARTURE_NOTICE_SENT를 저장한다
- [ ] [서비스] 두 번째 경로 확정은 출발 알림을 다시 요청하지 않는다
- [ ] [서비스] 경로 확정은 SPOT_CONFIRMED를 저장한다
- [ ] [서비스] 장소 변경은 철회되지 않은 발송 호출이 있으면 CALL_ALREADY_SENT로 거절한다
- [ ] [서비스] 장소 변경이 성공하면 SPOT_CHANGED를 저장한다
- [ ] [서비스] 픽업 취소는 철회되지 않은 호출을 REVOKED로 바꾸고 CALL_REVOKED와 SESSION_CANCELLED를 저장한다
- [ ] [서비스] 픽업 취소는 자녀 위치 세 컬럼을 비운다
- [ ] [서비스] 연락처는 전화번호가 있는 가족 멤버(CHILD 제외)와 role ACADEMY인 학원 대표번호를 돌려준다
- [ ] [서비스] 차량 ETA 수신은 ETA_UPDATED를 저장하지 않고 발행만 한다
- [ ] [서비스] AUTO 가족에서 트리거와 가드를 통과하면 Call ACCEPTED · CALL 알림 기록 요청 · CALL_TRIGGERED를 한 트랜잭션으로 저장한다
- [ ] [서비스] 호출 저장 중 예외가 나면 Call · SessionEvent가 남지 않고 이벤트도 발행되지 않는다
- [ ] [서비스] ON_HOLD 세션에서는 트리거를 만족해도 호출을 만들지 않는다
- [ ] [서비스] callHeld가 true면 트리거를 만족해도 호출을 만들지 않는다
- [ ] [서비스] 철회되지 않은 호출이 있으면 새 호출을 만들지 않는다
- [ ] [서비스] 가드를 통과하지 못하면 호출을 만들지 않고 세션 뷰 call.guard.unmet에 그 가드를 보인다
- [ ] [서비스] GUARDIAN_CONFIRM 가족에서는 트리거와 가드를 통과해도 호출을 자동으로 만들지 않는다
- [ ] [서비스] callNotBefore 전에는 트리거를 만족해도 호출을 만들지 않고 callNotBefore 뒤 첫 SIG-01에서 만든다
- [ ] [서비스] 철회된 호출 뒤 조건이 다시 맞으면 prevCallId를 단 새 호출을 만든다
- [ ] [서비스] markCallSent는 Call을 SENT로, 세션 자녀를 CALLED로 바꾸고 CALL_SENT를 저장한다
- [ ] [서비스] markCallDelivered는 Call을 DELIVERED로 바꾸고 CALL_DELIVERED를 저장한다
- [ ] [서비스] 차량 이벤트 수신은 같은 eventId를 두 번째로 받으면 무시한다
- [ ] [서비스] 차량 이벤트 STOPPED는 vehiclePhase를 STOPPED로 바꾸고 VEHICLE_STOPPED를 저장한다
- [ ] [서비스] 차량 이벤트 DEPARTED는 vehiclePhase를 DEPARTED로 바꾸고 VEHICLE_DEPARTED를 저장한다
- [ ] [서비스] 탑승 후보가 된 DOOR_CYCLE은 BOARDING_DETECTED를 저장한다
- [ ] [서비스] 호출 보류는 이미 보낸 호출이 있으면 CALL_ALREADY_SENT로 거절한다
- [ ] [서비스] 호출 보류 해제는 callHeld를 false로 바꾸고 저장된 ETA로 판정을 다시 돌려 조건이 맞으면 호출을 만든다
- [ ] [서비스] 합류 안내 보내기는 AUTO 가족이면 NOT_GUARDIAN_CONFIRM_MODE로 거절한다
- [ ] [서비스] 합류 안내 보내기는 가드를 통과하지 못하면 CALL_GUARD_UNMET과 unmet 목록으로 거절한다
- [ ] [서비스] 합류 안내 보내기는 이미 보낸 호출이 있으면 CALL_ALREADY_SENT로 거절한다
- [ ] [서비스] 합류 안내 보내기가 성공하면 triggerType MANUAL인 Call을 만든다
- [ ] [서비스] 호출 철회는 세션 자녀를 WAITING으로 되돌리고 위치를 비우고 CALL_REVOKED를 저장한다
- [ ] [서비스] 호출 확인은 형제가 모두 확인했을 때만 CALL_ACKNOWLEDGED를 저장한다
- [ ] [서비스] 철회된 호출을 확인하면 CALL_ALREADY_REVOKED로 거절한다
- [ ] [서비스] 준비 시간 추가는 합류 안내를 이미 받은 자녀면 CALL_ALREADY_SENT로 거절한다
- [ ] [서비스] 준비 시간 추가는 PREP_TIME_EXTENDED를 저장하고 보호자에게 PREP_EXTENDED 알림 기록을 요청한다
- [ ] [서비스] 자녀 위치는 위치 동의가 없으면 LOCATION_CONSENT_REQUIRED로 거절한다
- [ ] [서비스] 자녀 위치는 끝난 세션이면 SESSION_ALREADY_FINISHED로 거절한다
- [ ] [서비스] 자녀 위치는 WAITING 자녀면 409로 거절한다
- [ ] [서비스] 자녀 위치는 CALLED 자녀의 lastLat · lastLng · lastLocatedAt을 덮어쓴다
- [ ] [서비스] 도움 요청은 HELP_REQUESTED(payload childUserId · type)를 저장하고 보호자에게 HELP 알림 기록을 요청하고 nextAllowedAt = 지금 + 60초를 돌려준다
- [ ] [서비스] 도움 요청 1분 제한은 형제의 요청에 영향을 받지 않는다
- [ ] [서비스] 탑승 완료 IMMEDIATE는 PickupOutcome을 boardingMethod IMMEDIATE로 저장하고 SESSION_COMPLETED를 저장한다
- [ ] [서비스] 탑승 완료 MANUAL은 미탑승 지정 외 자녀의 boardedAt을 채우고 boardingMethod MANUAL로 저장한다
- [ ] [서비스] 탑승 완료는 PickupOutcome.actualStopAt에 VEHICLE_STOPPED의 occurredAt을 넣는다
- [ ] [서비스] 자동 완료 작업은 취소 창이 지나고 모든 자녀가 탄 세션을 boardingMethod DOOR로 완료한다
- [ ] [서비스] 자동 완료 작업은 SES-12로 boardedAt이 비워진 자녀가 있는 세션을 완료하지 않는다
- [ ] [서비스] 탑승 취소는 자녀가 여러 명인데 childUserId가 없으면 VALIDATION_FAILED로 거절한다
- [ ] [서비스] 탑승 예외는 INCIDENT_OPENED를 저장한다
- [ ] [서비스] 픽업 이력은 month 2026-11에 한국 날짜 11월 1일~30일 세션을 돌려준다
- [ ] [서비스] 픽업 결과는 예상 18:11:00 · 실제 18:11:50이면 diffSeconds 50을 돌려준다
- [ ] [서비스] 결과 정정은 actualStopAt을 그대로 두고 correctedStopAt · correctionReason · correctionStatus REQUESTED · correctedBy를 채운다
- [ ] [서비스] 결과 정정은 correctedStopAt이 세션 날짜 밖이면 VALIDATION_FAILED로 거절한다
- [ ] [서비스] 데이터 설정 변경은 learningConsent와 retentionDays 90을 돌려준다
- [ ] [서비스] SSE 리스너는 커밋된 세션 이벤트를 그 세션 차량의 연결로 보낸다
- [ ] [web] GET /v1/pickup-sessions/active는 진행 중 세션이 있으면 200과 sessionId · status · stateVersion을 돌려준다
- [ ] [web] GET /v1/pickup-sessions/active는 진행 중 세션이 없으면 204를 돌려준다
- [ ] [web] POST /v1/pickup-sessions는 새 세션이면 201과 SessionView를 돌려준다
- [ ] [web] POST /v1/pickup-sessions는 진행 중인 같은 세션이면 200을 돌려준다
- [ ] [web] POST /v1/pickup-sessions가 오늘이 아니면 422 NOT_TODAY를 돌려준다
- [ ] [web] POST /v1/pickup-sessions에 빈 scheduleIds를 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] [web] POST /v1/pickup-sessions를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}는 200과 SessionView 필드를 돌려준다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}의 시각 필드는 +09:00으로 내려간다
- [ ] [web] PUT /v1/pickup-sessions/{sessionId}/route는 200과 SessionView를 돌려준다
- [ ] [web] PUT /v1/pickup-sessions/{sessionId}/route가 저장하지 않은 장소면 422 SPOT_NOT_SAVED를 돌려준다
- [ ] [web] PUT /v1/pickup-sessions/{sessionId}/route의 stateVersion이 다르면 409 STATE_VERSION_CONFLICT와 최신 세션 뷰를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/spot-changes는 200과 SessionView를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/spot-changes가 자녀 이동 중이면 409 CHILD_ALREADY_MOVING을 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/spot-changes를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] PUT /v1/pickup-sessions/{sessionId}/hold는 200과 status.session ON_HOLD인 SessionView를 돌려준다
- [ ] [web] DELETE /v1/pickup-sessions/{sessionId}/hold는 쿼리 stateVersion을 받아 200과 status.session ACTIVE인 SessionView를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/cancellation은 200과 status.session CANCELLED인 SessionView를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/cancellation이 끝난 세션이면 409 SESSION_ALREADY_FINISHED를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/cancellation을 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/stream은 그 세션 차량 토큰에 text/event-stream으로 응답한다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/stream을 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/stream에 Last-Event-ID 41을 보내면 seq 42 이후 저장 이벤트부터 보낸다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/stream은 이벤트를 event · id(seq) · data(eventId · seq · stateVersion · occurredAt · payload) 형식으로 보낸다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/stream은 ping 간격마다 빈 주석 `: ping`을 보낸다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/events는 200과 items[] · hasMore를 돌려준다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/events에 limit 101을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/contacts는 200과 items[](name · role · phone)를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/completion은 200과 status.session COMPLETED인 SessionView를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/completion이 MANUAL인데 정차 중이 아니면 422 VEHICLE_NOT_STOPPED를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/completion이 빠진 자녀가 있으면 422 CHILDREN_NOT_ALL_BOARDED를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/boarding-rejection은 200과 SessionView를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/boarding-rejection이 10초가 지났으면 409 REJECTION_WINDOW_CLOSED를 돌려준다
- [ ] [web] PUT /v1/pickup-sessions/{sessionId}/incident는 200과 incident가 채워진 SessionView를 돌려준다
- [ ] [web] PUT /v1/pickup-sessions/{sessionId}/incident가 이미 열린 예외면 409 INCIDENT_ALREADY_OPEN을 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry는 202를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry에 음수 navEtaSeconds를 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/vehicle-events는 202를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/vehicle-events에 door 없는 DOOR_CYCLE을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions는 200과 extraPrepMinutes · callNotBefore를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions에 minutes 7을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions가 누적 15분을 넘으면 422 PREP_EXTENSION_LIMIT을 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions를 GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/child-locations는 202를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/child-locations가 동의 없으면 403 LOCATION_CONSENT_REQUIRED를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/help-requests는 201과 nextAllowedAt을 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/help-requests가 1분 안 재요청이면 429 HELP_REQUEST_TOO_SOON과 nextAllowedAt을 돌려준다
- [ ] [web] PUT /v1/pickup-sessions/{sessionId}/call-hold는 200과 call.held true인 SessionView를 돌려준다
- [ ] [web] PUT /v1/pickup-sessions/{sessionId}/call-hold가 발송 뒤면 409 CALL_ALREADY_SENT를 돌려준다
- [ ] [web] DELETE /v1/pickup-sessions/{sessionId}/call-hold는 쿼리 stateVersion을 받아 200과 call.held false인 SessionView를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/calls는 201과 callId · status를 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/calls가 가드 미충족이면 422 CALL_GUARD_UNMET과 unmet을 돌려준다
- [ ] [web] POST /v1/pickup-sessions/{sessionId}/calls가 AUTO 가족이면 409 NOT_GUARDIAN_CONFIRM_MODE를 돌려준다
- [ ] [web] POST /v1/calls/{callId}/revocation은 200과 status.call REVOKED인 SessionView를 돌려준다
- [ ] [web] POST /v1/calls/{callId}/revocation이 이미 철회된 호출이면 409 CALL_ALREADY_REVOKED를 돌려준다
- [ ] [web] POST /v1/calls/{callId}/revocation을 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] POST /v1/calls/{callId}/acknowledgement는 200과 callId · status · acknowledgedAt을 돌려준다
- [ ] [web] POST /v1/calls/{callId}/acknowledgement를 차량 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] GET /v1/children/{childId}/pickup-history는 200과 items[](date · sessionId · status · completedAt)를 돌려준다
- [ ] [web] GET /v1/children/{childId}/pickup-history에 month 2026-13을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/outcome은 200과 completedAt · spotName · boardingMethod · predictedStopAt · actualStopAt · diffSeconds · correction을 돌려준다
- [ ] [web] PUT /v1/pickup-sessions/{sessionId}/outcome/correction은 200과 correction.status REQUESTED를 돌려준다
- [ ] [web] PUT /v1/pickup-sessions/{sessionId}/outcome/correction에 201자 note를 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] [web] PUT /v1/guardians/me/data-settings는 200과 learningConsent · retentionDays를 돌려준다
- [ ] [web] PUT /v1/guardians/me/data-settings를 CO_GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] Idempotency-Key는 SES-02 · 04~07 · 11~13, SIG-03 · 05, CALL-01~04에만 요구되고 SIG-01 · 02 · 04와 조회 · OUT API에는 요구되지 않는다
- [x] [기반] 모듈 연결 out 포트의 어댑터가 있다: LoadSchedulePort는 schedule, LoadSpotPort는 place, LoadFamilyPort는 user, LoadChildReadinessPort는 auth의 in 포트로 읽고, RecordNotificationPort는 notification 구현 전 임시로 빈 목록을 돌려준다
- [x] [기반] 공통 오류 본문은 도메인 오류가 error 안에 추가 키(unmet · missing · nextAllowedAt)를 더할 수 있고, STATE_VERSION_CONFLICT는 error와 나란히 session에 최신 세션 뷰를 싣는다
- [x] [기반] PickupErrors는 API 명세의 픽업 오류 코드마다 상태 · code · 한국어 메시지를 담은 예외를 만든다
- [ ] user · auth · place의 진행 중 픽업 확인 어댑터가 임시 false · 빈 값 대신 pickup ActivePickupQueryUseCase를 부른다
- [x] [기반] GuardianDataSetting 엔티티는 userId PK · learningConsent로 매핑되고 PickupOutcome 엔티티는 correctionNote VARCHAR(200)을 갖고, out 포트에 LoadDataSettingPort · SaveDataSettingPort가 있다
- [ ] [persistence] GuardianDataSetting을 저장하고 읽으면 learningConsent가 같고 행이 없는 보호자는 빈 값을 돌려준다
- [x] [기반] SessionEvent 도메인은 eventId · sessionId · seq · type(SessionEventType 19종) · stateVersion · occurredAt · payload를 담고 restore로 다시 만들어진다
- [x] [기반] PickupOutcome 도메인은 완료 때 completedAt · boardingMethod · predictedStopAt · actualStopAt으로 만들어지고, 정정은 actualStopAt을 두고 correctedStopAt · correctionReason · correctionStatus REQUESTED · correctedBy · correctionNote를 채우며, diffSeconds는 둘 중 하나가 없으면 비어 있다
- [x] [기반] GuardianDataSetting 도메인은 행이 없으면 learningConsent false로 만들어지고 동의 값을 바꿀 수 있다
- [x] [기반] Call은 markDelivered로 DELIVERED · deliveredAt을 채우고, 모든 필드를 restore로 되살리며, REVOKED인 Call에 늦게 온 markSent · markDelivered는 무시한다
- [x] [기반] PickupSession은 childUserId로 자녀를 바꾸는 진입점(호출됨 CALLED · 철회로 WAITING과 위치 · acknowledgedAt 비움 · 확인 MEETING · 준비 시간 추가 · 위치 갱신)을 갖고, 위치 갱신 밖의 변경은 stateVersion을 1 올리며, 세션에 없는 자녀면 CHILD_NOT_IN_SESSION으로 거절된다
- [x] [기반] PickupSession은 markStopped · markDeparted로 vehiclePhase를 STOPPED · DEPARTED로, holdCall · releaseCallHold로 callHeld를 바꾸고 stateVersion을 1 올린다
- [x] [기반] 끝난 세션의 hold · releaseHold · complete · detectBoarding · openIncident는 SESSION_ALREADY_FINISHED로 거절되고, releaseHold는 열린 예외의 incidentResolvedAt을 채우며, 탑승 인식 전 rejectBoarding은 REJECTION_WINDOW_CLOSED로 거절된다
- [x] [기반] PickupSession은 familyId를 start · restore로 담고 pickup_session은 family_id 컬럼을 가지며, LoadSessionPort에 loadActiveBySpot · LoadCallPort에 loadLatestCall(REVOKED 포함) · LoadFamilyPort에 loadMember(userId)가 있다
- [x] [기반] PickupErrors는 CHILD_NOT_CALLED 409를 만들고 PickupViolation CHILD_NOT_IN_SESSION을 NOT_FOUND로 바꾼다

## 확인 필요 사항

| 항목 | 현재 가정 |
|---|---|
| 「같은 시간대」 묶기 기준(SES-02) | 소스에 없음. 일정의 classEndTime + targetPickup(목표 픽업 시각)이 같으면 같은 시간대로 가정 |
| 「진행 중인 같은 세션」(SES-02 200) | 같은 차량 · 같은 date · 같은 scheduleIds 묶음으로 가정. 다르면 409 ANOTHER_SESSION_ACTIVE |
| DRAFT 일정으로 시작할 때 오류 코드 | 소스는 「픽업 시작 불가」만 있음. 422 SETUP_INCOMPLETE로 가정 |
| 일정 값 스냅샷 컬럼 | 소스는 schedule 값도 스냅샷한다고 했지만 SessionChild에는 scheduleId만 있음. 지연 판정(목표 시각 + 10분)에 쓸 목표 시각을 어디서 읽을지 정해야 함 |
| 합류 안내 방식 스냅샷 | SET-06은 「다음 세션부터 적용」인데 PickupSession에 mode 컬럼이 없음. 지금은 판정 때 Family.joinCallMode를 읽는다고 가정(진행 중에 바뀌면 바로 반영됨) |
| G3 「경로 일치」의 판정 | 소스에 정의 없음. SES-04로 확정한 장소 버전과 지금 spotVersion이 같으면 통과로 가정 |
| G5와 형제 | Call은 세션에 하나라 모든 세션 자녀의 callNotBefore가 지나야 통과로 가정. callNotBefore가 지금과 같은 시각이면 지난 것으로 본다(「이 시각 전에는 호출 안 함」) |
| ETA_3MIN · DIST_700M이 동시에 맞을 때 triggerType | 소스에 없음. ETA_3MIN으로 가정 |
| vehiclePhase APPROACHING 기준 | 「SIG-01 ETA · 거리 기준」만 있음. 기준값을 정해야 함 |
| spotStatus ROUTING · INVALIDATED로 바꾸는 계기 | 이 상태로 바꾸는 API가 없음. 정해야 함 |
| navRouteId 저장 위치 | SES-04 요청에 있으나 컬럼이 없음. 저장하지 않는다고 가정 |
| SES-05 spotVersion 불일치 오류 | 소스에 없음. 409 STATE_VERSION_CONFLICT로 가정 |
| SES-05에서 CALL_ALREADY_SENT와 CHILD_ALREADY_MOVING이 함께 맞을 때 | MEETING이면 호출도 보낸 상태. CHILD_ALREADY_MOVING을 먼저 본다고 가정 |
| SES-06 · 07 reason 저장 위치 | 세션에 사유 컬럼이 없고 보류 이벤트도 19종에 없음. 취소 사유는 자동 철회 Call.revokeReason에 넣고 보류 사유는 저장하지 않는다고 가정 |
| CALL-02 응답 status | [사람 결정 2026-11-09] 응답 시점의 실제 상태 ACCEPTED를 준다. 발송(SENT)은 CALL_SENT 이벤트로 알린다 |
| Call SENT를 기록하는 주체 | notification이 FCM 성공 뒤 pickup in 포트(markCallSent)를 부른다고 가정 |
| CALL-02가 트리거(180초 · 700m)도 검사하는지 | 소스에는 가드 미충족 오류만 있음. 가드만 검사한다고 가정 |
| CALL_GUARD_UNMET의 unmet 위치 | 오류 형식에 unmet 칸이 없음. error 안에 unmet 배열을 더한다고 가정 |
| CALL-03으로 ACCEPTED(발송 전) 호출을 철회할 수 있는지 | 소스는 「발송 후」만 말함. ACCEPTED도 철회 가능으로 가정 |
| CALL-03 때 MEETING 자녀 | 상태 다이어그램은 CALLED → WAITING만 있음. 4.14대로 MEETING도 WAITING으로 돌린다고 가정 |
| CALL-04 = MEETING | 4.9는 「열람 ≠ 이동 시작」, 3장 · 9.6은 CALL-04로 MEETING. 3장 · 9.6을 따름 |
| boardedAt 기록 시점 | 플로우차트는 「창이 지나면 기록」, SES-12는 「boardedAt을 비움」. DOOR_CYCLE 때 기록하고 SES-12가 비운다고 가정 |
| SES-12 뒤 예외 열기 | 플로우차트는 SES-12 다음에 SES-13 예외로 이어짐. 서버가 자동으로 열지 않고 차량 앱이 SES-13을 부른다고 가정 |
| SES-11 IMMEDIATE를 탑승 인식 전에 누른 경우 | 소스에 없음. 탑승하지 않은 자녀가 있으면 422 CHILDREN_NOT_ALL_BOARDED로 가정 |
| 완료 뒤 · 주행 중 SES-13 | [사람 결정 2026-11-09 「끝난 세션의 동작」에 따름] 완료 · 취소 세션에서는 예외를 열 수 없다(409 SESSION_ALREADY_FINISHED) |
| 끝난 세션의 차량 정차 · 출발, 호출 보류 | [팀장 결정 2026-11-09] markStopped · markDeparted는 끝난 세션에서도 받는다(완료 뒤 출발이 정상 흐름). holdCall · releaseCallHold는 끝난 세션이면 409 SESSION_ALREADY_FINISHED |
| incident의 childUserId 저장 | 세션 뷰에는 incident.childUserId가 있으나 컬럼이 없음. INCIDENT_OPENED payload에 저장하고 거기서 읽는다고 가정 |
| 예외 해소(incidentResolvedAt) 방법 | 해소 API가 없음. SES-06 DELETE(보류 해제) 때 채운다고 가정 |
| actualStopAt · predictedStopAt 값 | actualStopAt은 VEHICLE_STOPPED occurredAt. [사람 결정 2026-11-09] predictedStopAt은 SIG-02 STOPPED를 받을 때의 세션 etaAt(정차 직전 마지막 ETA)이고 VEHICLE_STOPPED payload에 담아 완료 때 읽는다. etaAt이 없으면 null |
| SIG-02 STOP_FAILED 처리 | 소스에 없음. 상태 변화 없이 무시로 가정 |
| SIG-02 eventId 저장 위치 | 「Idempotency-Key로 써서 중복 무시」. idempotency_key 테이블에 저장한다고 가정 |
| SIG-04 WAITING · 철회 뒤 요청의 오류 코드 | [사람 결정 2026-11-09] 끝난 세션은 SESSION_ALREADY_FINISHED, 그 밖(WAITING · 철회 뒤)은 409 CHILD_NOT_CALLED |
| SES-04 spotId가 세션 장소와 다를 때 | [사람 결정 2026-11-09] 409 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절. 장소 변경은 SES-05로만 |
| 자녀 단위 변경과 stateVersion | [사람 결정 2026-11-09] 호출됨 · 확인 · 준비 시간 · 철회로 자녀 상태가 바뀌면 stateVersion +1. 자녀 위치(SIG-04) · ETA(SIG-01)는 올리지 않음 |
| 철회 때 acknowledgedAt | [사람 결정 2026-11-09] 철회하면 자녀 acknowledgedAt도 비워 새 호출에서 다시 확인 |
| 탑승 인식 전 SES-12 | [사람 결정 2026-11-09] 409 REJECTION_WINDOW_CLOSED |
| 끝난 세션의 동작 | [사람 결정 2026-11-09] COMPLETED · CANCELLED 세션의 보류 · 보류 해제 · 완료 · 탑승 인식 · 예외 열기는 409 SESSION_ALREADY_FINISHED |
| 철회된 호출에 늦게 온 발송 · 수신 확인 | [사람 결정 2026-11-09] 무시(상태 변화 없음) |
| CALL_ALREADY_SENT의 「이미 보냄」 | [사람 결정 2026-11-09] 철회되지 않은 호출이 있으면(ACCEPTED 포함) |
| 예외 해소 | [사람 결정 2026-11-09] 보류 해제(SES-06 DELETE) 때 열린 예외의 incidentResolvedAt을 채움 |
| 세션에 없는 childUserId | [팀장 결정 2026-11-09] 404 NOT_FOUND |
| SessionEvent.type | [팀장 결정 2026-11-09] 도메인에서는 19종 enum(SessionEventType), 저장은 문자열 |
| 세션 뷰 조립 위치 | [팀장 결정 2026-11-09] SessionView는 port.in record라 application/service에서 조립(「구조」 절의 domain 조립 규칙 대신) |
| 위치 동의 저장 위치 | 동의 테이블이 없음(05_인증-auth.md에도 기록) |
| SIG-01이 끝난 세션에 올 때 | 소스에 없음. 202로 받고 무시한다고 가정 |
| 세션 행 잠금 범위 | 소스는 SIG-05만 명시. 세션을 바꾸는 모든 요청을 loadForUpdate로 처리한다고 가정 |
| call.scheduledAt 계산 | 예시는 ETA − 3분(18:12 → 18:09). 자녀 callNotBefore 중 가장 늦은 값보다 앞서지 않게 한다고 가정 |
| 완료 세션 뷰의 nextPickup | 5.3은 「완료 세션에 nextPickup 포함」이나 세션 뷰 필드에 없음. 필드 모양을 정해야 함 |
| 허용 실외 대기 5분 · 지연 10분의 실행 시점 | 별도 타이머가 없고 세션 뷰에 지연 필드도 없음. 실행 방법과 응답 필드를 정해야 함 |
| Notification 행을 만드는 이벤트 | Notification type이 SESSION_STARTED · DEPARTURE · CALL · PREP_EXTENDED · HELP 등. 이 다섯은 행을 만들고 나머지 이벤트는 행 없이 FCM 데이터 메시지만 보낸다고 가정 |
| DEPARTURE_NOTICE_SENT 기록 시점 | SES-04 커밋 때 저장한다고 가정(실제 FCM 발송은 커밋 뒤) |
| SSE에서 저장 안 하는 ETA_UPDATED의 id | seq가 없음. id 줄을 빼고 보낸다고 가정 |
| SSE 연결이 여러 개일 때 | 같은 세션에 차량 연결이 둘 이상이면 모두에 보낸다고 가정 |
| OUT-02 · 03을 완료 전 세션에 부를 때 | PickupOutcome이 없음. 404 NOT_FOUND로 가정 |
| OUT-03 note 저장 위치 | [사람 결정 2026-11-09] PickupOutcome.correctionNote VARCHAR(200) |
| OUT-04 learningConsent · 파기 대기 저장 위치 | [사람 결정 2026-11-09] pickup 모듈의 GuardianDataSetting(userId PK, learningConsent). 「파기 대기」 처리는 후속 과제 |
| 오류 본문의 추가 값 위치 | unmet · missing · nextAllowedAt은 error 안의 키(schedule overlapScheduleIds와 같은 방식). STATE_VERSION_CONFLICT의 최신 세션 뷰는 error와 나란히 session(03 가정) |
| 문서 크기 | 엔드포인트 28개 · 항목이 많음. 소스의 모듈 경계(pickup 한 패키지)를 따라 한 문서로 둠. 나눌지 팀장이 정함 |
| SIG-02 eventId와 idempotency_key 테이블 | [팀장 결정 2026-11-09, 사람 확인 전] idempotency_key와 구조가 달라 pickup 전용 테이블 ProcessedVehicleEvent(eventId VARCHAR(36) PK, sessionId BIGINT, createdAt)에 저장한다. markProcessed는 이 PK 충돌로 중복을 판정 |
| SES-08 SSE 요청 로그 · 비동기 실행기 | 03 요청 로깅은 SSE를 연결(stream=open) · 종료(stream=closed) 두 줄로 남김. 타임아웃 · 연결 끊김 · completeWithError 때 종료 줄의 status · errorCode와 MVC 비동기 실행기 설정 필요 여부를 SES-08 때 정해야 함 |
