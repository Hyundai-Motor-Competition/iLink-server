# 06. 일정 (schedule)

학원 · 일정과 설정 요약 · 완료, 차량용 주간 일정 · 설정 요약. 자녀는 role = CHILD인 User이고 경로의 {childId}는 그 userId다.

| 항목 | 내용 |
|---|---|
| 범위 | 엔티티 Academy · PickupSchedule. API SET-01~04, SET-07~10. 다른 모듈용 in 포트(일정 조회 · 학원 조회) |
| 의존 | 03_공유모듈-shared, 04_사용자-user(자녀 · 가족 · 합류 안내 방식 · settingsVersion · 보호자 이름. 서비스가 user in 포트 LoadMemberUseCase · LoadFamilyUseCase를 직접 부름), 05_인증-auth(자녀 기기 상태. out 포트 LoadMemberDeviceStatusPort → auth in 포트). 07_장소-place(장소 목록 · 기본 장소 유무. out 포트 LoadPickupSpotPort → place in 포트 LoadPickupSpotUseCase.findSpots). schedule 코드는 user.domain 타입을 쓰지 않는다(JoinCallMode · DeviceStatus는 schedule 자체 enum) |
| 진행 | 계층 병렬 |

## 구조

```text
schedule
├─ adapter/in/web          ScheduleController(SET-01~04), SetupController(SET-07 · 08), VehicleScheduleController(SET-09 · 10), doc 인터페이스
├─ adapter/out/persistence AcademyEntity · PickupScheduleEntity, 저장소
├─ adapter/out/map         KakaoMapSearchAdapter: 카카오 로컬 키워드 검색(RestClient), 키는 환경 변수 KAKAO_REST_API_KEY, 연결 2s · 읽기 3s
├─ adapter/out/auth        LoadMemberDeviceStatusPort 구현 → auth in 포트
├─ adapter/out/place       LoadPickupSpotPort 구현 → place in 포트 LoadPickupSpotUseCase.findSpots
├─ adapter/out/pickup      LoadPickupProgressPort 구현 → pickup in 포트 PickupProgressQueryUseCase(SET-12, 사람 결정 2026-11-14)
├─ application/port/in · out · service
└─ domain                  Academy, PickupSchedule, ScheduleStatus, JoinCallMode · DeviceStatus(schedule 자체 enum), InvalidScheduleException, 겹침 · 하루 한도 규칙, 이름 마스킹
```

## 테이블

학원 (Academy)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| academyId | BIGINT | PK | |
| familyId | BIGINT | NOT NULL, UNIQUE(familyId, externalPlaceId) | 학원을 등록한 가족. 학원은 가족마다 따로 둔다(사람 결정 2026-11-13) |
| externalPlaceId | VARCHAR(100) | NOT NULL, UNIQUE(familyId, externalPlaceId) | 외부 지도 장소 id(예: kakao:12345). 같은 가족 안에서만 같은 학원을 다시 쓴다 |
| name | VARCHAR(100) | | |
| address | VARCHAR(255) | | |
| location | geography(Point,4326) | GIST | 학원 위치 |
| entrance | geography(Point,4326) | NULL | 출입구. 없으면 일정이 DRAFT |
| phone | VARCHAR(20) | NULL | 대표번호 |

픽업 일정 (PickupSchedule)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| scheduleId | BIGINT | PK | |
| childUserId | BIGINT | | User의 userId, role = CHILD. DB FK 제약 없음 |
| academyId | BIGINT | | Academy의 academyId. DB FK 제약 없음 |
| weekdays | VARCHAR(30) | | 예: MON,WED,FRI |
| classEndTime | TIME | | 한국 시간 |
| targetPickup | SMALLINT | | 수업 종료 후 몇 분 뒤(0 = 종료 즉시, 5 = 5분 후, 직접 = 0~60) |
| status | VARCHAR(10) | | READY · DRAFT |

- childUserId · academyId는 DB FK 제약 없이 컬럼만 둔다(user · auth와 같은 관례).

## API 명세

