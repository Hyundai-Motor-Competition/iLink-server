# FOLLOWUP · 일정 (schedule)

- 생성: 2026-11-14 02:25 KST · tdd-guard
- 상태: 완료 (체크 88/88 · 완료 기준 4/4)
- 테스트: ● GREEN · 2914 passed · 전체 · 127s · .\gradlew.bat test --console=plain
- 설계서: C:/pw/schedule/docs/06_일정-schedule.md

## 완료 항목
- [x] #1 [기반] PickupSchedule은 학원 출입구가 없으면 DRAFT, 있으면 READY로 만들어진다
- [x] #2 [기반] 일정 규칙은 targetPickup이 0~60을 벗어나면 거절한다
- [x] #3 [기반] 일정 규칙은 weekdays가 비어 있으면 거절한다
- [x] #4 [기반] 겹침 규칙은 같은 자녀 · 같은 요일 · 같은 classEndTime 일정을 겹침으로 판정한다
- [x] #5 [기반] 하루 한도 규칙은 같은 자녀 같은 요일에 네 번째 일정이면 한도 초과로 판정한다
- [x] #6 [기반] targetPickupTime 계산은 classEndTime 18:00 · targetPickup 5에서 18:05를 돌려준다
- [x] #7 [기반] 이름 마스킹은 김보호를 김*호로 바꾼다
- [x] #8 [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] #9 [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] #10 [기반] AcademyEntity · PickupScheduleEntity가 BaseTimeEntity를 상속하고 location · entrance는 geography(Point,4326)로 매핑된다
- [x] #11 [기반] 테스트 시드로 학원 1곳(해솔영어학원, 출입구 있음)과 자녀 민준의 월 · 수 18:00 일정 1개를 만든다
- [x] #12 [persistence] Academy를 저장하고 읽으면 location · entrance 좌표가 같다
- [x] #13 [persistence] weekdays [MON, WED, FRI]를 저장하면 컬럼에 MON,WED,FRI로 들어가고 같은 목록으로 읽힌다
- [x] #14 [persistence] classEndTime 18:00을 저장하면 시간대 변환 없이 18:00으로 읽힌다
- [x] #15 [persistence] loadSchedulesOfChild는 그 자녀의 일정만 돌려준다
- [x] #16 [persistence] loadSchedulesOfChildren은 여러 자녀의 일정을 한 번에 돌려준다
- [x] #17 [서비스] 학원 검색은 외부 지도 API 결과를 저장하지 않고 externalPlaceId · name · address · location으로 돌려준다
- [x] #18 [서비스] 학원 검색은 외부 지도 API가 실패하면 MAP_API_UNAVAILABLE로 거절한다
- [x] #19 [서비스] 일정 등록은 학원을 만들고 scheduleId · academyId · status를 돌려준다
- [x] #20 [서비스] 일정 등록은 출입구 없는 학원이면 status DRAFT를 돌려준다
- [x] #21 [서비스] 일정 등록은 childId가 CHILD가 아니면 NOT_FOUND로 거절한다
- [x] #22 [서비스] 일정 등록은 다른 가족의 자녀면 NOT_FOUND로 거절한다
- [x] #23 [서비스] 일정 등록은 겹치는 일정이 있으면 SCHEDULE_OVERLAP과 겹치는 scheduleId 목록으로 거절한다
- [x] #24 [서비스] 일정 등록은 같은 요일 일정이 이미 3개면 DAILY_SCHEDULE_LIMIT으로 거절한다
- [x] #25 [서비스] 일정 수정은 자기 자신과는 겹침으로 판정하지 않는다
- [x] #26 [서비스] 일정 수정은 요일 · 시간 · 학원을 바꾸고 status를 다시 판정한다
- [x] #27 [서비스] 설정 요약은 자녀 기기가 CONNECTED가 아니면 missing에 CHILD_DEVICE를 넣는다
- [x] #28 [서비스] 설정 요약은 READY 일정이 없으면 missing에 SCHEDULE을 넣는다
- [x] #29 [서비스] 설정 요약은 일정 학원에 기본 장소가 없으면 missing에 PICKUP_SPOT을 넣는다
- [x] #30 [서비스] 설정 요약은 joinCallMode와 Family.settingsVersion을 담는다
- [x] #31 [서비스] 설정 완료는 settingsVersion이 다르면 SETTINGS_VERSION_CONFLICT로 거절한다
- [x] #32 [서비스] 설정 완료는 missing이 남아 있으면 SETUP_INCOMPLETE로 거절한다
- [x] #33 [서비스] 주간 일정은 weekStart가 없으면 한국 날짜 기준 이번 주 월요일부터 7일을 돌려준다
- [x] #34 [서비스] 주간 일정은 요일이 맞는 날짜에만 그 일정을 넣는다
- [x] #35 [서비스] 주간 일정 readiness는 자녀 기기나 기본 장소가 빠지면 startable false와 missing을 돌려준다
- [x] #36 [서비스] 차량용 설정 요약은 GUARDIAN 이름을 가려서 돌려주고 자녀 목록과 joinCallMode를 담는다
- [x] #37 [서비스] findSchedules는 scheduleId 목록의 일정과 학원 정보를 돌려준다
- [x] #38 [web] GET /v1/places/academies는 200과 items[]를 돌려준다
- [x] #39 [web] GET /v1/places/academies에 1자 query를 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] #40 [web] GET /v1/places/academies가 외부 API 오류면 502 MAP_API_UNAVAILABLE을 돌려준다
- [x] #41 [web] POST /v1/children/{childId}/schedules는 201과 scheduleId · academyId · status를 돌려준다
- [x] #42 [web] POST /v1/children/{childId}/schedules에 targetPickup 61을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] #43 [web] POST /v1/children/{childId}/schedules가 겹치면 409 SCHEDULE_OVERLAP과 overlapScheduleIds를 돌려준다
- [x] #44 [web] POST /v1/children/{childId}/schedules가 하루 한도를 넘으면 422 DAILY_SCHEDULE_LIMIT을 돌려준다
- [x] #45 [web] PUT /v1/schedules/{scheduleId}는 200과 scheduleId · academyId · status를 돌려준다
- [x] #46 [web] PUT /v1/schedules/{scheduleId}를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] #47 [web] GET /v1/children/{childId}/schedules는 200과 items[]를 돌려준다
- [x] #48 [web] GET /v1/children/{childId}/setup-summary는 200과 child · schedules · pickupSpots · joinCallMode · missing · settingsVersion을 돌려준다
- [x] #49 [web] POST /v1/children/{childId}/setup-completion은 200과 syncedToVehicle을 돌려준다
- [x] #50 [web] POST /v1/children/{childId}/setup-completion이 버전 충돌이면 409 SETTINGS_VERSION_CONFLICT를 돌려준다
- [x] #51 [web] POST /v1/children/{childId}/setup-completion이 누락이 있으면 422 SETUP_INCOMPLETE를 돌려준다
- [x] #52 [web] GET /v1/vehicles/me/weekly-pickups는 차량 토큰으로 200과 weekStart · days · readiness를 돌려준다
- [x] #53 [web] GET /v1/vehicles/me/weekly-pickups를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] #54 [web] GET /v1/vehicles/me/settings-summary는 차량 토큰으로 200과 guardianNameMasked · children · joinCallMode를 돌려준다
- [x] #55 [web] Idempotency-Key는 POST /v1/children/{childId}/schedules · POST /v1/children/{childId}/setup-completion에만 요구된다
- [x] #56 [persistence] 지도 API 어댑터는 카카오 로컬 검색 응답을 externalPlaceId(kakao:{id}) · name · address · location의 AcademyCandidate로 바꾼다
- [x] #57 [persistence] 지도 API 어댑터는 외부 API 오류 · 시간 초과면 502 MAP_API_UNAVAILABLE로 거절한다
- [x] #58 [persistence] LoadPickupSpotPort 어댑터는 place LoadPickupSpotUseCase.findSpots로 읽은 가족의 실제 장소 목록을 돌려준다(사람 결정 2026-11-13)
- [x] #59 [persistence] LoadMemberDeviceStatusPort 어댑터는 auth의 멤버 기기 상태를 schedule DeviceStatus로 바꿔 돌려준다
- [x] #60 [기반] 공통 BusinessException에 overlapScheduleIds를 담아 던지면 오류 응답 error.overlapScheduleIds에 실리고, 담지 않으면 그 키가 응답에 없다
- [x] #61 [서비스] findAcademy는 학원 id의 이름 · 주소 · 위치 · 출입구 · 대표번호를 돌려주고 없으면 빈 값을 돌려준다
- [x] #62 지도 API 어댑터는 200 응답이라도 본문을 읽지 못하거나 좌표가 없거나 숫자가 아니면 502 MAP_API_UNAVAILABLE로 거절한다
- [x] #63 saveSchedule로 scheduleId가 있는 일정을 다시 저장하면 새 행을 만들지 않고 그 행의 요일 · 시간 · 학원 · status가 바뀐다
- [x] #64 loadSchedule · loadAcademy에 없는 id를 주면 빈 값을 돌려준다
- [x] #65 [서비스] 일정 목록은 그 자녀의 일정을 scheduleId · 학원(academyId · name) · weekdays · classEndTime · targetPickup · status로 돌려주고, 내 가족의 CHILD가 아니면 NOT_FOUND로 거절한다
- [x] #66 PickupSchedule 검증 실패는 프레임워크와 무관한 도메인 예외(field · reason)로 던지고, 일정 등록 · 수정 서비스는 이를 422 VALIDATION_FAILED(fields)로 바꾼다
- [x] #67 Academy는 externalPlaceId를 가지며 academy.external_place_id는 유니크로 저장되고, 저장한 학원을 externalPlaceId로 찾을 수 있다
- [x] #68 일정 등록 · 수정은 같은 externalPlaceId의 학원이 있으면 새로 만들지 않고 그 academyId를 쓰며 학원 정보는 요청 값으로 갱신한다
- [x] #69 주간 일정 readiness는 오늘 일정 중 DRAFT가 있으면 startable false를 돌려준다
- [x] #70 일정 등록 · 수정은 같은 학원을 다시 쓸 때 요청의 entrance · phone이 비어 있으면 기존 값을 유지한다
- [x] #71 같은 externalPlaceId의 새 학원이 동시에 저장되어 유니크가 부딪히면 오류 없이 이미 저장된 그 학원을 다시 읽어 쓴다
- [x] #72 일정 수정은 자기 자신을 뺀 같은 요일 일정이 이미 3개면 DAILY_SCHEDULE_LIMIT으로 거절한다
- [x] #73 loadSchedules는 여러 scheduleId의 일정을 학원과 함께 한 번에 읽어 돌려준다(일정 조회 1회 · 학원 조회 1회)
- [x] #74 findSchedules는 loadSchedules를 한 번만 불러 받은 순서대로 돌려주고 없는 id는 뺀다
- [x] #75 joinCallMode는 schedule 자체 enum으로 설정 요약 · 차량용 설정 요약 in 포트와 응답에 담고 user.domain 타입을 import하지 않는다
- [x] #76 POST /v1/children/{childId}/schedules · PUT /v1/schedules/{scheduleId}에 101자 externalPlaceId를 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] #77 일정 수정은 등록과 같이 저장한 학원(기존 출입구 반영) 기준으로 status를 판정한다
- [x] #78 [서비스] SET-09 readiness는 오늘 일정이 DRAFT면 startable false와 missing에 SCHEDULE을 넣어 SES-02 픽업 시작 판정과 같은 값을 준다(D10, 사람 결정 2026-11-13) ← GetWeeklyPickupsServiceTest.주간 일정 readiness는 오늘 일정 중 DRAFT가 있으면 기기와 기본 장소가 다 있어도 startable false와 missing [SCHEDULE]을 돌려준다, GetWeeklyPickupsServiceTest.주간 일정 readiness는 오늘 일정이 DRAFT이고 기기도 기본 장소도 없으면 missing을 CHILD_DEVICE, SCHEDULE, PICKUP_SPOT 순서로 돌려준다
- [x] #79 [web] GET /v1/children/me/next-pickup(CHILD 전용)은 지금 이후 가장 가까운 픽업의 academyName · spotName · targetAt(KST)을 돌려주고, 남은 일정이 없으면 200과 null 본문을 준다(최종디자인 11/13 C01 실내 대기) ← ChildScheduleControllerTest.GET /v1/children/me/next-pickup은 CHILD 토큰으로 200과 academyName · spotName · targetAt(KST +09:00)을 돌려준다, ChildScheduleControllerTest.GET /v1/children/me/next-pickup은 남은 일정이 없으면 200과 빈(null) 본문을 돌려준다, ChildScheduleControllerTest.GET /v1/children/me/next-pickup을 GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다, GetNextPickupServiceTest.다음 픽업은 오늘 아직 지나지 않은 READY 일정의 학원 이름 · 기본 장소 이름 · targetAt(classEndTime + targetPickup분)을 돌려준다, GetNextPickupServiceTest.다음 픽업은 여러 일정 중 targetAt(classEndTime + targetPickup분)이 가장 이른 것을 돌려준다
- [x] #80 [web] GET /v1/vehicles/me/weekly-pickups의 각 픽업에 자녀 profileImageUrl을 담는다(V01 자녀 카드) ← VehicleScheduleControllerTest.GET /v1/vehicles/me/weekly-pickups는 pickups[]마다 자녀 profileImageUrl을 담고 URL이 없으면 null로 돌려준다, GetWeeklyPickupsServiceTest.주간 일정은 각 픽업에 그 자녀의 profileImageUrl을 담고 URL이 없는 자녀는 null로 둔다, GetWeeklyPickupsServiceTest.주간 일정은 가족 CHILD들의 userId로 findProfileImageUrls를 한 번만 부른다, LoadMemberServiceTest.findProfileImageUrls는 profileImageKey가 있는 사용자에게 그 키의 서명 URL을, 없는 사용자에게 기본 아바타 서명 URL을 준다
- [x] #81 [web] GET /v1/families/me/today-pickups(GUARDIAN · CO_GUARDIAN)는 오늘 가족 자녀들의 픽업을 시각 순으로 childName · academyName · targetAt · status(SCHEDULED · IN_PROGRESS · COMPLETED)로 돌려준다(부모 메인 오늘의 픽업 타임라인) ← FamilyScheduleControllerTest.GET /v1/families/me/today-pickups는 GUARDIAN 토큰으로 200과 items[](scheduleId · childName · academyName · targetAt(KST +09:00) · status)를 유스케이스 순서대로 돌려준다, FamilyScheduleControllerTest.GET /v1/families/me/today-pickups는 CO_GUARDIAN 토큰으로도 200과 items를 돌려준다, FamilyScheduleControllerTest.GET /v1/families/me/today-pickups를 CHILD 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다, GetTodayPickupsServiceTest.오늘의 픽업은 한국 날짜 오늘 요일이 들어 있는 가족 자녀들의 일정만 돌려주고 DRAFT 일정도 넣는다
- [x] #82 [persistence] Academy는 familyId를 갖고 (familyId, externalPlaceId)가 유니크다. 두 가족이 같은 externalPlaceId로 일정을 등록하면 학원 행이 가족마다 따로 생긴다(사람 결정 2026-11-13: 학원은 가족마다 따로 둠) ← SchedulePersistenceAdapterTest.두 가족이 같은 externalPlaceId로 학원을 저장하면 academyId가 서로 다른 academy 행이 가족마다 따로 생긴다, SchedulePersistenceAdapterTest.loadAcademyByExternalPlaceId(familyId, externalPlaceId)는 그 가족의 학원만 돌려주고 그 externalPlaceId 학원이 없는 가족으로 찾으면 빈 값을 돌려준다, SchedulePersistenceAdapterTest.Academy를 저장하고 loadAcademy로 읽으면 familyId가 그대로이고 academy.family_id 컬럼에 그 값이 들어간다, SchedulePersistenceEntitiesMappingTest.academy에는 (family_id, external_place_id)를 묶은 유니크 인덱스가 1개 있고 external_place_id 하나만 묶은 유니크 인덱스는 없다
- [x] #83 [서비스] 일정 등록 · 수정의 학원 다시 쓰기는 같은 가족의 학원만 갱신하고, 다른 가족 학원의 이름 · 주소 · 위치 · 출입구 · 대표번호를 바꾸지 않는다 ← AcademyReuseAcrossFamiliesIntegrationTest.가족 A의 학원이 있는 externalPlaceId로 가족 B가 다른 이름 · 주소 · 위치 · 출입구 · 대표번호를 담아 일정을 등록하면 B는 새 academyId(B 가족 소유)를 받고 A 학원 행의 값은 그대로다, AcademyReuseAcrossFamiliesIntegrationTest.가족 B가 일정 수정으로 같은 externalPlaceId 학원의 이름 · 주소 · 위치 · 출입구 · 대표번호를 바꾸면 B의 학원만 바뀌고 가족 A 학원 행의 값은 그대로다
- [x] #84 [서비스] 해제된 자녀(user MemberInfo.removed true)에게는 일정 생성 · 수정을 404 NOT_FOUND로 거절한다. 해제된 자녀의 기존 일정 행은 지우지 않는다(사람 결정 2026-11-14) ← CreateScheduleServiceTest.일정 등록은 해제된 자녀(removed true)면 NOT_FOUND로 거절하고 학원 · 일정을 저장하지 않는다, UpdateScheduleServiceTest.일정 수정은 해제된 자녀(removed true)의 일정이면 NOT_FOUND로 거절하고 학원 · 일정을 저장하지 않는다
- [x] #85 [서비스] 주간 일정(SET-09)의 자녀 profileImageUrl은 findMembers가 주는 MemberInfo.profileImageUrl을 쓰고, user LoadMemberUseCase.findProfileImageUrls와 LoadUserPort.loadUsersByIds는 지운다(사람 결정 2026-11-14, main 병합 뒤 중복 정리) ← GetWeeklyPickupsServiceTest.주간 일정은 각 픽업에 findMembers가 준 그 자녀의 MemberInfo.profileImageUrl을 담고 URL이 없는 자녀는 null로 둔다, GetWeeklyPickupsServiceTest.주간 일정은 findMembers가 준 MemberInfo.profileImageUrl을 담고 findProfileImageUrls 같은 별도 조회를 하지 않는다, UserInPortsTest.LoadMemberUseCase에 findProfileImageUrls가 없다(사진 URL은 MemberInfo.profileImageUrl로 준다), UserOutPortsTest.LoadUserPort에 loadUsersByIds가 없다
- [x] #86 [서비스] 일정 등록 · 수정으로 학원에 출입구가 생기면 같은 가족이 그 학원을 쓰는 다른 DRAFT 일정도 READY로 다시 판정해 저장한다(사람 결정 2026-11-14) ← CreateScheduleServiceTest.일정 등록으로 학원에 출입구가 생기면 같은 가족이 그 학원을 쓰는 다른 DRAFT 일정을 READY로 다시 저장하고 이미 READY인 일정은 다시 저장하지 않는다, UpdateScheduleServiceTest.일정 수정으로 학원에 출입구가 생기면 같은 가족이 그 학원을 쓰는 다른 DRAFT 일정을 READY로 다시 저장하고 이미 READY인 일정은 다시 저장하지 않으며 수정한 일정은 한 번만 저장한다, AcademyEntranceRejudgeIntegrationTest.출입구 없는 학원으로 DRAFT인 민준의 일정이 있을 때 서윤이 같은 학원을 출입구와 함께 등록하면 민준의 일정 행이 READY로 바뀌고 같은 externalPlaceId를 쓰는 다른 가족의 DRAFT 일정은 그대로다, SchedulePersistenceAdapterTest.loadSchedulesOfAcademy는 그 학원을 쓰는 일정만 자녀와 상관없이 학원과 함께 돌려주고 다른 학원의 일정은 뺀다
- [x] #87 [web] GET /v1/children/me/next-pickup(SET-12)은 남은 일정이 없으면 200, Content-Type application/json, 본문 JSON null을 돌려준다(사람 결정 2026-11-14) ← ChildScheduleControllerTest.GET /v1/children/me/next-pickup은 남은 일정이 없으면 200, Content-Type application/json, 본문 JSON null을 돌려준다
- [x] #88 [서비스] 해제된 자녀(MemberInfo.removed true)면 일정 목록(SET-04) · 설정 요약(SET-07)도 404 NOT_FOUND로 거절한다(설계서 「해제된 자녀」 절) ← ListSchedulesServiceTest.일정 목록은 해제된 자녀(removed true)면 NOT_FOUND로 거절하고 일정 조회 포트를 부르지 않는다, GetSetupSummaryServiceTest.설정 요약은 해제된 자녀(removed true)면 NOT_FOUND로 거절하고 일정 조회 포트를 부르지 않는다

