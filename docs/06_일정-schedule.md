# 06. 일정 (schedule)

학원 · 일정과 설정 요약 · 완료, 차량용 주간 일정 · 설정 요약. 자녀는 role = CHILD인 User이고 경로의 {childId}는 그 userId다.

| 항목 | 내용 |
|---|---|
| 범위 | 엔티티 Academy · PickupSchedule. API SET-01~04, SET-07~10. 다른 모듈용 in 포트(일정 조회 · 학원 조회) |
| 의존 | 03_공유모듈-shared, 04_사용자-user(자녀 · 가족 · 합류 안내 방식 · settingsVersion · 보호자 이름), 05_인증-auth(자녀 기기 상태). place의 in 포트(장소 목록 · 기본 장소 유무)는 out 포트 + 테스트 대역 |
| 진행 | 계층 병렬 |

## 구조

```text
schedule
├─ adapter/in/web          ScheduleController(SET-01~04), SetupController(SET-07 · 08), VehicleScheduleController(SET-09 · 10), doc 인터페이스
├─ adapter/out/persistence AcademyEntity · PickupScheduleEntity, 저장소
├─ adapter/out/map         외부 지도 API 호출(RestClient)
├─ application/port/in · out · service
└─ domain                  Academy, PickupSchedule, ScheduleStatus, 겹침 · 하루 한도 규칙, 이름 마스킹
```

## 테이블

학원 (Academy)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| academyId | BIGINT | PK | |
| name | VARCHAR(100) | | |
| address | VARCHAR(255) | | |
| location | geography(Point,4326) | GIST | 학원 위치 |
| entrance | geography(Point,4326) | NULL | 출입구. 없으면 일정이 DRAFT |
| phone | VARCHAR(20) | NULL | 대표번호 |

픽업 일정 (PickupSchedule)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| scheduleId | BIGINT | PK | |
| childUserId | BIGINT | FK → User | role = CHILD |
| academyId | BIGINT | FK → Academy | |
| weekdays | VARCHAR(30) | | 예: MON,WED,FRI |
| classEndTime | TIME | | 한국 시간 |
| targetPickup | SMALLINT | | 수업 종료 후 몇 분 뒤(0 = 종료 즉시, 5 = 5분 후, 직접 = 0~60) |
| status | VARCHAR(10) | | READY · DRAFT |

## API 명세

| ID | Method · 경로 | 호출 | Idempotency-Key | 요청 → 응답 | 오류 |
|---|---|---|---|---|---|
| SET-01 | GET /v1/places/academies | GUARDIAN · CO_GUARDIAN | 불필요 | query(2~50자, 필수) → 200 items[](externalPlaceId · name · address · location{lat, lng}) | 502 MAP_API_UNAVAILABLE |
| SET-02 | POST /v1/children/{childId}/schedules | GUARDIAN · CO_GUARDIAN | 필요 | academy{externalPlaceId, name, address, location, entrance(선택), phone(선택)}, weekdays[](MON~SUN, 1개 이상), classEndTime(HH:MM), targetPickup(0~60) → 201 scheduleId, academyId, status | 409 SCHEDULE_OVERLAP(overlapScheduleIds 함께), 422 DAILY_SCHEDULE_LIMIT |
| SET-03 | PUT /v1/schedules/{scheduleId} | GUARDIAN · CO_GUARDIAN | 불필요 | SET-02와 같음 → 200 scheduleId, academyId, status | 409 SCHEDULE_OVERLAP, 422 DAILY_SCHEDULE_LIMIT |
| SET-04 | GET /v1/children/{childId}/schedules | GUARDIAN · CO_GUARDIAN | 불필요 | — → 200 items[](scheduleId · academy{academyId, name} · weekdays · classEndTime · targetPickup · status) | 공통 |
| SET-07 | GET /v1/children/{childId}/setup-summary | GUARDIAN · CO_GUARDIAN | 불필요 | — → 200 child{userId, name, deviceStatus}, schedules[](scheduleId · academyName · status), pickupSpots[](spotId · name · isDefault), joinCallMode, missing[](CHILD_DEVICE · SCHEDULE · PICKUP_SPOT), settingsVersion | 공통 |
| SET-08 | POST /v1/children/{childId}/setup-completion | GUARDIAN · CO_GUARDIAN | 필요 | settingsVersion → 200 syncedToVehicle | 409 SETTINGS_VERSION_CONFLICT, 422 SETUP_INCOMPLETE |
| SET-09 | GET /v1/vehicles/me/weekly-pickups | 차량 앱 | 불필요 | weekStart(선택, 그 주 월요일) → 200 weekStart, days[](date · pickups[](scheduleId · childName · academyName · classEndTime · targetPickupTime)), readiness{startable, missing[](CHILD_DEVICE · PICKUP_SPOT)} | 공통 |
| SET-10 | GET /v1/vehicles/me/settings-summary | 차량 앱 | 불필요 | — → 200 guardianNameMasked, children[](userId · name), joinCallMode | 공통 |