| ID | Method · 경로 | 호출 | Idempotency-Key | 요청 → 응답 | 오류 |
|---|---|---|---|---|---|
| SET-01 | GET /v1/places/academies | GUARDIAN · CO_GUARDIAN | 불필요 | query(2~50자, 필수) → 200 items[](externalPlaceId · name · address · location{lat, lng}) | 502 MAP_API_UNAVAILABLE |
| SET-02 | POST /v1/children/{childId}/schedules | GUARDIAN · CO_GUARDIAN | 필요 | academy{externalPlaceId(1~100자), name, address, location, entrance(선택), phone(선택)}, weekdays[](MON~SUN, 1개 이상), classEndTime(HH:MM), targetPickup(0~60) → 201 scheduleId, academyId, status | 404 NOT_FOUND(내 가족 CHILD 아님), 422 VALIDATION_FAILED, 409 SCHEDULE_OVERLAP(overlapScheduleIds 함께), 422 DAILY_SCHEDULE_LIMIT |
| SET-03 | PUT /v1/schedules/{scheduleId} | GUARDIAN · CO_GUARDIAN | 불필요 | SET-02와 같음(externalPlaceId 1~100자) → 200 scheduleId, academyId, status | 404 NOT_FOUND(일정이 없거나 내 가족 자녀 일정 아님), 422 VALIDATION_FAILED, 409 SCHEDULE_OVERLAP, 422 DAILY_SCHEDULE_LIMIT |
| SET-04 | GET /v1/children/{childId}/schedules | GUARDIAN · CO_GUARDIAN | 불필요 | — → 200 items[](scheduleId · academy{academyId, name} · weekdays · classEndTime · targetPickup · status) | 404 NOT_FOUND(내 가족 CHILD 아님) |
| SET-07 | GET /v1/children/{childId}/setup-summary | GUARDIAN · CO_GUARDIAN | 불필요 | — → 200 child{userId, name, deviceStatus}, schedules[](scheduleId · academyName · status), pickupSpots[](spotId · name · isDefault), joinCallMode, missing[](CHILD_DEVICE · SCHEDULE · PICKUP_SPOT), settingsVersion | 404 NOT_FOUND(내 가족 CHILD 아님) |
| SET-08 | POST /v1/children/{childId}/setup-completion | GUARDIAN · CO_GUARDIAN | 필요 | settingsVersion → 200 syncedToVehicle | 404 NOT_FOUND(내 가족 CHILD 아님), 409 SETTINGS_VERSION_CONFLICT, 422 SETUP_INCOMPLETE |
| SET-09 | GET /v1/vehicles/me/weekly-pickups | 차량 앱 | 불필요 | weekStart(선택, 그 주 월요일) → 200 weekStart, days[](date · pickups[](scheduleId · childName · profileImageUrl · academyName · classEndTime · targetPickupTime)), readiness{startable, missing[](CHILD_DEVICE · SCHEDULE · PICKUP_SPOT)} | 400 INVALID_REQUEST(weekStart 형식 오류) |
| SET-10 | GET /v1/vehicles/me/settings-summary | 차량 앱 | 불필요 | — → 200 guardianNameMasked, children[](userId · name), joinCallMode | 공통 |
| SET-11 | GET /v1/children/me/next-pickup | CHILD | 불필요 | — → 200 academyName, spotName(그 학원의 가족 기본 장소 이름, 없으면 null), targetAt(KST, 오프셋 +09:00). 지금 이후 가장 가까운 READY 일정의 픽업. 남은 일정이 없으면 200과 null 본문 | 403 FORBIDDEN(CHILD 아님) |
| SET-12 | GET /v1/families/me/today-pickups | GUARDIAN · CO_GUARDIAN | 불필요 | — → 200 items[](scheduleId · childName · academyName · targetAt(KST +09:00) · status(SCHEDULED · IN_PROGRESS · COMPLETED)). 한국 날짜 오늘 요일이 맞는 가족 자녀들의 일정(DRAFT 포함), targetAt 오름차순(같으면 scheduleId). status는 오늘 그 일정이 들어간 픽업 세션 상태로 정함: ACTIVE · ON_HOLD → IN_PROGRESS, COMPLETED → COMPLETED, 세션 없음 · CANCELLED만 → SCHEDULED(여러 세션이면 COMPLETED > IN_PROGRESS > SCHEDULED). 세션 상태는 out 포트 LoadPickupProgressPort → pickup in 포트(사람 결정 2026-11-14: schedule → pickup 허용) | 403 FORBIDDEN(GUARDIAN · CO_GUARDIAN 아님) |

