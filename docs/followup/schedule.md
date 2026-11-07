# FOLLOWUP · 일정 (schedule)

- 생성: 2026-11-07 23:49 KST · tdd-guard
- 상태: 진행 중 (체크 11/55 · 완료 기준 0/4)
- 테스트: ● GREEN · 779 passed · 전체 · 81s · cd C:/pickup && ./gradlew.bat test --console=plain > "C:/Users/keuye/AppData/Local/Temp/claude/86b70dd9bf0b8321/08c3708…
- 설계서: C:/pickup/docs/06_일정-schedule.md

## 완료 항목
- [x] #1 [기반] PickupSchedule은 학원 출입구가 없으면 DRAFT, 있으면 READY로 만들어진다 ← PickupScheduleTest.학원 출입구가 없으면 PickupSchedule은 DRAFT로 만들어진다
- [x] #2 [기반] 일정 규칙은 targetPickup이 0~60을 벗어나면 거절한다 ← PickupScheduleTest.[2] targetPickup = 61
- [x] #3 [기반] 일정 규칙은 weekdays가 비어 있으면 거절한다 ← PickupScheduleTest.일정 규칙은 weekdays가 비어 있으면 422 VALIDATION_FAILED(field weekdays)로 거절한다
- [x] #4 [기반] 겹침 규칙은 같은 자녀 · 같은 요일 · 같은 classEndTime 일정을 겹침으로 판정한다 ← ScheduleOverlapRuleTest.겹침 규칙은 같은 자녀 · 같은 요일 · 같은 classEndTime 일정을 겹침으로 판정한다
- [x] #5 [기반] 하루 한도 규칙은 같은 자녀 같은 요일에 네 번째 일정이면 한도 초과로 판정한다 ← DailyScheduleLimitRuleTest.하루 한도 규칙은 같은 자녀 같은 요일에 네 번째 일정이면 한도 초과로 판정한다
- [x] #6 [기반] targetPickupTime 계산은 classEndTime 18:00 · targetPickup 5에서 18:05를 돌려준다 ← PickupScheduleTest.targetPickupTime 계산은 classEndTime 18:00 · targetPickup 5에서 18:05를 돌려준다
- [x] #7 [기반] 이름 마스킹은 김보호를 김*호로 바꾼다 ← GuardianNameMaskerTest.이름 마스킹은 김보호를 김*호로 바꾼다
- [x] #8 [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다 ← ScheduleInPortsTest.핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] #9 [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다 ← ScheduleOutPortsTest.핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] #10 [기반] AcademyEntity · PickupScheduleEntity가 BaseTimeEntity를 상속하고 location · entrance는 geography(Point,4326)로 매핑된다 ← SchedulePersistenceEntitiesTest.AcademyEntity · PickupScheduleEntity는 @Entity이고 BaseTimeEntity를 바로 상속한다
- [x] #11 [기반] 테스트 시드로 학원 1곳(해솔영어학원, 출입구 있음)과 자녀 민준의 월 · 수 18:00 일정 1개를 만든다 ← ScheduleSeedTest.테스트 시드로 학원 1곳(해솔영어학원, 출입구 있음)과 자녀 민준의 월 · 수 18:00 일정 1개를 만든다

