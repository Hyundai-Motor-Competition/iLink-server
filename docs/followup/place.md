# FOLLOWUP · 장소 (place)

- 생성: 2026-11-08 09:22 KST · tdd-guard
- 상태: 진행 중 (체크 58/58 · 완료 기준 0/4)
- 테스트: ● GREEN · 1141 passed · 전체 · 66s · cd C:/pickup && ./gradlew.bat test --console=plain > "C:/Users/keuye/AppData/Local/Temp/claude/86b70dd9bf0b8321/08c3708…
- 설계서: C:/pickup/docs/07_장소-place.md

## 완료 항목
- [x] #1 [기반] 기본 장소 규칙은 가족 · 학원의 첫 장소를 isDefault true로 만든다 ← DefaultSpotRuleTest.기본 장소 규칙은 가족 · 학원의 첫 장소를 isDefault false로 보내도 isDefault true로 만든다
- [x] #2 [기반] 기본 장소 규칙은 새 기본 장소를 지정하면 같은 가족 · 학원의 이전 기본 장소를 false로 바꾼다 ← DefaultSpotRuleTest.기본 장소 규칙은 새 기본 장소를 지정하면 같은 가족 · 학원의 이전 기본 장소를 false로 바꿔 돌려준다
- [x] #3 [기반] 삭제 규칙은 같은 학원에 다른 장소가 남아 있는 기본 장소의 삭제를 거절한다 ← SpotDeletionRuleTest.삭제 규칙은 같은 학원에 다른 장소가 남아 있는 기본 장소의 삭제를 거절한다
- [x] #4 [기반] 삭제 규칙은 학원에 하나 남은 기본 장소의 삭제를 허용한다 ← SpotDeletionRuleTest.삭제 규칙은 학원에 하나 남은 기본 장소의 삭제를 허용한다
- [x] #5 [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다 ← PlaceInPortsTest.핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] #6 [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다 ← PlaceOutPortsTest.핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] #7 [기반] PickupSpotEntity가 BaseTimeEntity를 상속하고 stopPoint · meetingPoint는 geography(Point,4326)로 매핑된다 ← PlacePersistenceEntitiesTest.PickupSpotEntity는 @Entity이고 BaseTimeEntity를 바로 상속한다
- [x] #8 [기반] 테스트 시드로 해솔영어학원의 기본 장소 정문 앞과 다른 장소 1개를 만든다 ← PlaceSeedTest.테스트 시드로 해솔영어학원의 기본 장소 정문 앞과 다른 장소 1개를 만든다
- [x] #9 [persistence] PickupSpot을 저장하고 읽으면 stopPoint · meetingPoint의 lat · lng가 소수점 6자리까지 같다 ← PlacePersistenceAdapterTest.PickupSpot을 저장하고 읽으면 stopPoint · meetingPoint의 lat · lng가 소수점 6자리까지 같다
- [x] #10 [persistence] stop_point 컬럼에 GIST 인덱스가 있다 ← PlacePersistenceEntitiesMappingTest.서버가 뜨면 pickup_spot.stop_point에 GIST 인덱스가 만들어진다
- [x] #11 [persistence] loadSpots는 그 가족의 장소만 돌려주고 academyId를 주면 그 학원 장소만 돌려준다 ← PlacePersistenceAdapterTest.loadSpots는 academyId를 주면 그 가족의 그 학원 장소만 돌려준다
- [x] #12 [persistence] loadSpotsWithin은 near에서 radiusM 안에 정차 지점이 있는 장소만 돌려준다 ← PlacePersistenceAdapterTest.loadSpotsWithin은 near에서 radiusM 안에 정차 지점이 있는 장소만 돌려준다: 499m 안 · 501m 밖, 만남 지점만 안이면 빼고, 다른 가족 장소는 뺀다
- [x] #13 [persistence] isWithinMeters는 학원에서 499m인 좌표에 true, 501m인 좌표에 false를 돌려준다 ← PostgisDistanceAdapterTest.isWithinMeters는 학원에서 499m인 좌표에 true, 501m인 좌표에 false를 돌려준다
- [x] #14 [persistence] loadDefaultSpot은 가족 · 학원의 isDefault true 장소를 돌려준다 ← PlacePersistenceAdapterTest.loadDefaultSpot은 가족 · 학원의 isDefault true 장소를 돌려주고 다른 가족 · 학원의 기본 장소는 돌려주지 않는다
- [x] #15 [서비스] 장소 추가는 학원의 첫 장소면 isDefault false로 보내도 isDefault true로 저장한다 ← AddPickupSpotServiceTest.장소 추가는 학원의 첫 장소면 isDefault false로 보내도 isDefault true로 저장한다
- [x] #16 [서비스] 장소 추가는 isDefault true면 같은 학원의 이전 기본 장소를 해제한다 ← AddPickupSpotServiceTest.장소 추가는 isDefault true면 같은 학원의 이전 기본 장소를 해제한다
- [x] #17 [서비스] 장소 추가는 정차 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다 ← AddPickupSpotServiceTest.장소 추가는 정차 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [x] #18 [서비스] 장소 추가는 만남 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다 ← AddPickupSpotServiceTest.장소 추가는 만남 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [x] #19 [서비스] 장소 추가는 호출한 보호자를 createdBy로 저장한다 ← AddPickupSpotServiceTest.장소 추가는 호출한 보호자를 createdBy로 저장한다
- [x] #20 [서비스] 장소 수정은 보낸 필드만 바꾼다 ← UpdatePickupSpotServiceTest.장소 수정은 이름만 보내면 이름만 바꾸고 나머지 필드는 그대로 둔다
- [x] #21 [서비스] 장소 수정은 바꾼 좌표가 500m를 넘으면 SPOT_TOO_FAR로 거절한다 ← UpdatePickupSpotServiceTest.장소 수정은 바꾼 정차 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [x] #22 [서비스] 장소 수정은 isDefault true로 바꾸면 같은 학원의 이전 기본 장소를 해제한다 ← UpdatePickupSpotServiceTest.장소 수정은 isDefault true로 바꾸면 같은 학원의 이전 기본 장소를 해제한다
- [x] #23 [서비스] 장소 삭제는 진행 중 세션이 쓰는 장소면 SPOT_IN_USE로 거절한다 ← DeletePickupSpotServiceTest.장소 삭제는 진행 중 세션이 쓰는 장소면 SPOT_IN_USE로 거절한다
- [x] #24 [서비스] 장소 삭제는 다른 장소가 남은 기본 장소면 DEFAULT_SPOT_REQUIRED로 거절한다 ← DeletePickupSpotServiceTest.장소 삭제는 다른 장소가 남은 기본 장소면 DEFAULT_SPOT_REQUIRED로 거절한다
- [x] #25 [서비스] 다른 가족의 spotId를 수정 · 삭제하려 하면 NOT_FOUND로 거절한다 ← DeletePickupSpotServiceTest.다른 가족의 spotId를 삭제하려 하면 NOT_FOUND로 거절한다
- [x] #26 [서비스] 장소 조회는 학원별로 묶어 academyName과 함께 돌려준다 ← ListPickupSpotsServiceTest.장소 조회는 학원별로 묶어 academyName과 함께 academyId · spotId 오름차순으로 돌려준다
- [x] #27 [서비스] 장소 조회는 진행 중 세션이 없으면 walkMinutes를 null로 돌려준다 ← ListPickupSpotsServiceTest.장소 조회는 진행 중 세션이 없으면 모든 장소의 walkMinutes를 null로 돌려준다
- [x] #28 [서비스] findDefaultSpot은 가족 · 학원의 기본 장소를 돌려주고 없으면 빈 값을 돌려준다 ← LoadPickupSpotServiceTest.findDefaultSpot은 가족 · 학원의 기본 장소를 SpotInfo로 돌려준다
- [x] #29 [web] GET /v1/families/me/pickup-spots는 200과 academies[]를 돌려준다 ← PickupSpotControllerTest.GET /v1/families/me/pickup-spots는 200과 academies[](academyId · academyName · spots[])를 isDefault 키와 null walkMinutes 그대로 돌려준다
- [x] #30 [web] GET /v1/families/me/pickup-spots는 차량 토큰으로도 200을 돌려준다 ← PickupSpotControllerTest.GET /v1/families/me/pickup-spots는 차량 토큰으로도 200과 academies[]를 돌려준다
- [x] #31 [web] GET /v1/families/me/pickup-spots에 near만 보내고 radiusM을 빼면 422 VALIDATION_FAILED를 돌려준다 ← PickupSpotControllerTest.GET /v1/families/me/pickup-spots에 near만 보내고 radiusM을 빼면 422 VALIDATION_FAILED와 fields의 radiusM을 돌려주고 유스케이스를 부르지 않는다
- [x] #32 [web] GET /v1/families/me/pickup-spots에 radiusM 3001을 보내면 422 VALIDATION_FAILED를 돌려준다 ← PickupSpotControllerTest.[2] radiusM = 3001
- [x] #33 [web] GET /v1/families/me/pickup-spots를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← PickupSpotControllerTest.GET /v1/families/me/pickup-spots를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다
- [x] #34 [web] POST /v1/families/me/pickup-spots는 201과 spotId · isDefault를 돌려준다 ← PickupSpotControllerTest.POST /v1/families/me/pickup-spots는 201과 spotId · isDefault를 돌려준다
- [x] #35 [web] POST /v1/families/me/pickup-spots에 51자 name을 보내면 422 VALIDATION_FAILED를 돌려준다 ← PickupSpotControllerTest.POST /v1/families/me/pickup-spots에 51자 name을 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려주고 유스케이스를 부르지 않는다
- [x] #36 [web] POST /v1/families/me/pickup-spots가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다 ← PickupSpotControllerTest.POST /v1/families/me/pickup-spots가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다
- [x] #37 [web] POST /v1/families/me/pickup-spots를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← PickupSpotControllerTest.POST /v1/families/me/pickup-spots를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다
- [x] #38 [web] PATCH /v1/pickup-spots/{spotId}는 200과 spotId · name · stopPoint · meetingPoint · memo · isDefault를 돌려준다 ← PickupSpotControllerTest.PATCH /v1/pickup-spots/{spotId}는 200과 spotId · name · stopPoint · meetingPoint · memo · isDefault를 돌려준다
- [x] #39 [web] PATCH /v1/pickup-spots/{spotId}가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다 ← PickupSpotControllerTest.PATCH /v1/pickup-spots/{spotId}가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다
- [x] #40 [web] DELETE /v1/pickup-spots/{spotId}는 204를 돌려준다 ← PickupSpotControllerTest.DELETE /v1/pickup-spots/{spotId}를 CO_GUARDIAN 토큰으로 부르면 204를 돌려준다
- [x] #41 [web] DELETE /v1/pickup-spots/{spotId}가 진행 중 세션의 장소면 409 SPOT_IN_USE를 돌려준다 ← PickupSpotControllerTest.DELETE /v1/pickup-spots/{spotId}가 진행 중 세션의 장소면 409 SPOT_IN_USE를 돌려준다
- [x] #42 [web] DELETE /v1/pickup-spots/{spotId}가 다른 장소가 남은 기본 장소면 422 DEFAULT_SPOT_REQUIRED를 돌려준다 ← PickupSpotControllerTest.DELETE /v1/pickup-spots/{spotId}가 다른 장소가 남은 기본 장소면 422 DEFAULT_SPOT_REQUIRED를 돌려준다
- [x] #43 [web] Idempotency-Key는 POST /v1/families/me/pickup-spots · DELETE /v1/pickup-spots/{spotId}에만 요구된다 ← PlaceIdempotencyKeyTest.Idempotency-Key는 POST /v1/families/me/pickup-spots · DELETE /v1/pickup-spots/{spotId}에만 요구된다
- [x] #44 [persistence] pickup 구현 전 임시 CheckActivePickupPort는 isUsedByActiveSession false, loadActiveChildLocation 빈 값을 돌려준다 ← PlaceModuleLinkAdaptersTest.pickup 구현 전 임시 CheckActivePickupPort는 isUsedByActiveSession false, loadActiveChildLocation 빈 값을 돌려준다
- [x] #45 [persistence] LoadAcademyPort 어댑터는 schedule의 LoadAcademyUseCase.findAcademy 결과를 place가 쓰는 학원 정보(academyId · name · location)로 바꿔 돌려주고 없으면 빈 값을 돌려준다 ← PlaceModuleLinkAdaptersTest.LoadAcademyPort 어댑터는 schedule의 LoadAcademyUseCase.findAcademy 결과를 place 학원 정보(academyId · name · location)로 바꿔 돌려준다
- [x] #46 [서비스] findSpot은 spotId의 장소를 돌려주고 없으면 빈 값을, findSpots는 가족의 학원 목록에 속한 장소를 spotId · academyId · name · isDefault로 돌려준다 ← LoadPickupSpotServiceTest.findSpots는 가족의 학원 목록에 속한 장소만 spotId · academyId · name · isDefault로 돌려주고 장소 조회는 1회만 한다
- [x] #47 schedule의 임시 LoadPickupSpotPort를 place findSpots 연동으로 바꿔 설정 요약 · 주간 일정의 PICKUP_SPOT 판정이 실제 장소를 본다 ← ScheduleModuleLinkAdaptersTest.설정 요약은 시드 가족 · 민준의 해솔영어학원에 기본 장소가 있으면 missing에 PICKUP_SPOT을 넣지 않고 그 장소를 보여준다
- [x] #48 [서비스] 장소 수정은 기본 장소를 isDefault false로 바꾸려 하면 DEFAULT_SPOT_REQUIRED로 거절한다(다른 장소를 기본으로 지정해야 바뀜) ← UpdatePickupSpotServiceTest.장소 수정은 기본 장소를 isDefault false로 바꾸려 하면 DEFAULT_SPOT_REQUIRED로 거절한다
- [x] #49 [persistence] deleteSpot은 그 장소 행만 지우고 같은 가족 · 학원의 다른 장소는 남긴다 ← PlacePersistenceAdapterTest.deleteSpot은 그 장소 행만 지우고 같은 가족 · 학원의 다른 장소는 남긴다
- [x] #50 장소 삭제는 진행 중 세션이 쓰는 기본 장소면 같은 학원에 다른 장소가 남아 있어도 SPOT_IN_USE를 먼저 돌려준다 ← DeletePickupSpotServiceTest.장소 삭제는 진행 중 세션이 쓰는 기본 장소면 같은 학원에 다른 장소가 남아 있어도 SPOT_IN_USE를 먼저 돌려준다
- [x] #51 장소 수정은 좌표를 바꿀 때 학원 정보가 없으면 NOT_FOUND로 거절하고 저장하지 않는다 ← UpdatePickupSpotServiceTest.장소 수정은 좌표를 바꿀 때 학원 정보가 없으면 NOT_FOUND로 거절하고 저장하지 않는다
- [x] #52 [web] GET /v1/families/me/pickup-spots에 lat,lng 형식이 아닌 near(abc · 37.5)를 보내면 422 VALIDATION_FAILED와 fields의 near를 돌려준다 ← PickupSpotControllerTest.[1] near = "abc"
- [x] #53 [서비스] 장소 조회는 학원 정보가 없는 학원의 장소도 academyName null로 묶어 돌려준다 ← ListPickupSpotsServiceTest.장소 조회는 학원 정보가 없는 학원의 장소도 academyName null로 묶어 돌려준다
- [x] #54 [서비스] 장소 수정은 기본 장소를 isDefault false로 바꾸면서 500m 밖 좌표를 보내도 DEFAULT_SPOT_REQUIRED로 먼저 거절하고 거리 검사를 하지 않는다 ← UpdatePickupSpotServiceTest.장소 수정은 기본 장소를 isDefault false로 바꾸면서 500m 밖 좌표를 보내도 DEFAULT_SPOT_REQUIRED로 먼저 거절하고 거리 검사를 하지 않는다
- [x] #55 [persistence] saveSpot으로 spotId가 있는 장소를 다시 저장하면 새 행을 만들지 않고 그 행의 이름 · 좌표 · isDefault가 바뀐다 ← PlacePersistenceAdapterTest.saveSpot으로 spotId가 있는 장소를 다시 저장하면 새 행을 만들지 않고 그 행의 이름 · 좌표 · isDefault가 바뀐다
- [x] #56 pickup_spot에 (family_id, academy_id) WHERE is_default 부분 유니크 인덱스가 있어 같은 가족 · 학원에 기본 장소 두 개를 저장할 수 없다 ← PlacePersistenceEntitiesMappingTest.서버가 뜨면 pickup_spot에 (family_id, academy_id) WHERE is_default 부분 유니크 인덱스가 만들어진다
- [x] #57 장소 추가 · 수정에서 isDefault true로 지정하면 이전 기본 장소를 먼저 해제한 뒤 저장해 유니크 인덱스와 부딪히지 않는다(실제 DB로 확인) ← AddPickupSpotServiceIntegrationTest.기본 장소가 있는 학원에 isDefault true로 장소를 추가하면 유니크 인덱스와 부딪히지 않고 새 장소만 기본이 되고 이전 기본은 false가 된다
- [x] #58 동시 요청으로 기본 장소 유니크 인덱스가 부딪히면 409 DEFAULT_SPOT_CONFLICT로 거절한다 ← AddPickupSpotServiceTest.장소 추가는 저장소가 기본 장소 충돌을 알리면 409 DEFAULT_SPOT_CONFLICT로 거절한다