응답 예시
- SET-01: `{"items":[{"externalPlaceId":"kakao:12345","name":"해솔영어학원","address":"서울 강남구 …","location":{"lat":37.501234,"lng":127.039876}}]}`
- SET-04: `{"items":[{"scheduleId":801,"academy":{"academyId":55,"name":"해솔영어학원"},"weekdays":["MON","WED"],"classEndTime":"18:00","targetPickup":5,"status":"READY"}]}`
- SET-07: `{"child":{"userId":131,"name":"민준","deviceStatus":"CONNECTED"},"schedules":[{"scheduleId":801,"academyName":"해솔영어학원","status":"READY"}],"pickupSpots":[{"spotId":21,"name":"정문 앞","isDefault":true}],"joinCallMode":"AUTO","missing":[],"settingsVersion":8}`
- SET-09: `{"weekStart":"2026-11-02","days":[{"date":"2026-11-05","pickups":[{"scheduleId":801,"childName":"민준","profileImageUrl":"https://…","academyName":"해솔영어학원","classEndTime":"18:00","targetPickupTime":"18:05"}]}],"readiness":{"startable":true,"missing":[]}}`
- SET-11: `{"academyName":"해솔영어학원","spotName":"정문 앞","targetAt":"2026-11-16T18:05:00+09:00"}`
- SET-12: `{"items":[{"scheduleId":801,"childName":"민준","academyName":"해솔영어학원","targetAt":"2026-11-16T18:05:00+09:00","status":"SCHEDULED"}]}`
- SET-10: `{"guardianNameMasked":"김*호","children":[{"userId":131,"name":"민준"}],"joinCallMode":"AUTO"}`

## 핵심 로직

### 학원 검색 (SET-01)
- 외부 지도 API를 RestClient로 부르고 결과를 저장하지 않고 그대로 중계한다. 결과가 없으면 빈 items.
- 502 MAP_API_UNAVAILABLE: 외부 API 4xx · 5xx · 시간 초과, 200이어도 본문을 못 읽거나 좌표가 없거나 숫자가 아닐 때.
- 학원(Academy)은 SET-02 · 03으로 일정을 저장할 때 만들거나 다시 쓴다(아래).

### 일정 등록 · 수정 (SET-02 · 03)
- childId는 내 가족의 CHILD여야 한다(아니면 404 NOT_FOUND). SET-03은 scheduleId가 없거나 내 가족 자녀 일정이 아니면 404 NOT_FOUND.
- 저장한 학원(요청 entrance가 비면 기존 값을 유지한 학원)에 출입구가 없으면 status DRAFT(픽업 시작 불가), 있으면 READY. 등록 · 수정 모두 저장한 학원 기준으로 판정한다.
- weekdays는 MON~SUN 1개 이상, classEndTime은 한국 시간 HH:MM, targetPickup은 0~60. weekdays 중복(["MON","MON"])은 하나로 합친다. classEndTime 형식 오류는 422.
- 검증 순서와 오류

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | 요청 형식 검증(웹 @Valid, 유스케이스 호출 전) | 422 VALIDATION_FAILED(fields) |
| 2 | 자녀 확인(SET-03은 일정 확인 포함) | 404 NOT_FOUND |
| 3 | 도메인 검증(InvalidScheduleException) | 422 VALIDATION_FAILED(fields) |
| 4 | 겹침 | 409 SCHEDULE_OVERLAP, overlapScheduleIds |
| 5 | 하루 한도 | 422 DAILY_SCHEDULE_LIMIT |

- 겹침과 한도가 둘 다면 겹침(409)이 먼저다.
- 겹침의 현재 가정: 같은 자녀 · 같은 요일 · 같은 classEndTime.
- 하루 한도의 현재 가정: 자녀 1명의 같은 요일 기준. 새 일정의 요일마다 세고 한 요일이라도 이미 3개 이상이면 초과.
- 수정 때 겹침 · 한도는 자기 자신을 뺀 목록으로 판정한다(서비스가 뺀다).
- 도메인 검증 실패는 도메인 전용 예외 InvalidScheduleException(field, reason)으로 던지고 서비스가 422 VALIDATION_FAILED(fields)로 바꾼다. 도메인은 Spring · common.error를 쓰지 않는다.
- SET-03은 진행 중인 세션에도 바로 적용된다. pickup은 세션 시작 때 일정 값을 스냅샷하지 않고(SessionChild에는 scheduleId만 둠) 판정 때마다 최신 값을 읽는다(사람 결정 2026-11-13).

학원 다시 쓰기(같은 가족 · externalPlaceId 기준. 다른 가족의 학원은 찾지도 바꾸지도 않는다)