## 미완료 항목
- [ ] #12 [persistence] Academy를 저장하고 읽으면 location · entrance 좌표가 같다
- [ ] #13 [persistence] weekdays [MON, WED, FRI]를 저장하면 컬럼에 MON,WED,FRI로 들어가고 같은 목록으로 읽힌다
- [ ] #14 [persistence] classEndTime 18:00을 저장하면 시간대 변환 없이 18:00으로 읽힌다
- [ ] #15 [persistence] loadSchedulesOfChild는 그 자녀의 일정만 돌려준다
- [ ] #16 [persistence] loadSchedulesOfChildren은 여러 자녀의 일정을 한 번에 돌려준다
- [ ] #17 [서비스] 학원 검색은 외부 지도 API 결과를 저장하지 않고 externalPlaceId · name · address · location으로 돌려준다
- [ ] #18 [서비스] 학원 검색은 외부 지도 API가 실패하면 MAP_API_UNAVAILABLE로 거절한다
- [ ] #19 [서비스] 일정 등록은 학원을 만들고 scheduleId · academyId · status를 돌려준다
- [ ] #20 [서비스] 일정 등록은 출입구 없는 학원이면 status DRAFT를 돌려준다
- [ ] #21 [서비스] 일정 등록은 childId가 CHILD가 아니면 NOT_FOUND로 거절한다
- [ ] #22 [서비스] 일정 등록은 다른 가족의 자녀면 NOT_FOUND로 거절한다
- [ ] #23 [서비스] 일정 등록은 겹치는 일정이 있으면 SCHEDULE_OVERLAP과 겹치는 scheduleId 목록으로 거절한다
- [ ] #24 [서비스] 일정 등록은 같은 요일 일정이 이미 3개면 DAILY_SCHEDULE_LIMIT으로 거절한다
- [ ] #25 [서비스] 일정 수정은 자기 자신과는 겹침으로 판정하지 않는다
- [ ] #26 [서비스] 일정 수정은 요일 · 시간 · 학원을 바꾸고 status를 다시 판정한다
- [ ] #27 [서비스] 설정 요약은 자녀 기기가 CONNECTED가 아니면 missing에 CHILD_DEVICE를 넣는다
- [ ] #28 [서비스] 설정 요약은 READY 일정이 없으면 missing에 SCHEDULE을 넣는다
- [ ] #29 [서비스] 설정 요약은 일정 학원에 기본 장소가 없으면 missing에 PICKUP_SPOT을 넣는다
- [ ] #30 [서비스] 설정 요약은 joinCallMode와 Family.settingsVersion을 담는다
- [ ] #31 [서비스] 설정 완료는 settingsVersion이 다르면 SETTINGS_VERSION_CONFLICT로 거절한다
- [ ] #32 [서비스] 설정 완료는 missing이 남아 있으면 SETUP_INCOMPLETE로 거절한다
- [ ] #33 [서비스] 주간 일정은 weekStart가 없으면 한국 날짜 기준 이번 주 월요일부터 7일을 돌려준다
- [ ] #34 [서비스] 주간 일정은 요일이 맞는 날짜에만 그 일정을 넣는다
- [ ] #35 [서비스] 주간 일정 readiness는 자녀 기기나 기본 장소가 빠지면 startable false와 missing을 돌려준다
- [ ] #36 [서비스] 차량용 설정 요약은 GUARDIAN 이름을 가려서 돌려주고 자녀 목록과 joinCallMode를 담는다
- [ ] #37 [서비스] findSchedules는 scheduleId 목록의 일정과 학원 정보를 돌려준다
- [ ] #38 [web] GET /v1/places/academies는 200과 items[]를 돌려준다
- [ ] #39 [web] GET /v1/places/academies에 1자 query를 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #40 [web] GET /v1/places/academies가 외부 API 오류면 502 MAP_API_UNAVAILABLE을 돌려준다
- [ ] #41 [web] POST /v1/children/{childId}/schedules는 201과 scheduleId · academyId · status를 돌려준다
- [ ] #42 [web] POST /v1/children/{childId}/schedules에 targetPickup 61을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #43 [web] POST /v1/children/{childId}/schedules가 겹치면 409 SCHEDULE_OVERLAP과 overlapScheduleIds를 돌려준다
- [ ] #44 [web] POST /v1/children/{childId}/schedules가 하루 한도를 넘으면 422 DAILY_SCHEDULE_LIMIT을 돌려준다
- [ ] #45 [web] PUT /v1/schedules/{scheduleId}는 200과 scheduleId · academyId · status를 돌려준다
- [ ] #46 [web] PUT /v1/schedules/{scheduleId}를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #47 [web] GET /v1/children/{childId}/schedules는 200과 items[]를 돌려준다
- [ ] #48 [web] GET /v1/children/{childId}/setup-summary는 200과 child · schedules · pickupSpots · joinCallMode · missing · settingsVersion을 돌려준다
- [ ] #49 [web] POST /v1/children/{childId}/setup-completion은 200과 syncedToVehicle을 돌려준다
- [ ] #50 [web] POST /v1/children/{childId}/setup-completion이 버전 충돌이면 409 SETTINGS_VERSION_CONFLICT를 돌려준다
- [ ] #51 [web] POST /v1/children/{childId}/setup-completion이 누락이 있으면 422 SETUP_INCOMPLETE를 돌려준다
- [ ] #52 [web] GET /v1/vehicles/me/weekly-pickups는 차량 토큰으로 200과 weekStart · days · readiness를 돌려준다
- [ ] #53 [web] GET /v1/vehicles/me/weekly-pickups를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #54 [web] GET /v1/vehicles/me/settings-summary는 차량 토큰으로 200과 guardianNameMasked · children · joinCallMode를 돌려준다
- [ ] #55 [web] Idempotency-Key는 POST /v1/children/{childId}/schedules · POST /v1/children/{childId}/setup-completion에만 요구된다

