# FOLLOWUP · 장소 (place)

- 생성: 2026-11-08 02:54 KST · tdd-guard
- 상태: 진행 중 (체크 8/47 · 완료 기준 0/4)
- 테스트: ● GREEN · 1 passed · cd C:/pickup; ./gradlew.bat test --cons… · 19s · cd C:/pickup; ./gradlew.bat test --console=plain --tests "com.example.pickupservice.place.fixture.PlaceSeedTest" 2>&1 |…
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

## 미완료 항목
- [ ] #9 [persistence] PickupSpot을 저장하고 읽으면 stopPoint · meetingPoint의 lat · lng가 소수점 6자리까지 같다
- [ ] #10 [persistence] stop_point 컬럼에 GIST 인덱스가 있다
- [ ] #11 [persistence] loadSpots는 그 가족의 장소만 돌려주고 academyId를 주면 그 학원 장소만 돌려준다
- [ ] #12 [persistence] loadSpotsWithin은 near에서 radiusM 안에 정차 지점이 있는 장소만 돌려준다
- [ ] #13 [persistence] isWithinMeters는 학원에서 499m인 좌표에 true, 501m인 좌표에 false를 돌려준다
- [ ] #14 [persistence] loadDefaultSpot은 가족 · 학원의 isDefault true 장소를 돌려준다
- [ ] #15 [서비스] 장소 추가는 학원의 첫 장소면 isDefault false로 보내도 isDefault true로 저장한다
- [ ] #16 [서비스] 장소 추가는 isDefault true면 같은 학원의 이전 기본 장소를 해제한다
- [ ] #17 [서비스] 장소 추가는 정차 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [ ] #18 [서비스] 장소 추가는 만남 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [ ] #19 [서비스] 장소 추가는 호출한 보호자를 createdBy로 저장한다
- [ ] #20 [서비스] 장소 수정은 보낸 필드만 바꾼다
- [ ] #21 [서비스] 장소 수정은 바꾼 좌표가 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [ ] #22 [서비스] 장소 수정은 isDefault true로 바꾸면 같은 학원의 이전 기본 장소를 해제한다
- [ ] #23 [서비스] 장소 삭제는 진행 중 세션이 쓰는 장소면 SPOT_IN_USE로 거절한다
- [ ] #24 [서비스] 장소 삭제는 다른 장소가 남은 기본 장소면 DEFAULT_SPOT_REQUIRED로 거절한다
- [ ] #25 [서비스] 다른 가족의 spotId를 수정 · 삭제하려 하면 NOT_FOUND로 거절한다
- [ ] #26 [서비스] 장소 조회는 학원별로 묶어 academyName과 함께 돌려준다
- [ ] #27 [서비스] 장소 조회는 진행 중 세션이 없으면 walkMinutes를 null로 돌려준다
- [ ] #28 [서비스] findDefaultSpot은 가족 · 학원의 기본 장소를 돌려주고 없으면 빈 값을 돌려준다
- [ ] #29 [web] GET /v1/families/me/pickup-spots는 200과 academies[]를 돌려준다
- [ ] #30 [web] GET /v1/families/me/pickup-spots는 차량 토큰으로도 200을 돌려준다
- [ ] #31 [web] GET /v1/families/me/pickup-spots에 near만 보내고 radiusM을 빼면 422 VALIDATION_FAILED를 돌려준다
- [ ] #32 [web] GET /v1/families/me/pickup-spots에 radiusM 3001을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #33 [web] GET /v1/families/me/pickup-spots를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #34 [web] POST /v1/families/me/pickup-spots는 201과 spotId · isDefault를 돌려준다
- [ ] #35 [web] POST /v1/families/me/pickup-spots에 51자 name을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #36 [web] POST /v1/families/me/pickup-spots가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다
- [ ] #37 [web] POST /v1/families/me/pickup-spots를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #38 [web] PATCH /v1/pickup-spots/{spotId}는 200과 spotId · name · stopPoint · meetingPoint · memo · isDefault를 돌려준다
- [ ] #39 [web] PATCH /v1/pickup-spots/{spotId}가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다
- [ ] #40 [web] DELETE /v1/pickup-spots/{spotId}는 204를 돌려준다
- [ ] #41 [web] DELETE /v1/pickup-spots/{spotId}가 진행 중 세션의 장소면 409 SPOT_IN_USE를 돌려준다
- [ ] #42 [web] DELETE /v1/pickup-spots/{spotId}가 다른 장소가 남은 기본 장소면 422 DEFAULT_SPOT_REQUIRED를 돌려준다
- [ ] #43 [web] Idempotency-Key는 POST /v1/families/me/pickup-spots · DELETE /v1/pickup-spots/{spotId}에만 요구된다
- [ ] #44 [persistence] pickup 구현 전 임시 CheckActivePickupPort는 isUsedByActiveSession false, loadActiveChildLocation 빈 값을 돌려준다
- [ ] #45 [persistence] LoadAcademyPort 어댑터는 schedule의 LoadAcademyUseCase.findAcademy 결과를 place가 쓰는 학원 정보(academyId · name · location)로 바꿔 돌려주고 없으면 빈 값을 돌려준다
- [ ] #46 [서비스] findSpot은 spotId의 장소를 돌려주고 없으면 빈 값을, findSpots는 가족의 학원 목록에 속한 장소를 spotId · academyId · name · isDefault로 돌려준다
- [ ] #47 schedule의 임시 LoadPickupSpotPort를 place findSpots 연동으로 바꿔 설정 요약 · 주간 일정의 PICKUP_SPOT 판정이 실제 장소를 본다