| 경우 | 동작 |
|---|---|
| 같은 가족에 같은 externalPlaceId 학원이 있음 | 그 academyId를 쓴다. name · address · location은 요청 값으로 갱신, entrance · phone은 요청 값이 비어 있으면(null) 기존 값 유지 |
| 없음 | 새로 만든다 |
| 같은 가족의 같은 새 학원이 동시에 저장되어 유니크가 부딪힘 | 오류 없이 저장된 학원을 쓴다(INSERT … ON CONFLICT (family_id, external_place_id), 위와 같은 갱신 규칙) |

### 일정 목록 (SET-04)
- scheduleId 오름차순, weekdays는 월요일부터.
- 내 가족 CHILD가 아니면 404 NOT_FOUND.

### 설정 요약 · 완료 (SET-07 · 08)
- missing 판정(현재 가정): CHILD_DEVICE = 자녀 deviceStatus가 CONNECTED가 아님, SCHEDULE = READY 일정이 없음, PICKUP_SPOT = 일정의 학원 중 가족의 기본 장소가 없는 학원이 있음.
- PICKUP_SPOT 판정은 모든 일정의 학원이 대상이다. 일정이 하나도 없으면 missing에는 SCHEDULE만 들어간다(PICKUP_SPOT 없음).
- 기기 상태가 없으면 NONE으로 본다.
- pickupSpots는 그 자녀 일정의 학원들에 저장된 가족 장소다(place in 포트).
- SET-08: 검사 순서는 버전 비교 → missing.
  - 보낸 settingsVersion이 Family.settingsVersion과 다르면 409 SETTINGS_VERSION_CONFLICT(SET-07을 다시 부름).
  - missing이 남아 있으면 422 SETUP_INCOMPLETE.
  - 성공하면 syncedToVehicle true를 돌려준다. 완료 기록은 아직 저장하지 않는다(후속 과제).

### 차량용 (SET-09 · 10)
- 차량 토큰의 familyId 가족 자녀들의 일정을 보여 준다.
- weekStart가 없으면 이번 주 월요일(한국 날짜). weekStart 형식 오류는 400 INVALID_REQUEST.
- days는 7일, 하루 최대 3개. 하루 안은 classEndTime 오름차순. DRAFT 일정도 days에 보여 준다.
- targetPickupTime = classEndTime + targetPickup분(예: 18:00 + 5 = 18:05).
- readiness: 오늘 픽업을 시작할 수 있는지와 빠진 항목(CHILD_DEVICE · SCHEDULE · PICKUP_SPOT, SES-02와 같은 순서). 보호자가 설정을 끝내기 전에는 차량에서 픽업을 시작할 수 없다.
- readiness 판정 대상은 한국 날짜 오늘 일정이 있는 자녀 · 학원이다.

| readiness 경우 | startable | missing |
|---|---|---|
| 오늘 일정이 없음 | false | 빈 목록 |
| 오늘 일정 중 DRAFT가 있음 | false | SCHEDULE을 넣음(SES-02와 같은 순서 CHILD_DEVICE · SCHEDULE · PICKUP_SPOT, D10 사람 결정 2026-11-13) |
| 자녀 기기 · 기본 장소가 빠짐 | false | CHILD_DEVICE · PICKUP_SPOT |

- SET-10 guardianNameMasked는 GUARDIAN 이름의 가운데 글자를 가린다(김보호 → 김*호). GUARDIAN이 없으면 null.

### 시그니처