## 완료 기준 (공통 규칙 문서)
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 다시 리뷰 필요: 2026-10-28 21:19 KST 리뷰 뒤에 체크 · 추가된 항목 #1, #2, #3, #4, #5, #6, #7, #8, #9, #10, #11, #12, #13, #14, #15, #16, #17, #18, #19, #20, #21, #22, #23, #24, #25, #26, #27, #28, #29, #30, #31, #32, #33, #34, #35, #36, #37, #38, #39, #40, #41, #42, #43, #44, #45, #46, #47, #48, #49, #50, #51, #52, #53, #54, #55
- 2026-10-28 21:19 KST · 도메인 리뷰어 · 결론 조건부 통과 · 상 0 · 중 0 · 하 6 · 리뷰 시점 체크 15/15
- 리뷰 시점 테스트: ● GREEN · 1194 passed · 1 skipped · 전체 · 2.2s · cd /c/designdot/designdot-server && ./gradlew.bat test --console=plain --rerun 2>&1 | grep -E "BUILD|FAILED|> Task :tes…
  - 결론: 조건부 통과
  - (상 0 · 중 0 · 하 6 · 리뷰 시점 체크 15/15 · 완료 기준 5/8. 조건: 커밋 전 [하]③ 되돌리기 · [하]② 기록 정정, [하]① CalendarConfig 바인딩 테스트를 항목으로 올릴지 결정)
  - [하] CalendarConfig 바인딩 회귀 보호 없음 — CalendarConfig.java:16이 유일한 바인딩 지점인데 src/test에 CalendarConfig 테스트 0건, application*.yml · test resources 어디에도 week-start 키 없음. 키 오타 · 이름 변경 시 기본 MONDAY로 조용히 떨어져 어떤 테스트도 실패하지 않음 → 항목 추가 권고(테스트만: ApplicationContextRunner + CalendarConfig로 SUNDAY → SUNDAY, 생략 → MONDAY, 소문자 sunday → SUNDAY, xyz → 기동 실패). application.yml 명시 여부는 사람 결정
  - [하] 진행 기록과 실제 상태 불일치 — REVIEW.md:4 · schedule.md:41 "리뷰 시점 테스트: ● RED · 0 passed · 4 failed"는 2차 리뷰어의 의도된 xyz 기동 실패 실험이 마지막 실행으로 기록된 것. 실제는 1195 · 실패 0 · 스킵 1 GREEN → note(기록 정정)
  - [하] schedule/adapter/in/web/dto/package-info.java:1 Javadoc 여는 줄 앞 공백 4칸(이번 작업에서 생긴 변경), 빌드 영향 없음 → 리팩터링(커밋 전 되돌리기)
  - [하] archfixture ServiceUsingAutowired의 불필요한 school 참조 남음(ServiceUsingAutowired.java:3), 판정 영향 없음 → 리팩터링
  - [하] 01:305 다이어그램 라벨 `sc1(CalendarGrid) -->|"Period.MAX_DAYS · dDayFrom"|` — dDayFrom 호출자는 GetUpcomingExhibitionsService, 라벨만 보면 도메인→도메인 참조로 읽힘(06:23은 정확) → note
  - [하] PublicAccess POST 403 단언 중복(ScheduleControllerTest:640-661과 파라미터화 8행 중 POST 2행) → note(실패 위치 좁힘 용도면 유지)
  - 점검 요약: (1) #13 충족 — 픽스처 4개 @Service 없음, AllowedTransactionalService는 Service.class 상수로 stereotype 참조 유지. ArchfixtureIsolationTest는 HealthCheckTest · ApiDocsTest와 애노테이션 동일, 전체 실행 로그에서 같은 설정 @SpringBootTest 8개 중 컨텍스트 기동 로그 1건뿐 → 새 컨텍스트 없음. ArchitectureTest 25/25, applicationSpringRuleCatchesValueAutowiredAndEventPublisherInFixtures 위반 3 · 허용 2 유지. @Service 상태였다면 isEmpty() 실패하므로 우연 통과 아님. 02:639 · p…
  - 완료 기준:
  - C1 충족 — CalendarGrid는 java.time + Period.MAX_DAYS(허용 ③)만, domainIsPureJava ok
  - C2 충족 — UseCase 2 · Result 3 · LoadSchedulePort · Row 2 · 서비스 2
  - C3 충족(해당 없음 명시) — QueryDSL 어댑터, 자체 엔티티 · 시드 · ops/ddl 없음, 쿼리 1회 #15, 인덱스는 2차 EXPLAIN
  - C4 충족 — Controller · ApiDocs · dto 3
  - C5 충족 — schedule 120건 + ArchitectureTest 25 · ArchfixtureIsolationTest 1 · ApiDocsTest 13 · PeriodTest 24. 전체 1195 · 실패 0 · 스킵 1. 단 CalendarConfig 바인딩 테스트 없음
  - C6 사람 확인 필요 — Swagger 완료, 프론트 공유 · 주 시작 요일 미확인
  - C7 미충족 — 커밋 0, dev 배포 전
  - C8 미충족 — 원격 브랜치 · PR 없음
  - 후속 과제: 08 접두어 남음 / 2차 중간 ① 해소 · ② 설계 결정 필요 · ③ 해소 · ④ 남음 · ⑤ 의도적 유지 · ⑥ 남음 / 도메인 리뷰(10-28) ① 해소 · ② 남음 · ③ 남음 · ④ ⑤ 해소(#14 · #15) · ⑥ note 유지 · ⑦ 남음 · ⑧ 해소 / 정정 note ① ② 남음 · ③ 사람 결정 · ④ 해소(01:305 라벨 위치는 위 [하])
  - 테스트: ./gradlew.bat test --console=plain --rerun → BUILD SUCCESSFUL in 1m 8s, tests=1195 · failures=0 · errors=0 · skipped=1(SchemaDdlExportTest). 코드 · 테스트 · 설계서 수정 없음
- 이전 리뷰: 2026-10-28 20:31 KST (조건부 통과), 2026-10-28 00:38 KST (조건부 통과)

## 확인 필요 사항 (설계서)
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

## 다음 단계
- 남은 항목 44건부터 계속: /tdd domain schedule → /tdd go
- 다음 도메인: 장소 (07_장소-place.md). 시작: /tdd domain place

## 후속 과제
- [하] 문서 설정 키 접두어 불일치: 08 문서는 `designdot.jwt.secret` · `designdot.admin.*`, 실제 코드는 `designmodu.*`. 06 week-start는 `designmodu.calendar.week-start`로 맞춤(일정 #1, 2026-10-27). 08 문서 표기 정리 필요
- [하] 일정 2차 중간 리뷰(2026-10-28): ① ArchitectureTest의 applicationDoesNotDependOnAdaptersOrTechnology와 새 applicationUsesOnlyServiceAndTransactionalFromSpring이 spring web · data 부분 중복(둘 다 유지), 02 문서 ArchUnit 예시 코드(189–297행)에 새 규칙 미반영 ② `designmodu.calendar.week-start`가 application.yml에 미기재, 대소문자 구분(sunday → 기동 실패), CalendarConfig 바인딩 전용 테스트 없음 ③ MonthlyScheduleResponse.weekStart Swagger enum 값 의미 설명 누락(02 "enum은 모든 값 의미 나열") ④ archfixture ServiceUsingAutowired의 불필요한 school 참조 ⑤ SchedulePersistenceAdapterTest · ApiDocsTest 일부 중복 단언(실패 위치 좁힘 용도로 유지) ⑥ SchoolPersistenceTestSupport · ExhibitionPersistenceTestSupport region · school · major 헬퍼 중복
- [하] 일정 도메인 리뷰(2026-10-28) 남은 과제: ① 2차 리뷰 ②의 "sunday → 기동 실패"는 Boot 4.1.1 LenientStringToEnumConverterFactory 때문에 틀렸을 수 있음(실기동 미확인) — CalendarConfig 바인딩 테스트 · application.yml 기재와 함께 확인 ② ApiDocsTest에 UpcomingExhibitionResponse 스키마 속성 이름 "dDay" 단언 없음 ③ GetMonthlyScheduleUseCase(Integer, Integer) → MonthlyScheduleQuery 또는 Optional<YearMonth>로 불변 조건을 유스케이스 입력에 담는 리팩터링 ④ ScheduleControllerTest · ExhibitionControllerTest에 공개 GET 허용 · POST 403 케이스 없음(02 626행) ⑤ SchedulePersistenceAdapterTest 쿼리 수 단언 없음(02 625행, PreparedStatementCounter 재사용) ⑥ GetUpcomingExhibitionsService가 행마다 Period.of 검증 → 잘못된 기간 행이면 GET 400 INVALID_PERIOD(DB CHECK로 위험 낮음) ⑦ ScheduleApiDocs 월간 400 설명에 "숫자 아님" 누락 ⑧ 월간 · 다가오는 조회 EXPLAIN으로 idx_exhibition_start_date_id 사용 확인. 문서 정합(02 패키지 트리 · ArchUnit 예시, 06 의존 표)은 리뷰 직후 반영함
- [정정 · 하] 일정 재리뷰(2026-10-28): 2차 중간 리뷰 ②의 "sunday → 기동 실패"는 틀림 — DESIGNMODU_CALENDAR_WEEK_START=sunday로 실기동 통과(Boot 관대한 enum 변환), xyz만 실패. 남은 것: CalendarConfig 바인딩 전용 테스트 없음, `designmodu.calendar.week-start`를 application.yml에 명시할지(설계 결정). 추가 [하]: ① ApiDocsTest에 UpcomingExhibitionResponse 스키마 속성 "dDay" 단언 없음 ② ScheduleApiDocs · ExhibitionApiDocs(hot limit) 400 설명에 "숫자 아님" 누락 — 두 도메인 함께 정리 ③ archunit.properties failOnEmptyShould=false 유지 중(픽스처 있는 규칙 5개뿐, 나머지는 패턴 오타 시 빈 검사 통과 가능) — true 전환은 전 도메인 공통 설계 결정 ④ 01 305행 · 06 23행 다이어그램은 리뷰 직후 Period.of · dDayFrom 반영함. EXPLAIN은 확인 완료(idx_exhibition_start_date_id 사용, searchFrom 하한 효과 확인)
- [하] 일정 3차 리뷰(2026-10-28): ① CalendarConfig 바인딩 테스트 없음 — 항목 추가 여부는 사람 결정(ApplicationContextRunner로 SUNDAY · 생략 → MONDAY · 소문자 sunday · xyz 기동 실패, application.yml 명시 여부와 함께) ② schedule/adapter/in/web/dto/package-info.java Javadoc 앞 공백 4칸 — /tdd pkginfo delete로 package-info를 지우면 함께 해소, 지우지 않으면 커밋 전 되돌리기 ③ ServiceUsingAutowired의 school ResolveRegionIdsUseCase 참조 정리 남음 ④ PublicAccess POST 403 단언 중복(실패 위치 좁힘 용도로 유지) ⑤ REVIEW.md · schedule.md의 리뷰 시점 테스트 RED 기록은 3차 리뷰 기록으로 GREEN(1194 passed · 1 skipped)으로 갱신됨 ⑥ 01:305 다이어그램 라벨은 리뷰 직후 "Period.MAX_DAYS (서비스는 dDayFrom도)"로 정정함
- schedule: 이름 마스킹 1자 이름 · 빈 문자열 규칙이 설계서에 없음. 지금은 그대로 돌려줌(GuardianNameMasker). 규칙 확정 필요
- schedule: DeviceStatus를 auth.domain.MemberDeviceStatus 재사용 대신 schedule.domain.DeviceStatus로 따로 둠(어댑터에서 변환). joinCallMode는 user.domain.JoinCallMode 재사용. SpotSummary에 academyId 추가(PICKUP_SPOT 학원별 판정용). ScheduleInfo에 targetPickupTime 없음(pickup이 계산)
- 환경: 시스템(Machine) PATH에 줄바꿈이 섞인 항목 "C:\Program⏎ Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin"이 있음. Docker가 꺼져 있으면 Testcontainers가 PATH를 읽다 InvalidPathException. 사람이 시스템 PATH를 고쳐야 함
- schedule: pickup_schedule.child_user_id → users, academy_id → academy는 설계서상 FK지만 DB FK 없이 Long 칼럼만 둠(user · auth와 같은 관례). DB FK가 필요하면 schema.sql로 추가할지 결정 필요