## 미완료 항목
- 없음

## 완료 기준 (공통 규칙 문서)
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 다시 리뷰 필요: 2026-11-08 03:42 KST 리뷰 뒤에 체크 · 추가된 항목 #52, #53, #54, #55, #56, #57, #58
- 2026-11-08 03:42 KST · 도메인 리뷰어 · 결론 조건부 통과 · 상 0 · 중 4 · 하 10 · 리뷰 시점 체크 51/51
- 리뷰 시점 테스트: ● GREEN · 1127 passed · 전체 · 1.7s · cd C:/pickup && ./gradlew.bat test --rerun --console=plain > "C:/Users/keuye/AppData/Local/Temp/claude/86b70dd9bf0b8321…
  - 결론: 조건부 통과
  - [중] SPOT-01 near 형식 오류 → 422 테스트 없음, lat · lng 범위 검사 없음(PickupSpotController.java:104-117, "NaN,NaN" · "91,200" 통과 → PostGIS 오류 500 가능, 미확인) → 항목 추가(형식 오류 422), 범위는 공통 LatLng 후속 과제
  - [중] Swagger 삭제 설명 「soft delete」가 실제(행 삭제)와 반대 — PickupSpotApiDoc.java:57 → 리팩터링
  - [중] 08 설계서 L38 「spotId FK → PickupSpot」이 07 · 관례(DB FK 없음)와 어긋남, FOLLOWUP 표 첫 행도 「FK라 못 지움」 → 사람 결정(pickup 시작 전)
  - [중] 쓰지 않는 loadActiveChildLocation 호출 남음 — ListPickupSpotsService.java:38-39 → note(walkMinutes 결정 때)
  - [하] academyName null 테스트 없음(ListPickupSpotsService.java:51) → 항목 추가
  - [하] PATCH 기본 장소 false 거절이 500m 검사보다 먼저인 순서 테스트 없음 → 항목 추가
  - [하] 기존 장소 saveSpot 갱신(merge) DB 통합 테스트 없음 → 항목 추가
  - [하] Swagger 404 설명 부정확(PickupSpotApiDoc.java:39 「다른 가족의 학원」, L49 좌표 변경 때 학원 없음 빠짐) → 리팩터링
  - [하] 기본 장소 유일성 서비스만 — 동시 첫 장소 2개면 기본 2개 · IncorrectResultSize 500(추론) → 사람 결정
  - [하] 설명 주석 9곳(02 L78) — PickupSpot.java:36, AcademyDistanceCheck.java:6, OwnedSpotLookup.java:8, UpdatePickupSpotService.java:45, ListPickupSpotsService.java:38, UpdateSpotRequest.java:8, ListSpotsQuery.java:3, UpdateSpotCommand.java:5, SpotSummary.java:3 → 리팩터링
  - [하] 파라미터 많음(PickupSpot.create 8 · restore 9, PickupSpotEntity 생성자 9) → note
  - [하] SpotResponse 이름 중복(최상위 · PickupSpotsResponse 안) → 리팩터링
  - [하] 02 L137 schema.sql 설명이 실제 인덱스와 다름 → note
  - [하] FOLLOWUP 확인 필요 표가 07과 다름 → note
  - 이전 지적: 삭제 순서 테스트 해소(#50) · 좌표 변경 학원 없음 해소(#51) · near · radiusM 하나만 web에서 막음 해소 · loadActiveChildLocation · 파라미터 남음
  - 완료 기준: C1 충족(51/51, 1127 passed, place 135) · C2 충족(Swagger 문구 2곳 수정 필요) · C3 사람 확인 필요(주석 규칙 하 9곳) · C4 충족(FOLLOWUP 표 동기화는 note)
- 이전 리뷰: 2026-11-08 03:25 KST (조건부 통과), 2026-11-08 03:18 KST (조건부 통과)

## 확인 필요 사항 (설계서)
| 항목 | 현재 가정 |
|---|---|
| 지난 세션이 가리키는 장소의 삭제 | DB FK가 없어 지금은 지워지고, 지난 세션의 spotId가 없는 행을 가리킬 수 있음. pickup 설계 때 삭제 대신 표시 등을 정함 |
| 반경 조회 기준 좌표 | near · radiusM이 정차 지점 기준인지 만남 지점 기준인지 소스에 없음. GIST가 걸린 정차 지점 기준으로 가정 |
| ST_MakeEnvelope(지도 범위) 조회 | 소스 2.2는 지도 범위 조회를 말하지만 API에는 near · radiusM만 있음. 범위(사각형) 파라미터는 두지 않음 |
| near · radiusM 중 하나만 온 경우 | 결정됨: 422 VALIDATION_FAILED(빠진 쪽을 fields에) |
| walkMinutes 계산 방법 | 걷는 속도 · 경로 계산 방법이 소스에 없음. 정해야 함. 지금은 늘 null |
| 유일한 기본 장소를 PATCH로 isDefault false로 바꾸는 경우 | 결정됨: 남은 장소 수와 상관없이 422 DEFAULT_SPOT_REQUIRED(다른 장소를 기본으로 지정해야 바뀜) |
| PATCH로 memo 지우기 | null은 「안 보냄」이라 지금은 못 지움. 빈 문자열을 「지움」으로 볼지 정해야 함 |
| 기본 장소 유일성을 DB로도 막을지 | 소스에는 규칙만 있음. 서비스에서만 지킨다고 가정 |
| SPOT-02의 academyId가 가족 일정에 없는 학원인 경우 | 그대로 둠: Academy가 있으면 받고, 없으면 404 |
| 장소 변경이 settingsVersion을 올리는지 | 04_사용자-user.md와 같은 질문. 올리지 않는다고 가정 |
| 11/4 기획 수정안(소스 7.1) | 확정되면 학원 출입구를 기본 장소로 쓰고 직접 찍는 장소는 대안 0~2개가 됨 → 「첫 장소가 기본」 규칙이 바뀜 |

## 다음 단계
- 도메인 리뷰: /tdd review place (2026-11-08 03:42 KST 리뷰 뒤에 체크 · 추가된 항목 #52, #53, #54, #55, #56, #57, #58)
- 커밋 · 푸시: /tdd commit (기능별 분할 커밋) · /tdd push (커밋 후 기능 브랜치 푸시)
- 다음 도메인: 픽업 (08_픽업-pickup.md). 시작: /tdd domain pickup

## 후속 과제
- place: SPOT-03 PATCH에서 null이 「안 보냄」이라 memo를 지울 방법이 없음 — 빈 문자열("")을 「지움」으로 볼지 결정 필요. 설계서에 없던 record 이름(AcademySpots · SpotItem), SpotInfo 필드(spotId · familyId · academyId · name · stopPoint · meetingPoint)는 test-writer가 정함
- place 결정(팀장): #48 기본 장소를 PATCH로 isDefault false로 바꾸면 남은 장소 수와 상관없이 422 DEFAULT_SPOT_REQUIRED(장소가 있는 학원엔 기본 장소가 늘 하나). 학원 정보가 없을 때 수정 · 추가는 404(테스트는 추가 쪽만)
- place 결정(팀장): SPOT-01 학원 정보가 없으면 academyName null로 묶음 유지, walkMinutes는 계산 방법이 정해질 때까지 늘 null, 학원 이름은 학원마다 loadAcademy 1회(가족 학원 수가 적다고 가정), 묶음 academyId · 장소 spotId 오름차순
- place: ListPickupSpotsService가 쓰지 않는 loadActiveChildLocation을 매번 부름(테스트가 strict stub으로 호출을 요구). walkMinutes 계산이 정해지면 결과를 이어 붙이거나, 아니면 테스트 stub을 lenient로 바꾸고 호출 제거
- place 가정: SPOT-01 near는 한 파라미터 "lat,lng" 형식, 형식 오류도 422 VALIDATION_FAILED(fields near, 팀장 결정). near만 오면 fields radiusM, radiusM만 오면 fields near
- [place 중간 리뷰] (1) 02 L72 네이티브 쿼리 예외에 DistancePort(SPOT-02 · 03 ST_DWithin 500m 검사, JdbcTemplate) 추가 필요(설계서 07 L86이 요구) (2) DB FK 없음 → 07 L147 지난 세션 장소 삭제 가정 다시 써야 함(pickup 전 결정: 삭제 대신 표시 등) (3) 기본 장소 유일성을 DB 부분 유니크 인덱스로도 막을지(07 L147 가정은 서비스만) (4) common GeoPoints.toPoint(LatLng) 추가 후보 (5) GeoOffsets 계산 근거 Javadoc
- place: SPOT-02 name 공백만("   ") 처리 미정, stopPoint · meetingPoint 안 lat · lng 누락은 공통 LatLng가 primitive라 0.0이 될 수 있음(LatLng 범위 · 필수 검증은 공통 후속 과제와 같음)
- place: SPOT-03 PATCH 오류 칸에 422 DEFAULT_SPOT_REQUIRED(#48)와 404가 설계서 API 표에 없음 — 문서 갱신 필요. PATCH "memo":null 직접 전송은 「안 보냄」으로 처리(지우기 방법 미정)
- [결정 · 사람] 기본 장소 유일성을 DB 부분 유니크 인덱스로도 막음, 충돌은 장소 전용 409 DEFAULT_SPOT_CONFLICT(팀장: 공통 409 코드가 없어 새 코드). [place 리뷰 note] 08 L38 spotId FK 표기 vs DB FK 없음(pickup 전 결정), lat · lng 범위 검증(공통 LatLng 후속), loadActiveChildLocation 쓰지 않는 호출(walkMinutes 결정 때), 파라미터 많음(PickupSpot.create 8 · restore 9, 엔티티 9), 02 L137 schema.sql 설명 갱신, FOLLOWUP 확인 필요 표 07과 동기화
- place 운영 위험: dev · prod DB에 같은 가족 · 학원의 기본 장소가 이미 둘 이상이면 서버 시작 때 CREATE UNIQUE INDEX uk_pickup_spot_default가 실패해 서버가 안 뜸 — 배포 전 중복 정리 필요. saveSpot은 저장마다 saveAndFlush(되돌리면 INSERT가 UPDATE보다 먼저 나가 인덱스와 부딪힘, 통합 테스트 2건이 막음)