```java
// in 포트 — web
List<AcademyCandidate> searchAcademies(String query);                    // SearchAcademyUseCase
ScheduleResult createSchedule(CreateScheduleCommand c);                  // CreateScheduleUseCase
ScheduleResult updateSchedule(UpdateScheduleCommand c);                  // UpdateScheduleUseCase
List<ScheduleView> listSchedules(ChildQuery q);                          // ListSchedulesUseCase
SetupSummary getSetupSummary(ChildQuery q);                              // GetSetupSummaryUseCase
SetupCompletionResult completeSetup(CompleteSetupCommand c);             // CompleteSetupUseCase
WeeklyPickups getWeeklyPickups(long familyId, LocalDate weekStart);      // GetWeeklyPickupsUseCase
VehicleSettingsSummary getVehicleSettingsSummary(long familyId);         // GetVehicleSettingsSummaryUseCase
Optional<NextPickup> getNextPickup(long familyId, long childUserId);     // GetNextPickupUseCase (SET-11). READY만, 한국 날짜 오늘~오늘+7, targetAt이 지금보다 엄격히 뒤인 가장 이른 것

// in 포트 — 다른 모듈용
List<ScheduleInfo> findSchedules(List<Long> scheduleIds);                // LoadScheduleUseCase (pickup SES-02). loadSchedules를 1회 부름
Optional<AcademyInfo> findAcademy(long academyId);                       // LoadAcademyUseCase (place 500m 검사, pickup 연락처)

// out 포트
List<AcademyCandidate> search(String query);                             // SearchAcademyPort (외부 지도 API)
Optional<Academy> loadAcademy(long academyId);                           // LoadAcademyPort
Optional<Academy> loadAcademyByExternalPlaceId(long familyId, String externalPlaceId);  // LoadAcademyPort (가족마다 따로)
Academy saveAcademy(Academy a);                                          // SaveAcademyPort
Optional<PickupSchedule> loadSchedule(long scheduleId);                  // LoadSchedulePort
List<PickupSchedule> loadSchedules(List<Long> scheduleIds);              // LoadSchedulePort (findSchedules가 1회 사용)
List<PickupSchedule> loadSchedulesOfChild(long childUserId);             // LoadSchedulePort
List<PickupSchedule> loadSchedulesOfChildren(List<Long> childUserIds);   // LoadSchedulePort
PickupSchedule saveSchedule(PickupSchedule s);                           // SaveSchedulePort
List<SpotSummary> loadSpots(long familyId, List<Long> academyIds);       // LoadPickupSpotPort → place
Map<Long, DeviceStatus> loadDeviceStatuses(List<Long> userIds);          // LoadMemberDeviceStatusPort → auth
Map<Long, TodayPickupStatus> loadProgress(long familyId, LocalDate date); // LoadPickupProgressPort → pickup PickupProgressQueryUseCase (SET-12)
List<TodayPickup> getTodayPickups(long familyId);                       // GetTodayPickupsUseCase (SET-12)

// domain
InvalidScheduleException(String field, String reason)                   // 도메인 검증 실패. Spring · common.error 안 씀, 서비스가 422 VALIDATION_FAILED(fields)로 바꿈
// JoinCallMode · DeviceStatus: schedule 자체 enum(user.domain 타입을 import하지 않음)
```