## 미완료 항목
- 없음

## 완료 기준 (공통 규칙 문서)
- [x] C1 설계서의 테스트 케이스가 모두 GREEN
- [x] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [x] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [x] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 2026-11-14 02:24 KST · 도메인 리뷰어 · 결론 통과 · 상 0 · 중 0 · 하 7 · 리뷰 시점 체크 88/88
- 리뷰 시점 테스트: ● GREEN · 2914 passed · 전체 · 127s · .\gradlew.bat test --console=plain
  - 결론: 통과
  - 지난 리뷰(11-14 01:27)의 [중] 4건은 모두 해소: main 병합 충돌 · #80 중복 구현(git log HEAD..main 없음, findProfileImageUrls · loadUsersByIds 0건, GetWeeklyPickupsService.java:53-61이 MemberInfo.profileImageUrl 사용), 해제된 자녀 404(FamilyChildGuard.java:18, SET-02 · 03 · 04 · 07), SET-11 번호 중복(ChildScheduleApiDoc:17 SET-12, FamilyScheduleApiDoc:13 SET-13), 출입구가 생겨도 DRAFT 그대로(ScheduleWriteRules.java:43-52, AcademyEntranceRejud…
  - [하] TODO.md에서 공유모듈과 사용자가 다시 「대기 · 완료 기준 0/4 · 기록 없음」으로 돌아감 — TODO.md:12-28. 실제 기록은 shared.md:4 완료 4/4, user.md:4 완료 4/4 → note(TODO 다시 만들기, 원인 확인은 사람 결정)
  - [하] FOLLOWUP schedule.md에 해소됐거나 뒤집힌 메모가 남음(L163 #88로 해소, L164 rejudgedWith 테스트 해소, L155 #85로 뒤집힘, L168 #86으로 결정, L157 · L159 · L160 옛 번호 SET-11 · SET-12) → note(정리)
  - [하] 옛 API 번호가 남음 — 08:436 findProgressBySchedule 주석 「(schedule SET-12)」는 SET-13이어야 함, FamilyScheduleControllerTest.java:42 주석도 「SET-12」 → note(문서 · 주석 수정)
  - [하] #86 다시 판정이 형제 일정의 행 전체를 merge로 덮어씀 — ScheduleWriteRules.java:51, PickupScheduleEntity.java:20에 @Version 없음. 동시 수정이 사라질 수 있음(학원 merge 동시 수정 메모와 같은 종류) → note
  - [하] pickup_schedule.academy_id에 인덱스가 없음 — findByAcademyId(PickupScheduleJpaRepository.java:13)를 출입구 있는 학원 등록 · 수정마다 부름 → note
  - [하] SET-08 설정 완료도 해제된 자녀에게 404를 줌. 06 「해제된 자녀」 절은 SET-02 · 03 · 04 · 07만 적고 테스트도 없음 — CompleteSetupService.java:22 → note(06 절에 SET-08 추가. 항목 추가는 선택)
  - [하] 08에 PickupProgressQueryService 테스트 항목이 아직 없음 → 항목 추가(pickup)
  - 완료 기준
  - C1 충족 — 체크 88/88, 전체 GREEN 2914 passed. #84~#88 증거 테스트가 모두 있고 단언이 구체적(verifyNoInteractions, times(2) · containsExactlyInAnyOrder, 통합 테스트는 DB 행 status 직접 확인).
  - C2 충족 — 해제된 자녀 404, 출입구가 생기면 같은 가족 학원의 DRAFT만 다시 판정, SET-12 JSON null, SET-09 profileImageUrl이 06과 맞음. SET-12 범위 rangeClosed(0, 7) = 오늘~오늘+7.
  - C3 충족(이 리뷰 기록 시) — 도메인에 Spring 없음, 컨트롤러는 in 포트만, 설명은 doc 인터페이스에만. 02 위반 없음.
  - C4 충족 — 06 확인 필요 표의 모든 행에 현재 가정이나 결정이 있음. 새 결정 2건은 핵심 로직 절로 들어감.
  - 후속 과제 판정: 해소 — L155(#85), L161(사람 결정 4건), L163(#88), L168(#70 · #86), L177 · L188 · L193 · L194(대체됨). 남음 — academy 기존 DB 마이그레이션, #79 팀장 판단 사람 확인, place findAcademy 가족 확인, 자정 넘는 일정, #81 CANCELLED 표시, bisect 주의 커밋, 해제된 자녀 DRAFT도 READY가 됨(사람 결정), 1자 이름 마스킹, 06 시그니처 record 필드, 기반 리뷰 (2)(4)(5)(6), ScheduleErrors 문구 · 03 반영, 시스템 PATH, auth N+1, 카카오 카테고리 · 시간 초과, weekStart · 하루 3개 자르기, SET-01 query 400,…
  - 테스트: GREEN 2914 passed(127s)
- 이전 리뷰: 2026-11-14 01:27 KST (조건부 통과)

## 확인 필요 사항 (설계서)
| 항목 | 현재 가정 |
|---|---|
| Academy에 externalPlaceId 칸이 없음 | 해소: externalPlaceId 컬럼으로 같은 가족 안에서 같은 학원을 다시 씀((familyId, externalPlaceId) 유니크, #82). 핵심 로직 「학원 다시 쓰기」 |
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

## 다음 단계
- 커밋 · 푸시: /tdd commit (기능별 분할 커밋) · /tdd push (커밋 후 기능 브랜치 푸시)
- 다음 도메인: 장소 (07_장소-place.md). 시작: /tdd domain place

## 후속 과제
- 사람 결정 2026-11-14: #81 today-pickups status는 schedule out 포트(예: LoadPickupStatusPort) 어댑터가 pickup in 포트를 불러 정한다. 02 의존 방향에 schedule → pickup 추가(18개, 서로 부르는 쌍 7개). #80 profileImageUrl은 user LoadMemberUseCase에 userId 목록 → 서명 URL Map 메서드를 더해 받는다(GetMyFamilyService와 같은 규칙, MemberInfo는 그대로)
- academy 테이블이 이미 있는 DB(dev · prod)는 ddl-auto update로 스키마가 바뀌지 않음: 옛 external_place_id 단독 유니크 삭제, family_id NOT NULL 추가(기존 행은 일정의 자녀 가족으로 채움, 여러 가족이 같은 학원을 쓰면 행 복제) 마이그레이션이 따로 필요(#82)
- #79 next-pickup(SET-11) 팀장 판단: READY 일정만 대상(DRAFT는 SES-02가 시작을 막으므로 제외), 오늘부터 7일 안에서 targetAt이 지금보다 뒤인 가장 이른 것, spotName은 그 학원의 가족 기본 장소(없으면 null). 사람 확인 필요 시 조정
- [중 · place 도메인] 학원이 가족마다 따로 생긴 뒤(#82), place의 장소 추가 · 수정 · 목록 · 추천(AddPickupSpotService:34 등)이 findAcademy(academyId)로 읽은 학원이 요청 가족 것인지 확인하지 않아 다른 가족 academyId로 장소를 만들 수 있음. AcademyInfo에 familyId를 담고 place에서 404로 거절할지 07 장소 설계서와 함께 사람 결정 필요
- classEndTime + targetPickup이 자정을 넘는 일정(예 23:50 + 15)은 targetPickupTime(LocalTime)이 0시로 돌아가 SET-09 · SET-11에서 같은 날짜로 계산됨. 자정 넘는 일정을 막을지(검증) 다음 날로 볼지 설계 결정 필요
- #81 today-pickups(SET-12) 팀장 판단: DRAFT 일정도 타임라인에 보여 줌(status SCHEDULED), CANCELLED 세션만 있으면 SCHEDULED로 보임. 취소를 따로 보여 줄 값(CANCELLED)이 필요한지는 디자인과 함께 사람 확인 필요
- 사람 결정 2026-11-14(도메인 리뷰 뒤): (1) 현재 작업 커밋 → main을 feature/schedule에 병합 → #80은 main의 MemberInfo.profileImageUrl을 쓰고 findProfileImageUrls · loadUsersByIds 제거, main의 해제된 자녀 404 항목 반영. (2) API 번호: next-pickup SET-11 → SET-12, today-pickups SET-12 → SET-13. (3) 학원에 출입구가 생기면 같은 가족의 그 학원 일정 status를 다시 판정. (4) next-pickup 남은 일정 없음은 200 + JSON null(application/json)
- bisect 주의: 5678ff9(#82 · #83)와 6b93839(#79) 커밋은 테스트 코드가 컴파일되지 않음(프로덕션은 컴파일됨). d6e4bf9부터 전체 GREEN
- #84 팀장 판단: FamilyChildGuard가 해제된 자녀를 404로 거절하므로 SET-04 일정 목록 · SET-07 설정 요약도 해제된 자녀에게 404를 준다(이 동작을 확인하는 테스트는 아직 없음). #86 범위: 저장한 학원에 출입구가 있으면 그 학원의 다른 DRAFT 일정을 READY로 다시 판정(원래 출입구가 있었는데 DRAFT가 남은 경우도 같이 READY가 됨)
- #86 다시 판정은 자녀를 가리지 않아, 해제된 자녀의 남은 DRAFT 일정도 같은 학원에 출입구가 생기면 READY가 됨(해제된 자녀는 findMembers에서 빠져 SET-09 · 13에 나오지 않으므로 화면 영향 없음). 해제된 자녀 일정을 빼야 하는지는 사람 확인 필요. PickupSchedule.rejudgedWith 도메인 단위 테스트 없음(서비스 · 통합 테스트로만 검증)
- 재리뷰(11-14) [하] 처리: (1) TODO.md에서 공유모듈 · 사용자가 「대기 · 0/4」로 되돌아감(shared.md · user.md는 완료 4/4) — TODO 다시 만들기, 원인 확인 필요 (2) FOLLOWUP 해소 메모 정리(L155 · L163 · L164 · L168, 옛 번호 SET-11 · SET-12, 다른 저장소 메모 L170~L175) (3) FamilyScheduleControllerTest.java:42 주석 SET-12 → SET-13 (4) #86 다시 판정이 형제 일정 행 전체를 merge로 덮어씀, PickupScheduleEntity에 @Version 없음 — 동시 수정 유실 가능 (5) pickup_schedule.academy_id 인덱스 없음(findByAcademyId, child_user_id 인덱스와 함께) (6) SET-08도 해제된 자녀 404 — 06 절에 반영함, 테스트 항목은 선택 (7) 08 PickupProgressQuery · loadByFamilyAndDate 테스트 항목을 pickup 설계서에 추가함(테스트는 이미 있음, pickup 도메인 진행 때 체크)
- NameMasker.mask는 1자 · 빈 문자열 · null 이름에서 예외가 난다(repeat(-1) · charAt). User 이름 규칙상 이런 입력이 올 수 있는지 확인하고, 올 수 있으면 규칙을 정해 테스트부터 추가
- 설계서 06 시그니처의 LoadMemberDeviceStatusPort 반환 타입 표기 DeviceStatus를 실제 쓰는 auth.domain.MemberDeviceStatus로 고쳐야 함(05 인증 L152가 user · schedule용으로 내놓은 타입). 모듈용 record ScheduleInfo · AcademyInfo · SpotSummary 필드도 설계서에 없어 #37 · place 500m 검사 · pickup 연락처 용도로 정함 → 설계서에 반영 필요
- [기반 리뷰 · 하] (1) #37 findSchedules는 여러 id를 한 번에 읽는 포트가 없어 N+1 조회가 됨 (2) pickup_schedule.child_user_id에 인덱스 없음(loadSchedulesOfChild(ren) 조회 칸) (3) Academy · PickupSchedule.restore의 id 인자가 long으로 user · auth(Long)와 다름 (4) SearchAcademyPort(out)가 in 포트 record AcademyCandidate를 import함 (5) schedule 테스트 헬퍼 중복: PickupScheduleTest 학원 헬퍼 두 쌍, ScheduleOverlapRuleTest · DailyScheduleLimitRuleTest의 academy · newSchedule · savedSchedule, 포트 테스트 리플렉션 헬퍼가 모듈마다 복사됨 (6) ScheduleSeed.clear는 시드 행만 지우므로 lane 테스트가 만든 행은 각 테스트가 지워야 함
- [사람 결정 2026-11-09] (1) 학원은 가족 안에서 다시 쓴다: Academy에 familyId · externalPlaceId와 (familyId, externalPlaceId) 유니크. 다시 쓸 때 이름 · 주소 · 위치는 요청 값, 출입구 · 대표번호는 값이 있을 때만 바꾸고 출입구가 새로 생기면 가족 일정 status를 다시 판정(이 갱신 규칙은 팀장 가정 — 사람 확인 필요) (2) 409 SCHEDULE_OVERLAP은 error.overlapScheduleIds 키를 더한다. 후속: 03 공유모듈 설계서의 공통 오류 본문 규칙(키 목록)에 「도메인 오류가 추가 키를 더할 수 있음」 반영 필요. 같은 가족이 같은 학원으로 동시에 등록하면 유니크 위반이 날 수 있어 처리 방식(재조회 후 재사용) 결정 필요. 07 place · 08 pickup 설계서의 학원 참조 문구도 이 결정에 맞는지 확인
- ScheduleErrors의 한국어 메시지 5개(DAILY_SCHEDULE_LIMIT · SETTINGS_VERSION_CONFLICT · SETUP_INCOMPLETE · MAP_API_UNAVAILABLE · SCHEDULE_OVERLAP)는 설계서에 문구가 없어 구현자가 정함 → 문구 확인 필요. 03 공유모듈 설계서 89행(본문 키 목록)과 230~233행 시그니처에 BusinessException.withExtra · extras() 반영 필요
- [팀장 결정 · 설정 키] 픽업 기준값은 application.yml `pickup.session.*` 하위 키로 둠(`pickup:` 아래는 이미 async · idempotency · jwt 등 앱 전체 설정). 롤백되면 저장하지 않는 이벤트(ETA_UPDATED)도 커밋 뒤 발행하지 않음
- [하] 문서 설정 키 접두어 불일치: 08 문서는 `designdot.jwt.secret` · `designdot.admin.*`, 실제 코드는 `designmodu.*`. 06 week-start는 `designmodu.calendar.week-start`로 맞춤(일정 #1, 2026-10-27). 08 문서 표기 정리 필요
- [하] 일정 2차 중간 리뷰(2026-10-28): ① ArchitectureTest의 applicationDoesNotDependOnAdaptersOrTechnology와 새 applicationUsesOnlyServiceAndTransactionalFromSpring이 spring web · data 부분 중복(둘 다 유지), 02 문서 ArchUnit 예시 코드(189–297행)에 새 규칙 미반영 ② `designmodu.calendar.week-start`가 application.yml에 미기재, 대소문자 구분(sunday → 기동 실패), CalendarConfig 바인딩 전용 테스트 없음 ③ MonthlyScheduleResponse.weekStart Swagger enum 값 의미 설명 누락(02 "enum은 모든 값 의미 나열") ④ archfixture ServiceUsingAutowired의 불필요한 school 참조 ⑤ SchedulePersistenceAdapterTest · ApiDocsTest 일부 중복 단언(실패 위치 좁힘 용도로 유지) ⑥ SchoolPersistenceTestSupport · ExhibitionPersistenceTestSupport region · school · major 헬퍼 중복
- [하] 일정 도메인 리뷰(2026-10-28) 남은 과제: ① 2차 리뷰 ②의 "sunday → 기동 실패"는 Boot 4.1.1 LenientStringToEnumConverterFactory 때문에 틀렸을 수 있음(실기동 미확인) — CalendarConfig 바인딩 테스트 · application.yml 기재와 함께 확인 ② ApiDocsTest에 UpcomingExhibitionResponse 스키마 속성 이름 "dDay" 단언 없음 ③ GetMonthlyScheduleUseCase(Integer, Integer) → MonthlyScheduleQuery 또는 Optional<YearMonth>로 불변 조건을 유스케이스 입력에 담는 리팩터링 ④ ScheduleControllerTest · ExhibitionControllerTest에 공개 GET 허용 · POST 403 케이스 없음(02 626행) ⑤ SchedulePersistenceAdapterTest 쿼리 수 단언 없음(02 625행, PreparedStatementCounter 재사용) ⑥ GetUpcomingExhibitionsService가 행마다 Period.of 검증 → 잘못된 기간 행이면 GET 400 INVALID_PERIOD(DB CHECK로 위험 낮음) ⑦ ScheduleApiDocs 월간 400 설명에 "숫자 아님" 누락 ⑧ 월간 · 다가오는 조회 EXPLAIN으로 idx_exhibition_start_date_id 사용 확인. 문서 정합(02 패키지 트리 · ArchUnit 예시, 06 의존 표)은 리뷰 직후 반영함
- [정정 · 하] 일정 재리뷰(2026-10-28): 2차 중간 리뷰 ②의 "sunday → 기동 실패"는 틀림 — DESIGNMODU_CALENDAR_WEEK_START=sunday로 실기동 통과(Boot 관대한 enum 변환), xyz만 실패. 남은 것: CalendarConfig 바인딩 전용 테스트 없음, `designmodu.calendar.week-start`를 application.yml에 명시할지(설계 결정). 추가 [하]: ① ApiDocsTest에 UpcomingExhibitionResponse 스키마 속성 "dDay" 단언 없음 ② ScheduleApiDocs · ExhibitionApiDocs(hot limit) 400 설명에 "숫자 아님" 누락 — 두 도메인 함께 정리 ③ archunit.properties failOnEmptyShould=false 유지 중(픽스처 있는 규칙 5개뿐, 나머지는 패턴 오타 시 빈 검사 통과 가능) — true 전환은 전 도메인 공통 설계 결정 ④ 01 305행 · 06 23행 다이어그램은 리뷰 직후 Period.of · dDayFrom 반영함. EXPLAIN은 확인 완료(idx_exhibition_start_date_id 사용, searchFrom 하한 효과 확인)
- [하] 일정 3차 리뷰(2026-10-28): ① CalendarConfig 바인딩 테스트 없음 — 항목 추가 여부는 사람 결정(ApplicationContextRunner로 SUNDAY · 생략 → MONDAY · 소문자 sunday · xyz 기동 실패, application.yml 명시 여부와 함께) ② schedule/adapter/in/web/dto/package-info.java Javadoc 앞 공백 4칸 — /tdd pkginfo delete로 package-info를 지우면 함께 해소, 지우지 않으면 커밋 전 되돌리기 ③ ServiceUsingAutowired의 school ResolveRegionIdsUseCase 참조 정리 남음 ④ PublicAccess POST 403 단언 중복(실패 위치 좁힘 용도로 유지) ⑤ REVIEW.md · schedule.md의 리뷰 시점 테스트 RED 기록은 3차 리뷰 기록으로 GREEN(1194 passed · 1 skipped)으로 갱신됨 ⑥ 01:305 다이어그램 라벨은 리뷰 직후 "Period.MAX_DAYS (서비스는 dDayFrom도)"로 정정함
- schedule: 이름 마스킹 1자 이름 · 빈 문자열 규칙이 설계서에 없음. 지금은 그대로 돌려줌(GuardianNameMasker). 규칙 확정 필요
- schedule: DeviceStatus를 auth.domain.MemberDeviceStatus 재사용 대신 schedule.domain.DeviceStatus로 따로 둠(어댑터에서 변환). joinCallMode는 user.domain.JoinCallMode 재사용. SpotSummary에 academyId 추가(PICKUP_SPOT 학원별 판정용). ScheduleInfo에 targetPickupTime 없음(pickup이 계산)
- 환경: 시스템(Machine) PATH에 줄바꿈이 섞인 항목 "C:\Program⏎ Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin"이 있음. Docker가 꺼져 있으면 Testcontainers가 PATH를 읽다 InvalidPathException. 사람이 시스템 PATH를 고쳐야 함
- schedule: pickup_schedule.child_user_id → users, academy_id → academy는 설계서상 FK지만 DB FK 없이 Long 칼럼만 둠(user · auth와 같은 관례). DB FK가 필요하면 schema.sql로 추가할지 결정 필요
- schedule: 자녀 확인은 auth와 같이 user in 포트 LoadMemberUseCase를 서비스에 직접 주입함. 설계서 「시그니처」 out 포트 목록에 user 연결이 없으니 설계서 의존 · 시그니처에 반영 필요
- auth: GetMemberDeviceStatusService.getDeviceStatuses가 userId마다 loadLiveDevices · loadLatestInvite를 따로 불러 1명당 쿼리 2회(N+1). schedule SET-07 · 09가 이를 거침
- [결정 · 사람] 공통 오류 응답에 선택 칸 overlapScheduleIds(값 있을 때만 키 노출) 추가. 03 공유모듈 설계서 L89 「본문 키 고정」 문장과 BusinessException 시그니처 갱신 필요
- schedule 가정: SET-07 일정이 하나도 없으면 missing에 SCHEDULE만 넣고 PICKUP_SPOT은 넣지 않음. PICKUP_SPOT 판정에 DRAFT 일정 학원 포함 여부 미정(지금은 모든 일정 학원 포함). 기기 상태가 없으면 NONE으로 봄
- schedule: 카카오 학원 검색에 카테고리 필터(category_group_code=AC5 학원) · size · page를 쓸지 설계서에 없음. 지금은 키워드 검색만. 지도 API 시간 초과 값은 테스트로 확인하지 못함
- schedule: 카카오 응답 좌표 문자열이 잘못 오면 Double.parseDouble NumberFormatException으로 500(502 아님). 200 응답 본문 이상 처리 미정
- schedule 가정(SET-09): readiness 대상은 한국 날짜 오늘 일정이 있는 자녀 · 학원만. 오늘 일정이 없으면 startable false · missing 빈 목록. 하루 안 classEndTime 오름차순. 월요일이 아닌 weekStart 처리(422 또는 그 주 월요일로 맞춤) 미정. 「DRAFT를 readiness에 반영」 방식 미정(missing 값에 해당 없음). 「하루 최대 3개」 자르기 안 함
- schedule: SET-01 query를 아예 안 보내면 공통 처리로 400 INVALID_REQUEST(422 아님). 「필수」 위반을 422로 맞출지 결정 필요. 2~50자 길이 기준(code point/UTF-16) 미정
- schedule: findSchedules(pickup SES-02용)가 LoadSchedulePort.loadSchedule을 id마다 불러 N+1. 여러 id 조회 out 포트 추가 여부 결정 필요. SET-10에서 GUARDIAN이 없으면 guardianNameMasked null
- [중간 리뷰 · persistence 병합] [중] PickupSchedule(domain)이 common.error.BusinessException(Spring HttpStatus 의존)을 던짐 — 02 L70 「domain은 프레임워크 무관 common 순수 타입만」 위반. 도메인 전용 예외 + 변환 또는 BusinessException을 예외 목록에 넣을지 결정 필요(사람). [하] SchedulePersistenceAdapterTest:134 status 단언 약함(READY로), KakaoMapSearchAdapterContextTest 이름과 달리 키 비어 있음 보장 안 함, 502 변환 때 원인 로그 없음, ScheduleOverlapRule · DailyScheduleLimitRule private 생성자 없음, findByChildUserId 정렬 · child_user_id 인덱스 없음(SET-04 순서 기준 미정), DeviceStatus.valueOf(name()) 값 추가 때 깨짐, 설계서 구조에 adapter/out/auth · place 없음 · 지도 어댑터 lane 표와 다름, 수정 때 자기 제외는 service 몫임을 설계서에 적을 것
- schedule: SET-02 weekdays 중복(["MON","MON"]) 처리 미정. classEndTime 형식 오류를 422로 처리(02 공통 규칙의 400 타입 오류와 경계 확인 필요)
- schedule: SET-02 AcademyRequest 길이 제한(name 100 · address 255 · phone 20)을 넣었으나 테스트 없음. externalPlaceId 길이 제한 없음(저장 안 함)
- schedule 가정: SET-04 목록 순서는 scheduleId 오름차순(서비스에서 정렬), weekdays는 월요일부터
- [결정 · 사람] (1) 학원은 externalPlaceId로 재사용(가족 간 공유, 정보는 마지막 요청 값으로 갱신 — 다른 가족이 entrance를 바꾸면 함께 바뀌는 점 확인 필요, dev DB에 행이 있으면 NOT NULL 컬럼 추가 실패 가능) (2) 오늘 DRAFT 일정이 있으면 SET-09 startable false(missing 값은 그대로) (3) 설정 완료 기록 · readiness 반영은 후속 과제(user · pickup 설계와 함께) (4) 도메인 검증은 도메인 전용 예외 + 서비스에서 422 변환. 설계서 확인 필요 사항 L179 · L180 · L185 · L186 갱신 필요
- schedule 학원 공유 위험(#67 #68): 요청 값으로 덮으므로 entrance · phone 없이 보낸 요청이 다른 가족의 출입구를 null로 지움(다른 가족 일정의 출입구 판단에 영향). 두 가족이 같은 새 학원을 동시에 등록하면 유니크 위반 오류 처리 미정. 설계서 학원 표에 external_place_id VARCHAR(100) UNIQUE NOT NULL 추가 필요. dev(ddl-auto update) 기존 행 있으면 컬럼 추가 실패, prod(validate) 이전 계획 필요
- 커밋 bb9bf90 · 8e94d18은 단독으로는 테스트가 깨짐(bb9bf90: PickupScheduleTest 실패, 8e94d18: GetWeeklyPickupsServiceTest 컴파일 실패). 791e7ba부터 GREEN. 이 구간에서 bisect하면 오탐 가능
- [결정 · 사람, 도메인 리뷰 뒤] (1) 학원 재사용 때 entrance · phone이 비면 기존 값 유지 (2) loadSchedules(List<Long>) 포트 지금 추가 (3) JoinCallMode는 schedule 자체 enum (4) 동시 등록 유니크 충돌은 다시 읽어 재사용. [공통 · 하] LatLng 범위 검증 없음(location {} → (0,0), lat 999 → 500 가능) — place 도메인과 함께 공통에서 처리할지 결정 필요. [하] pickup_schedule.child_user_id 인덱스 없음, prod external_place_id DDL 계획 없음
- schedule 결정(팀장): 동시 등록 충돌로 학원을 재사용할 때도 name · address · location은 요청 값, entrance · phone은 비어 있으면 기존 값(COALESCE). 「비어 있으면」은 null만 — 빈 문자열 · 공백 phone 처리 미정
- [결정 · 사람] 02 L72 네이티브 쿼리 예외에 academy upsert 추가. [재리뷰 하 · 후속] 이미 있는 학원 갱신은 JPA merge라 동시 수정 때 다른 가족이 막 넣은 entrance를 지울 수 있음(이미 있는 학원도 DB COALESCE로 저장할지). FOLLOWUP 확인 필요 표가 06 해소 반영 전 내용이고, 해소된 메모(findSchedules N+1 · joinCallMode · 출입구 null · externalPlaceId 길이 · 좌표 500)와 다른 저장소 메모 5건이 남아 있음 — 정리 필요. name · address · phone 길이 테스트 없음. 빈 문자열 phone은 기존 값을 덮어씀(설계 결정 필요)
- [재리뷰 2 · 하 · 선택] UpdateScheduleService가 같은 입력으로 일정을 두 번 만듦(candidate는 겹침 · 한도용, judged는 저장 학원 기준 status용). 도메인에 id를 유지한 채 학원을 바꾸는 메서드를 두면 의도가 드러남