응답 예시
- SET-01: `{"items":[{"externalPlaceId":"kakao:12345","name":"해솔영어학원","address":"서울 강남구 …","location":{"lat":37.501234,"lng":127.039876}}]}`
- SET-04: `{"items":[{"scheduleId":801,"academy":{"academyId":55,"name":"해솔영어학원"},"weekdays":["MON","WED"],"classEndTime":"18:00","targetPickup":5,"status":"READY"}]}`
- SET-07: `{"child":{"userId":131,"name":"민준","deviceStatus":"CONNECTED"},"schedules":[{"scheduleId":801,"academyName":"해솔영어학원","status":"READY"}],"pickupSpots":[{"spotId":21,"name":"정문 앞","isDefault":true}],"joinCallMode":"AUTO","missing":[],"settingsVersion":8}`
- SET-09: `{"weekStart":"2026-11-02","days":[{"date":"2026-11-05","pickups":[{"scheduleId":801,"childName":"민준","academyName":"해솔영어학원","classEndTime":"18:00","targetPickupTime":"18:05"}]}],"readiness":{"startable":true,"missing":[]}}`
- SET-10: `{"guardianNameMasked":"김*호","children":[{"userId":131,"name":"민준"}],"joinCallMode":"AUTO"}`

## 핵심 로직

### 학원 검색 (SET-01)
- 외부 지도 API를 RestClient로 부르고 결과를 저장하지 않고 그대로 중계한다. 외부 API 오류는 502 MAP_API_UNAVAILABLE.
- 학원(Academy)은 SET-02로 일정을 저장할 때 만들어진다.

### 일정 등록 · 수정 (SET-02 · 03)
- childId는 내 가족의 CHILD여야 한다(아니면 404 NOT_FOUND).
- academy.entrance가 없으면 status DRAFT(픽업 시작 불가), 있으면 READY.
- weekdays는 MON~SUN 1개 이상, classEndTime은 한국 시간 HH:MM, targetPickup은 0~60.
- 같은 아이의 겹치는 일정이 있으면 409 SCHEDULE_OVERLAP과 overlapScheduleIds. 겹침의 현재 가정: 같은 자녀 · 같은 요일 · 같은 classEndTime.
- 하루 일정이 3개를 넘으면 422 DAILY_SCHEDULE_LIMIT. 현재 가정: 자녀 1명의 같은 요일 기준.
- SET-03은 진행 중 세션에 적용되지 않고 다음 세션부터 적용된다(pickup이 세션 시작 때 스냅샷).