## 테스트 케이스
- [x] [기반] PickupSchedule은 학원 출입구가 없으면 DRAFT, 있으면 READY로 만들어진다
- [x] [기반] 일정 규칙은 targetPickup이 0~60을 벗어나면 거절한다
- [x] [기반] 일정 규칙은 weekdays가 비어 있으면 거절한다
- [x] [기반] 겹침 규칙은 같은 자녀 · 같은 요일 · 같은 classEndTime 일정을 겹침으로 판정한다
- [x] [기반] 하루 한도 규칙은 같은 자녀 같은 요일에 네 번째 일정이면 한도 초과로 판정한다
- [x] [기반] targetPickupTime 계산은 classEndTime 18:00 · targetPickup 5에서 18:05를 돌려준다
- [x] [기반] 이름 마스킹은 김보호를 김*호로 바꾼다
- [x] [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] [기반] AcademyEntity · PickupScheduleEntity가 BaseTimeEntity를 상속하고 location · entrance는 geography(Point,4326)로 매핑된다
- [x] [기반] 테스트 시드로 학원 1곳(해솔영어학원, 출입구 있음)과 자녀 민준의 월 · 수 18:00 일정 1개를 만든다
- [x] [persistence] Academy를 저장하고 읽으면 location · entrance 좌표가 같다
- [x] [persistence] weekdays [MON, WED, FRI]를 저장하면 컬럼에 MON,WED,FRI로 들어가고 같은 목록으로 읽힌다
- [x] [persistence] classEndTime 18:00을 저장하면 시간대 변환 없이 18:00으로 읽힌다
- [x] [persistence] loadSchedulesOfChild는 그 자녀의 일정만 돌려준다
- [x] [persistence] loadSchedulesOfChildren은 여러 자녀의 일정을 한 번에 돌려준다
- [x] [서비스] 학원 검색은 외부 지도 API 결과를 저장하지 않고 externalPlaceId · name · address · location으로 돌려준다
- [x] [서비스] 학원 검색은 외부 지도 API가 실패하면 MAP_API_UNAVAILABLE로 거절한다
- [x] [서비스] 일정 등록은 학원을 만들고 scheduleId · academyId · status를 돌려준다
- [x] [서비스] 일정 등록은 출입구 없는 학원이면 status DRAFT를 돌려준다
- [x] [서비스] 일정 등록은 childId가 CHILD가 아니면 NOT_FOUND로 거절한다
- [x] [서비스] 일정 등록은 다른 가족의 자녀면 NOT_FOUND로 거절한다
- [x] [서비스] 일정 등록은 겹치는 일정이 있으면 SCHEDULE_OVERLAP과 겹치는 scheduleId 목록으로 거절한다
- [x] [서비스] 일정 등록은 같은 요일 일정이 이미 3개면 DAILY_SCHEDULE_LIMIT으로 거절한다
- [x] [서비스] 일정 수정은 자기 자신과는 겹침으로 판정하지 않는다
- [x] [서비스] 일정 수정은 요일 · 시간 · 학원을 바꾸고 status를 다시 판정한다
- [x] [서비스] 설정 요약은 자녀 기기가 CONNECTED가 아니면 missing에 CHILD_DEVICE를 넣는다
- [x] [서비스] 설정 요약은 READY 일정이 없으면 missing에 SCHEDULE을 넣는다
- [x] [서비스] 설정 요약은 일정 학원에 기본 장소가 없으면 missing에 PICKUP_SPOT을 넣는다
- [x] [서비스] 설정 요약은 joinCallMode와 Family.settingsVersion을 담는다
- [x] [서비스] 설정 완료는 settingsVersion이 다르면 SETTINGS_VERSION_CONFLICT로 거절한다
- [x] [서비스] 설정 완료는 missing이 남아 있으면 SETUP_INCOMPLETE로 거절한다
- [x] [서비스] 주간 일정은 weekStart가 없으면 한국 날짜 기준 이번 주 월요일부터 7일을 돌려준다
- [x] [서비스] 주간 일정은 요일이 맞는 날짜에만 그 일정을 넣는다
- [x] [서비스] 주간 일정 readiness는 자녀 기기나 기본 장소가 빠지면 startable false와 missing을 돌려준다
- [x] [서비스] 차량용 설정 요약은 GUARDIAN 이름을 가려서 돌려주고 자녀 목록과 joinCallMode를 담는다
- [x] [서비스] findSchedules는 scheduleId 목록의 일정과 학원 정보를 돌려준다
- [x] [web] GET /v1/places/academies는 200과 items[]를 돌려준다
- [x] [web] GET /v1/places/academies에 1자 query를 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] [web] GET /v1/places/academies가 외부 API 오류면 502 MAP_API_UNAVAILABLE을 돌려준다
- [x] [web] POST /v1/children/{childId}/schedules는 201과 scheduleId · academyId · status를 돌려준다
- [x] [web] POST /v1/children/{childId}/schedules에 targetPickup 61을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] [web] POST /v1/children/{childId}/schedules가 겹치면 409 SCHEDULE_OVERLAP과 overlapScheduleIds를 돌려준다
- [x] [web] POST /v1/children/{childId}/schedules가 하루 한도를 넘으면 422 DAILY_SCHEDULE_LIMIT을 돌려준다
- [x] [web] PUT /v1/schedules/{scheduleId}는 200과 scheduleId · academyId · status를 돌려준다
- [x] [web] PUT /v1/schedules/{scheduleId}를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] [web] GET /v1/children/{childId}/schedules는 200과 items[]를 돌려준다
- [x] [web] GET /v1/children/{childId}/setup-summary는 200과 child · schedules · pickupSpots · joinCallMode · missing · settingsVersion을 돌려준다
- [x] [web] POST /v1/children/{childId}/setup-completion은 200과 syncedToVehicle을 돌려준다
- [x] [web] POST /v1/children/{childId}/setup-completion이 버전 충돌이면 409 SETTINGS_VERSION_CONFLICT를 돌려준다
- [x] [web] POST /v1/children/{childId}/setup-completion이 누락이 있으면 422 SETUP_INCOMPLETE를 돌려준다
- [x] [web] GET /v1/vehicles/me/weekly-pickups는 차량 토큰으로 200과 weekStart · days · readiness를 돌려준다
- [x] [web] GET /v1/vehicles/me/weekly-pickups를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] [web] GET /v1/vehicles/me/settings-summary는 차량 토큰으로 200과 guardianNameMasked · children · joinCallMode를 돌려준다
- [x] [web] Idempotency-Key는 POST /v1/children/{childId}/schedules · POST /v1/children/{childId}/setup-completion에만 요구된다
- [x] [persistence] 지도 API 어댑터는 카카오 로컬 검색 응답을 externalPlaceId(kakao:{id}) · name · address · location의 AcademyCandidate로 바꾼다
- [x] [persistence] 지도 API 어댑터는 외부 API 오류 · 시간 초과면 502 MAP_API_UNAVAILABLE로 거절한다
- [x] [persistence] LoadPickupSpotPort 어댑터는 place LoadPickupSpotUseCase.findSpots로 읽은 가족의 실제 장소 목록을 돌려준다(사람 결정 2026-11-13)
- [x] [persistence] LoadMemberDeviceStatusPort 어댑터는 auth의 멤버 기기 상태를 schedule DeviceStatus로 바꿔 돌려준다
- [x] [기반] 공통 BusinessException에 overlapScheduleIds를 담아 던지면 오류 응답 error.overlapScheduleIds에 실리고, 담지 않으면 그 키가 응답에 없다
- [x] [서비스] findAcademy는 학원 id의 이름 · 주소 · 위치 · 출입구 · 대표번호를 돌려주고 없으면 빈 값을 돌려준다
- [x] 지도 API 어댑터는 200 응답이라도 본문을 읽지 못하거나 좌표가 없거나 숫자가 아니면 502 MAP_API_UNAVAILABLE로 거절한다
- [x] saveSchedule로 scheduleId가 있는 일정을 다시 저장하면 새 행을 만들지 않고 그 행의 요일 · 시간 · 학원 · status가 바뀐다
- [x] loadSchedule · loadAcademy에 없는 id를 주면 빈 값을 돌려준다
- [x] [서비스] 일정 목록은 그 자녀의 일정을 scheduleId · 학원(academyId · name) · weekdays · classEndTime · targetPickup · status로 돌려주고, 내 가족의 CHILD가 아니면 NOT_FOUND로 거절한다
- [x] PickupSchedule 검증 실패는 프레임워크와 무관한 도메인 예외(field · reason)로 던지고, 일정 등록 · 수정 서비스는 이를 422 VALIDATION_FAILED(fields)로 바꾼다
- [x] Academy는 externalPlaceId를 가지며 academy.external_place_id는 유니크로 저장되고, 저장한 학원을 externalPlaceId로 찾을 수 있다
- [x] 일정 등록 · 수정은 같은 externalPlaceId의 학원이 있으면 새로 만들지 않고 그 academyId를 쓰며 학원 정보는 요청 값으로 갱신한다
- [x] 주간 일정 readiness는 오늘 일정 중 DRAFT가 있으면 startable false를 돌려준다
- [x] 일정 등록 · 수정은 같은 학원을 다시 쓸 때 요청의 entrance · phone이 비어 있으면 기존 값을 유지한다
- [x] 같은 externalPlaceId의 새 학원이 동시에 저장되어 유니크가 부딪히면 오류 없이 이미 저장된 그 학원을 다시 읽어 쓴다
- [x] 일정 수정은 자기 자신을 뺀 같은 요일 일정이 이미 3개면 DAILY_SCHEDULE_LIMIT으로 거절한다
- [x] loadSchedules는 여러 scheduleId의 일정을 학원과 함께 한 번에 읽어 돌려준다(일정 조회 1회 · 학원 조회 1회)
- [x] findSchedules는 loadSchedules를 한 번만 불러 받은 순서대로 돌려주고 없는 id는 뺀다
- [x] joinCallMode는 schedule 자체 enum으로 설정 요약 · 차량용 설정 요약 in 포트와 응답에 담고 user.domain 타입을 import하지 않는다
- [x] POST /v1/children/{childId}/schedules · PUT /v1/schedules/{scheduleId}에 101자 externalPlaceId를 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] 일정 수정은 등록과 같이 저장한 학원(기존 출입구 반영) 기준으로 status를 판정한다
- [x] [서비스] SET-09 readiness는 오늘 일정이 DRAFT면 startable false와 missing에 SCHEDULE을 넣어 SES-02 픽업 시작 판정과 같은 값을 준다(D10, 사람 결정 2026-11-13)
- [x] [web] GET /v1/children/me/next-pickup(CHILD 전용)은 지금 이후 가장 가까운 픽업의 academyName · spotName · targetAt(KST)을 돌려주고, 남은 일정이 없으면 200과 null 본문을 준다(최종디자인 11/13 C01 실내 대기)
- [x] [web] GET /v1/vehicles/me/weekly-pickups의 각 픽업에 자녀 profileImageUrl을 담는다(V01 자녀 카드)
- [x] [web] GET /v1/families/me/today-pickups(GUARDIAN · CO_GUARDIAN)는 오늘 가족 자녀들의 픽업을 시각 순으로 childName · academyName · targetAt · status(SCHEDULED · IN_PROGRESS · COMPLETED)로 돌려준다(부모 메인 오늘의 픽업 타임라인)
- [x] [persistence] Academy는 familyId를 갖고 (familyId, externalPlaceId)가 유니크다. 두 가족이 같은 externalPlaceId로 일정을 등록하면 학원 행이 가족마다 따로 생긴다(사람 결정 2026-11-13: 학원은 가족마다 따로 둠)
- [x] [서비스] 일정 등록 · 수정의 학원 다시 쓰기는 같은 가족의 학원만 갱신하고, 다른 가족 학원의 이름 · 주소 · 위치 · 출입구 · 대표번호를 바꾸지 않는다