## 완료 기준 (공통 규칙 문서)
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 아직 없음. 모든 항목이 체크되면 도메인 리뷰(tdd-guard:domain-reviewer · /tdd review)를 받아 checklist review로 기록해야 도메인이 완료된다.

## 확인 필요 사항 (설계서)
| 항목 | 현재 가정 |
|---|---|
| 지난 세션이 가리키는 장소의 삭제 | PickupSession.spotId가 PickupSpot FK라 지난 세션이 쓴 장소는 행을 지울 수 없음. 삭제 대신 표시만 할지 정해야 함 |
| 반경 조회 기준 좌표 | near · radiusM이 정차 지점 기준인지 만남 지점 기준인지 소스에 없음. GIST가 걸린 정차 지점 기준으로 가정 |
| ST_MakeEnvelope(지도 범위) 조회 | 소스 2.2는 지도 범위 조회를 말하지만 API에는 near · radiusM만 있음. 범위(사각형) 파라미터는 두지 않음 |
| near · radiusM 중 하나만 온 경우 | 소스는 「함께」만 있음. 422 VALIDATION_FAILED로 가정 |
| walkMinutes 계산 방법 | 걷는 속도 · 경로 계산 방법이 소스에 없음. 정해야 함 |
| 유일한 기본 장소를 PATCH로 isDefault false로 바꾸는 경우 | 「가족 · 학원마다 하나만 true」와 충돌. 거절(422 DEFAULT_SPOT_REQUIRED)로 가정 |
| 기본 장소 유일성을 DB로도 막을지 | 소스에는 규칙만 있음. 서비스에서만 지킨다고 가정 |
| SPOT-02의 academyId가 가족 일정에 없는 학원인 경우 | 소스에 없음. Academy가 있으면 받는다고 가정, 없으면 404 |
| 장소 변경이 settingsVersion을 올리는지 | 04_사용자-user.md와 같은 질문. 올리지 않는다고 가정 |
| 11/4 기획 수정안(소스 7.1) | 확정되면 학원 출입구를 기본 장소로 쓰고 직접 찍는 장소는 대안 0~2개가 됨 → 「첫 장소가 기본」 규칙이 바뀜 |

## 다음 단계
- 남은 항목 39건부터 계속: /tdd domain place → /tdd go
- 다음 도메인: 픽업 (08_픽업-pickup.md). 시작: /tdd domain pickup

## 후속 과제
- place: SPOT-03 PATCH에서 null이 「안 보냄」이라 memo를 지울 방법이 없음 — 빈 문자열("")을 「지움」으로 볼지 결정 필요. 설계서에 없던 record 이름(AcademySpots · SpotItem), SpotInfo 필드(spotId · familyId · academyId · name · stopPoint · meetingPoint)는 test-writer가 정함