### 설정 요약 · 완료 (SET-07 · 08)
- missing 판정(현재 가정): CHILD_DEVICE = 자녀 deviceStatus가 CONNECTED가 아님, SCHEDULE = READY 일정이 없음, PICKUP_SPOT = 일정의 학원 중 가족의 기본 장소가 없는 학원이 있음.
- pickupSpots는 그 자녀 일정의 학원들에 저장된 가족 장소다(place in 포트).
- SET-08: 보낸 settingsVersion이 Family.settingsVersion과 다르면 409 SETTINGS_VERSION_CONFLICT(SET-07을 다시 부름). missing이 남아 있으면 422 SETUP_INCOMPLETE. 성공하면 syncedToVehicle을 돌려준다.

### 차량용 (SET-09 · 10)
- 차량 토큰의 familyId 가족 자녀들의 일정을 보여 준다.
- weekStart가 없으면 이번 주 월요일(한국 날짜). days는 7일, 하루 최대 3개.
- targetPickupTime = classEndTime + targetPickup분(예: 18:00 + 5 = 18:05).
- readiness: 오늘 픽업을 시작할 수 있는지와 빠진 항목(CHILD_DEVICE · PICKUP_SPOT). 보호자가 설정을 끝내기 전에는 차량에서 픽업을 시작할 수 없다.
- SET-10 guardianNameMasked는 GUARDIAN 이름의 가운데 글자를 가린다(김보호 → 김*호).

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

// in 포트 — 다른 모듈용
List<ScheduleInfo> findSchedules(List<Long> scheduleIds);                // LoadScheduleUseCase (pickup SES-02)
Optional<AcademyInfo> findAcademy(long academyId);                       // LoadAcademyUseCase (place 500m 검사, pickup 연락처)