## 확인 필요 사항

| 항목 | 현재 가정 |
|---|---|
| Academy에 externalPlaceId 칸이 없음 | 해소: externalPlaceId 컬럼(UNIQUE)으로 같은 학원을 다시 씀(가족 간 공유). 핵심 로직 「학원 다시 쓰기」 |
| 학원을 가족 간에 공유할지 | 결정됨: 학원은 가족마다 따로 둔다. Academy에 familyId를 더하고 (familyId, externalPlaceId)를 유니크로 한다. 테스트 케이스 끝의 두 항목으로 구현(사람 결정 2026-11-13) |
| 이미 있는 학원 갱신의 동시 수정 | 후속 과제. 이미 있는 학원 갱신은 JPA merge라 동시 수정 때 다른 가족이 막 넣은 entrance를 지울 수 있음 |
| 일정 수정 때 학원 정보가 바뀐 경우 | 해소: 같은 externalPlaceId면 그 행을 갱신, 다르면 그 학원을 찾거나 새로 만듦 |
| 「겹침」의 정의 | 소스는 「같은 아이의 겹치는 일정」만 있음. 같은 자녀 · 같은 요일 · 같은 classEndTime으로 가정 |
| 「하루 3개」의 기준 | 자녀별인지 가족 · 차량별인지 소스에 없음. 자녀별 같은 요일로 가정 |
| 4.5 표의 「목표 < 종료 422」 | 4.14는 targetPickup을 종료 후 0~60분으로 정해 목표가 종료보다 앞설 수 없음. 0~60 범위 검사로 대신함 |
| missing 판정 규칙 | 소스는 값 이름만 있음. 핵심 로직의 현재 가정으로 판정 |
| SET-08 「차량 동기화」의 동작과 syncedToVehicle 값 | 후속 과제로 둠. 지금은 추가 저장 없이 true를 돌려줌. 완료 기록 저장 · readiness 반영은 user · pickup 설계와 함께 정함 |
| DRAFT 일정을 SET-09에 보여 줄지 | 해소: days에 보여 주고, 오늘 일정 중 DRAFT가 있으면 readiness startable false |
| 이름 마스킹 규칙(2자 · 4자 이상) | 예시 「김*호」뿐. 첫 글자와 마지막 글자만 남기고 가운데를 *로 바꾼다고 가정(2자는 두 번째 글자를 가림) |
| 외부 지도 API 공급자 · 키 | 해소: 카카오 로컬 키워드 검색(KakaoMapSearchAdapter), 키는 환경 변수 KAKAO_REST_API_KEY, 연결 2s · 읽기 3s(구조 절) |
| 일정 변경이 settingsVersion을 올리는지 | 04_사용자-user.md와 같은 질문. 올리지 않는다고 가정 |
| SET-05 | API 번호가 SET-04 다음 SET-06으로 건너뜀. 빠진 API가 없는지 확인 필요 |
| schedule ↔ place 서로 부름 | 해소: place는 Academy를 schedule in 포트 LoadAcademyUseCase.findAcademy로 읽고, SET-07 · 09는 adapter/out/place가 place in 포트 LoadPickupSpotUseCase.findSpots로 실제 장소를 읽음(임시 어댑터는 연동으로 바뀜) |