// out 포트
List<AcademyCandidate> search(String query);                             // SearchAcademyPort (외부 지도 API)
Optional<Academy> loadAcademy(long academyId); Academy saveAcademy(Academy a);                         // LoadAcademyPort · SaveAcademyPort
Optional<PickupSchedule> loadSchedule(long scheduleId); List<PickupSchedule> loadSchedulesOfChild(long childUserId); List<PickupSchedule> loadSchedulesOfChildren(List<Long> childUserIds); PickupSchedule saveSchedule(PickupSchedule s);   // LoadSchedulePort · SaveSchedulePort
List<SpotSummary> loadSpots(long familyId, List<Long> academyIds);       // LoadPickupSpotPort → place
Map<Long, DeviceStatus> loadDeviceStatuses(List<Long> userIds);          // LoadMemberDeviceStatusPort → auth
```

## 테스트 케이스
- [ ] [기반] PickupSchedule은 학원 출입구가 없으면 DRAFT, 있으면 READY로 만들어진다
- [ ] [기반] 일정 규칙은 targetPickup이 0~60을 벗어나면 거절한다
- [ ] [기반] 일정 규칙은 weekdays가 비어 있으면 거절한다
- [ ] [기반] 겹침 규칙은 같은 자녀 · 같은 요일 · 같은 classEndTime 일정을 겹침으로 판정한다
- [ ] [기반] 하루 한도 규칙은 같은 자녀 같은 요일에 네 번째 일정이면 한도 초과로 판정한다
- [ ] [기반] targetPickupTime 계산은 classEndTime 18:00 · targetPickup 5에서 18:05를 돌려준다
- [ ] [기반] 이름 마스킹은 김보호를 김*호로 바꾼다
- [ ] [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [ ] [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [ ] [기반] AcademyEntity · PickupScheduleEntity가 BaseTimeEntity를 상속하고 location · entrance는 geography(Point,4326)로 매핑된다
- [ ] [기반] 테스트 시드로 학원 1곳(해솔영어학원, 출입구 있음)과 자녀 민준의 월 · 수 18:00 일정 1개를 만든다
- [ ] [persistence] Academy를 저장하고 읽으면 location · entrance 좌표가 같다
- [ ] [persistence] weekdays [MON, WED, FRI]를 저장하면 컬럼에 MON,WED,FRI로 들어가고 같은 목록으로 읽힌다
- [ ] [persistence] classEndTime 18:00을 저장하면 시간대 변환 없이 18:00으로 읽힌다
- [ ] [persistence] loadSchedulesOfChild는 그 자녀의 일정만 돌려준다
- [ ] [persistence] loadSchedulesOfChildren은 여러 자녀의 일정을 한 번에 돌려준다
- [ ] [서비스] 학원 검색은 외부 지도 API 결과를 저장하지 않고 externalPlaceId · name · address · location으로 돌려준다
- [ ] [서비스] 학원 검색은 외부 지도 API가 실패하면 MAP_API_UNAVAILABLE로 거절한다
- [ ] [서비스] 일정 등록은 학원을 만들고 scheduleId · academyId · status를 돌려준다
- [ ] [서비스] 일정 등록은 출입구 없는 학원이면 status DRAFT를 돌려준다
- [ ] [서비스] 일정 등록은 childId가 CHILD가 아니면 NOT_FOUND로 거절한다
- [ ] [서비스] 일정 등록은 다른 가족의 자녀면 NOT_FOUND로 거절한다
- [ ] [서비스] 일정 등록은 겹치는 일정이 있으면 SCHEDULE_OVERLAP과 겹치는 scheduleId 목록으로 거절한다
- [ ] [서비스] 일정 등록은 같은 요일 일정이 이미 3개면 DAILY_SCHEDULE_LIMIT으로 거절한다
- [ ] [서비스] 일정 수정은 자기 자신과는 겹침으로 판정하지 않는다
- [ ] [서비스] 일정 수정은 요일 · 시간 · 학원을 바꾸고 status를 다시 판정한다
- [ ] [서비스] 설정 요약은 자녀 기기가 CONNECTED가 아니면 missing에 CHILD_DEVICE를 넣는다
- [ ] [서비스] 설정 요약은 READY 일정이 없으면 missing에 SCHEDULE을 넣는다
- [ ] [서비스] 설정 요약은 일정 학원에 기본 장소가 없으면 missing에 PICKUP_SPOT을 넣는다
- [ ] [서비스] 설정 요약은 joinCallMode와 Family.settingsVersion을 담는다
- [ ] [서비스] 설정 완료는 settingsVersion이 다르면 SETTINGS_VERSION_CONFLICT로 거절한다
- [ ] [서비스] 설정 완료는 missing이 남아 있으면 SETUP_INCOMPLETE로 거절한다
- [ ] [서비스] 주간 일정은 weekStart가 없으면 한국 날짜 기준 이번 주 월요일부터 7일을 돌려준다
- [ ] [서비스] 주간 일정은 요일이 맞는 날짜에만 그 일정을 넣는다
- [ ] [서비스] 주간 일정 readiness는 자녀 기기나 기본 장소가 빠지면 startable false와 missing을 돌려준다
- [ ] [서비스] 차량용 설정 요약은 GUARDIAN 이름을 가려서 돌려주고 자녀 목록과 joinCallMode를 담는다
- [ ] [서비스] findSchedules는 scheduleId 목록의 일정과 학원 정보를 돌려준다
- [ ] [web] GET /v1/places/academies는 200과 items[]를 돌려준다
- [ ] [web] GET /v1/places/academies에 1자 query를 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] [web] GET /v1/places/academies가 외부 API 오류면 502 MAP_API_UNAVAILABLE을 돌려준다
- [ ] [web] POST /v1/children/{childId}/schedules는 201과 scheduleId · academyId · status를 돌려준다
- [ ] [web] POST /v1/children/{childId}/schedules에 targetPickup 61을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] [web] POST /v1/children/{childId}/schedules가 겹치면 409 SCHEDULE_OVERLAP과 overlapScheduleIds를 돌려준다
- [ ] [web] POST /v1/children/{childId}/schedules가 하루 한도를 넘으면 422 DAILY_SCHEDULE_LIMIT을 돌려준다
- [ ] [web] PUT /v1/schedules/{scheduleId}는 200과 scheduleId · academyId · status를 돌려준다
- [ ] [web] PUT /v1/schedules/{scheduleId}를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] GET /v1/children/{childId}/schedules는 200과 items[]를 돌려준다
- [ ] [web] GET /v1/children/{childId}/setup-summary는 200과 child · schedules · pickupSpots · joinCallMode · missing · settingsVersion을 돌려준다
- [ ] [web] POST /v1/children/{childId}/setup-completion은 200과 syncedToVehicle을 돌려준다
- [ ] [web] POST /v1/children/{childId}/setup-completion이 버전 충돌이면 409 SETTINGS_VERSION_CONFLICT를 돌려준다
- [ ] [web] POST /v1/children/{childId}/setup-completion이 누락이 있으면 422 SETUP_INCOMPLETE를 돌려준다
- [ ] [web] GET /v1/vehicles/me/weekly-pickups는 차량 토큰으로 200과 weekStart · days · readiness를 돌려준다
- [ ] [web] GET /v1/vehicles/me/weekly-pickups를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] GET /v1/vehicles/me/settings-summary는 차량 토큰으로 200과 guardianNameMasked · children · joinCallMode를 돌려준다
- [ ] [web] Idempotency-Key는 POST /v1/children/{childId}/schedules · POST /v1/children/{childId}/setup-completion에만 요구된다

## 확인 필요 사항

| 항목 | 현재 가정 |
|---|---|
| Academy에 externalPlaceId 칸이 없음 | 요청에는 externalPlaceId가 있으나 테이블에 없음. 같은 학원을 다시 쓸지(가족 · 자녀 간 공유) 매번 새로 만들지 정해야 함. 지금은 일정 저장 때마다 새로 만든다고 가정 |
| 일정 수정 때 학원 정보가 바뀐 경우 | 기존 Academy를 고칠지 새로 만들지 소스에 없음. 새로 만든다고 가정 |
| 「겹침」의 정의 | 소스는 「같은 아이의 겹치는 일정」만 있음. 같은 자녀 · 같은 요일 · 같은 classEndTime으로 가정 |
| 「하루 3개」의 기준 | 자녀별인지 가족 · 차량별인지 소스에 없음. 자녀별 같은 요일로 가정 |
| 4.5 표의 「목표 < 종료 422」 | 4.14는 targetPickup을 종료 후 0~60분으로 정해 목표가 종료보다 앞설 수 없음. 0~60 범위 검사로 대신함 |
| missing 판정 규칙 | 소스는 값 이름만 있음. 핵심 로직의 현재 가정으로 판정 |
| SET-08 「차량 동기화」의 동작과 syncedToVehicle 값 | 차량에 무엇을 보내는지, 완료 여부를 어디에 저장하는지 소스에 없음(완료 기록 컬럼 없음). 추가 저장 없이 true를 돌려준다고 가정 |
| DRAFT 일정을 SET-09에 보여 줄지 | 소스에 없음. 보여 주되 readiness에 반영한다고 가정 |
| 이름 마스킹 규칙(2자 · 4자 이상) | 예시 「김*호」뿐. 첫 글자와 마지막 글자만 남기고 가운데를 *로 바꾼다고 가정(2자는 두 번째 글자를 가림) |
| 외부 지도 API 공급자 · 키 | 예시 「kakao:12345」뿐. 카카오 로컬 검색으로 가정, 키는 환경 변수 |
| 일정 변경이 settingsVersion을 올리는지 | 04_사용자-user.md와 같은 질문. 올리지 않는다고 가정 |
| SET-05 | API 번호가 SET-04 다음 SET-06으로 건너뜀. 빠진 API가 없는지 확인 필요 |
| schedule ↔ place 서로 부름 | place는 Academy를 참조하고, SET-07 · 09는 place 장소를 읽음. schedule 쪽은 out 포트 + 테스트 대역으로 먼저 검증 |
