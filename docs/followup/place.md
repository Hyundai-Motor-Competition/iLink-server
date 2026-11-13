# FOLLOWUP · 장소 (place)

- 생성: 2026-11-14 00:55 KST · tdd-guard
- 상태: 완료 (체크 107/107 · 완료 기준 4/4)
- 테스트: ● GREEN · 2769 passed · 전체 · 255s · .\gradlew.bat test --console=plain
- 설계서: C:/pw/place/docs/07_장소-place.md

## 완료 항목
- [x] #1 [기반] 기본 장소 규칙은 가족 · 학원의 첫 장소를 isDefault true로 만든다
- [x] #2 [기반] 기본 장소 규칙은 새 기본 장소를 지정하면 같은 가족 · 학원의 이전 기본 장소를 false로 바꾼다
- [x] #3 [기반] 삭제 규칙은 같은 학원에 다른 장소가 남아 있는 기본 장소의 삭제를 거절한다
- [x] #4 [기반] 삭제 규칙은 학원에 하나 남은 기본 장소의 삭제를 허용한다
- [x] #5 [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] #6 [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] #7 [기반] PickupSpotEntity가 BaseTimeEntity를 상속하고 stopPoint · meetingPoint는 geography(Point,4326)로 매핑된다
- [x] #8 [기반] 테스트 시드로 해솔영어학원의 기본 장소 정문 앞과 다른 장소 1개를 만든다
- [x] #9 [persistence] PickupSpot을 저장하고 읽으면 stopPoint · meetingPoint의 lat · lng가 소수점 6자리까지 같다
- [x] #10 [persistence] stop_point 컬럼에 GIST 인덱스가 있다
- [x] #11 [persistence] loadSpots는 그 가족의 장소만 돌려주고 academyId를 주면 그 학원 장소만 돌려준다
- [x] #12 [persistence] loadSpotsWithin은 near에서 radiusM 안에 정차 지점이 있는 장소만 돌려준다
- [x] #13 [persistence] isWithinMeters는 학원에서 499m인 좌표에 true, 501m인 좌표에 false를 돌려준다
- [x] #14 [persistence] loadDefaultSpot은 가족 · 학원의 isDefault true 장소를 돌려준다
- [x] #15 [서비스] 장소 추가는 학원의 첫 장소면 isDefault false로 보내도 isDefault true로 저장한다
- [x] #16 [서비스] 장소 추가는 isDefault true면 같은 학원의 이전 기본 장소를 해제한다
- [x] #17 [서비스] 장소 추가는 정차 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [x] #18 [서비스] 장소 추가는 만남 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [x] #19 [서비스] 장소 추가는 호출한 보호자를 createdBy로 저장한다
- [x] #20 [서비스] 장소 수정은 보낸 필드만 바꾼다
- [x] #21 [서비스] 장소 수정은 바꾼 좌표가 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [x] #22 [서비스] 장소 수정은 isDefault true로 바꾸면 같은 학원의 이전 기본 장소를 해제한다
- [x] #23 [서비스] 장소 삭제는 진행 중 세션이 쓰는 장소면 SPOT_IN_USE로 거절한다
- [x] #24 [서비스] 장소 삭제는 다른 장소가 남은 기본 장소면 DEFAULT_SPOT_REQUIRED로 거절한다
- [x] #25 [서비스] 다른 가족의 spotId를 수정 · 삭제하려 하면 NOT_FOUND로 거절한다
- [x] #26 [서비스] 장소 조회는 학원별로 묶어 academyName과 함께 돌려준다
- [x] #27 [서비스] 장소 조회는 진행 중 세션이 없어도 walkMinutes를 계산해 돌려준다(null을 주지 않음. #98로 대체, 사람 결정 2026-11-13 · 2026-11-14)
- [x] #28 [서비스] findDefaultSpot은 가족 · 학원의 기본 장소를 돌려주고 없으면 빈 값을 돌려준다
- [x] #29 [web] GET /v1/families/me/pickup-spots는 200과 academies[]를 돌려준다
- [x] #30 [web] GET /v1/families/me/pickup-spots는 차량 토큰으로도 200을 돌려준다
- [x] #31 [web] GET /v1/families/me/pickup-spots에 near만 보내고 radiusM을 빼면 422 VALIDATION_FAILED를 돌려준다
- [x] #32 [web] GET /v1/families/me/pickup-spots에 radiusM 3001을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] #33 [web] GET /v1/families/me/pickup-spots를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] #34 [web] POST /v1/families/me/pickup-spots는 201과 spotId · isDefault를 돌려준다
- [x] #35 [web] POST /v1/families/me/pickup-spots에 51자 name을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] #36 [web] POST /v1/families/me/pickup-spots가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다
- [x] #37 [web] POST /v1/families/me/pickup-spots를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] #38 [web] PATCH /v1/pickup-spots/{spotId}는 200과 spotId · name · stopPoint · meetingPoint · memo · isDefault를 돌려준다
- [x] #39 [web] PATCH /v1/pickup-spots/{spotId}가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다
- [x] #40 [web] DELETE /v1/pickup-spots/{spotId}는 204를 돌려준다
- [x] #41 [web] DELETE /v1/pickup-spots/{spotId}가 진행 중 세션의 장소면 409 SPOT_IN_USE를 돌려준다
- [x] #42 [web] DELETE /v1/pickup-spots/{spotId}가 다른 장소가 남은 기본 장소면 422 DEFAULT_SPOT_REQUIRED를 돌려준다
- [x] #43 [web] Idempotency-Key는 POST /v1/families/me/pickup-spots · DELETE /v1/pickup-spots/{spotId}에만 요구된다
- [x] #44 [persistence] CheckActivePickupPort 어댑터는 isUsedByActiveSession · loadActiveChildLocation에 pickup ActivePickupQueryUseCase.isSpotInUse · activeChildLocation 결과를 그대로 돌려준다(사람 결정 2026-11-13)
- [x] #45 [persistence] LoadAcademyPort 어댑터는 schedule의 LoadAcademyUseCase.findAcademy 결과를 place가 쓰는 학원 정보(academyId · name · location)로 바꿔 돌려주고 없으면 빈 값을 돌려준다
- [x] #46 [서비스] findSpot은 spotId의 장소를 돌려주고 없으면 빈 값을, findSpots는 가족의 학원 목록에 속한 장소를 spotId · academyId · name · isDefault로 돌려준다
- [x] #47 schedule의 임시 LoadPickupSpotPort를 place findSpots 연동으로 바꿔 설정 요약 · 주간 일정의 PICKUP_SPOT 판정이 실제 장소를 본다
- [x] #48 [서비스] 장소 수정은 기본 장소를 isDefault false로 바꾸려 하면 DEFAULT_SPOT_REQUIRED로 거절한다(다른 장소를 기본으로 지정해야 바뀜)
- [x] #49 [persistence] deleteSpot은 그 장소 행만 지우고 같은 가족 · 학원의 다른 장소는 남긴다
- [x] #50 장소 삭제는 진행 중 세션이 쓰는 기본 장소면 같은 학원에 다른 장소가 남아 있어도 SPOT_IN_USE를 먼저 돌려준다
- [x] #51 장소 수정은 좌표를 바꿀 때 학원 정보가 없으면 NOT_FOUND로 거절하고 저장하지 않는다
- [x] #52 [web] GET /v1/families/me/pickup-spots에 lat,lng 형식이 아닌 near(abc · 37.5)를 보내면 422 VALIDATION_FAILED와 fields의 near를 돌려준다
- [x] #53 [서비스] 장소 조회는 학원 정보가 없는 학원의 장소도 academyName null로 묶어 돌려준다
- [x] #54 [서비스] 장소 수정은 기본 장소를 isDefault false로 바꾸면서 500m 밖 좌표를 보내도 DEFAULT_SPOT_REQUIRED로 먼저 거절하고 거리 검사를 하지 않는다
- [x] #55 [persistence] saveSpot으로 spotId가 있는 장소를 다시 저장하면 새 행을 만들지 않고 그 행의 이름 · 좌표 · isDefault가 바뀐다
- [x] #56 pickup_spot에 (family_id, academy_id) WHERE is_default 부분 유니크 인덱스가 있어 같은 가족 · 학원에 기본 장소 두 개를 저장할 수 없다
- [x] #57 장소 추가 · 수정에서 isDefault true로 지정하면 이전 기본 장소를 먼저 해제한 뒤 저장해 유니크 인덱스와 부딪히지 않는다(실제 DB로 확인)
- [x] #58 동시 요청으로 기본 장소 유니크 인덱스가 부딪히면 409 DEFAULT_SPOT_CONFLICT로 거절한다
- [x] #59 [기반] 걷는 시간 규칙은 직선거리에 1.3을 곱해 반올림한 walkMeters와 그것을 1.1로 나눠 올림한 walkSeconds를 돌려준다
- [x] #60 [기반] 후보 합치기 규칙은 길가 후보를 도로 이름마다 학원에 가장 가까운 1곳씩 최대 4곳만 남긴다
- [x] #61 [기반] 후보 합치기 규칙은 합친 후보를 walkMeters가 작은 순 8곳으로 줄인다
- [x] #62 [기반] 추천 순위 규칙은 totalSeconds가 작은 순으로 3곳에 rank 1~3을 붙이고 totalSeconds가 같으면 walkMeters가 작은 쪽을 앞에 둔다
- [x] #63 [기반] 추천 순위 규칙은 driveSeconds가 없으면 totalSeconds를 null로 두고 walkMeters가 작은 순으로 3곳을 고른다
- [x] #64 [기반] 주의 표시 규칙은 CURB_STOP에 LINE_DATA_2021, DOTTED_YELLOW 길가에 STOP_5MIN_ONLY, 단속 카메라 근처 길가에 ENFORCEMENT_CAMERA_NEAR를 붙이고 PUBLIC_PARKING에는 붙이지 않는다
- [x] #65 [기반] 지원 지역 규칙은 강남구 경계 도형 안의 좌표에 true, 밖의 좌표에 false를 돌려준다
- [x] #66 [기반] 핵심 로직 「시그니처」의 SPOT-04 in 포트 · out 포트 인터페이스와 Query · Result record가 있다
- [x] #67 [기반] PublicParkingEntity · CurbStopCandidateEntity가 테이블 정의대로 매핑되고 location은 geography(Point,4326)이다
- [x] #68 [persistence] 서버가 뜰 때 PublicParking · CurbStopCandidate가 비어 있으면 classpath CSV에서 60행 · 3,147행을 읽어 넣고 이미 행이 있으면 다시 넣지 않는다
- [x] #69 [persistence] loadPublicParkingWithin은 중심에서 radiusM 안의 공영주차장만 돌려준다
- [x] #70 [persistence] loadCurbStopsWithin은 중심에서 radiusM 안의 길가 후보만 돌려준다
- [x] #71 [persistence] public_parking · curb_stop_candidate의 location 컬럼에 GIST 인덱스가 있다
- [x] #72 [서비스] 차량 시간 어댑터는 자동차 길찾기를 목적지마다 불러 routes[0].summary.duration을 목적지 순서대로 돌려준다
- [x] #73 [서비스] 차량 시간 어댑터는 departAt이 있으면 미래 운행 정보 길찾기를 목적지마다 departure_time(yyyyMMddHHmm, Asia/Seoul)으로 부른다
- [x] #74 [서비스] 차량 시간 어댑터는 외부 API가 실패하거나 result_code가 0이 아니면 빈 목록을 돌려준다
- [x] #75 [서비스] 장소 추천은 학원 좌표가 강남구 밖이면 supported false와 빈 items를 돌려주고 외부 API를 부르지 않는다
- [x] #76 [서비스] 장소 추천은 공영주차장 · 길가 후보를 합쳐 totalSeconds가 작은 3곳을 rank 1~3으로 돌려준다
- [x] #77 [서비스] 장소 추천은 arriveAt이 없으면 driveSecondsNow를, 있으면 driveSecondsAt을 부른다
- [x] #78 [서비스] 장소 추천은 차량 시간이 빈 목록이면 driveSeconds · totalSeconds를 null로 두고 walkMeters가 작은 순으로 돌려준다
- [x] #79 [web] GET /v1/pickup-spot-recommendations는 200과 supported · items[](rank · kind · name · location · walkMeters · walkSeconds · driveSeconds · totalSeconds · cautions)를 돌려준다
- [x] #80 [web] GET /v1/pickup-spot-recommendations는 차량 토큰으로도 200을 돌려준다
- [x] #81 [web] GET /v1/pickup-spot-recommendations에 academyId나 origin이 없거나 origin이 lat,lng 형식이 아니면 422 VALIDATION_FAILED와 fields를 돌려준다
- [x] #82 [web] GET /v1/pickup-spot-recommendations에 과거 arriveAt을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] #83 [web] GET /v1/pickup-spot-recommendations를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] #84 [web] Idempotency-Key는 GET /v1/pickup-spot-recommendations에 요구되지 않는다
- [x] #85 [서비스] 장소 추천은 academyId의 학원을 LoadAcademyPort로 읽어 그 location을 학원 좌표로 쓰고 학원이 없으면 NOT_FOUND로 거절한다
- [x] #86 [web] GET /v1/pickup-spot-recommendations에 없는 academyId를 보내면 404 NOT_FOUND를 돌려준다
- [x] #87 [서비스] 장소 추천은 후보가 3곳보다 적으면 있는 만큼만 돌려주고 0곳이면 supported true와 빈 items를 돌려준다
- [x] #88 [기반] classpath reference/gangnam에 boundary_gangnam.geojson · parking_public.csv(60행) · curb_stop_candidates.csv(3,147행)가 있다
- [x] #89 [persistence] ServiceAreaPort 어댑터는 classpath reference/gangnam/boundary_gangnam.geojson을 읽어 강남구 안의 좌표에 true, 밖의 좌표에 false를 돌려준다
- [x] #90 [기반] parking_public.csv의 parking_id와 curb_stop_candidates.csv의 candidate_id는 겹치지 않는다
- [x] #91 직선거리 규칙은 두 좌표 사이 거리를 m로 돌려주고(위도 0.001도는 약 111m) place.domain은 JTS를 쓰는 GeoPoints 없이 계산한다
- [x] #92 장소 추천은 차량 시간 포트를 부를 때 DB 트랜잭션이 열려 있지 않다
- [x] #93 장소 추천은 실제 참조 데이터와 강남구 경계로 대치동 학원 좌표 주변의 공영주차장 · 길가 후보를 돌려준다(차량 시간은 목)
- [x] #94 GET /v1/pickup-spot-recommendations에 ISO 형식이 아닌 arriveAt을 보내면 422 VALIDATION_FAILED와 fields의 arriveAt을 돌려준다
- [x] #95 차량 시간 어댑터는 미래 운행 정보 길찾기에 summary 파라미터를 붙이지 않는다
- [x] #96 참조 데이터 로더는 테이블별 적재를 한 트랜잭션으로 해서 중간에 실패하면 그 테이블에 일부 행을 남기지 않는다
- [x] #97 [기반] 블록 우회 도보 거리 규칙은 두 좌표 사이 남북 거리와 동서 거리를 더한 값을 m로 돌려준다(북쪽 300m · 동쪽 400m면 직선 500m가 아니라 700m)
- [x] #98 [서비스] 장소 조회는 walkMinutes를 학원 출입구(없으면 학원 위치)에서 만남 지점까지 블록 우회 도보 거리를 초속 1.1m로 나눈 시간을 분으로 올림해 돌려준다
- [x] #99 [persistence] LoadAcademyPort 어댑터는 schedule 학원의 출입구 좌표(entrance)를 place AcademyInfo.entrance로 넘기고, 출입구가 없으면 null로 넘긴다
- [x] #100 [서비스] 차량 세션용 임시 장소 저장 in 포트는 familyId · academyId · name · stopPoint · meetingPoint로 temporary=true인 PickupSpot을 저장하고 spotId를 돌려준다(최종디자인 11/13 V02 · V03, 팀장 결정 2026-11-13) ← SaveTemporarySpotServiceTest.임시 장소 저장은 받은 familyId · academyId · name · stopPoint · meetingPoint 그대로 temporary true · isDefault false인 PickupSpot을 저장하고 저장 결과의 spotId를 돌려준다
SaveTemporarySpotServiceTest.임시 장소 저장은 정차 · 만남 지점이 학원에서 500m를 넘어도 거절하지 않고 그대로 저장한다
- [x] #101 [persistence] 가족 장소 목록(GET /v1/families/me/pickup-spots)과 기본 장소 판정은 temporary=true인 임시 장소를 빼고 돌려준다 ← PlacePersistenceAdapterTest.loadSpots는 temporary true인 임시 장소를 빼고 돌려준다(academyId null · academyId 지정 모두). 임시 장소는 spotId로는 읽히고 temporary true다
PlacePersistenceAdapterTest.loadSpots는 임시 장소만 있는 가족 · 학원이면 빈 목록을 돌려준다(첫 장소 기본 지정 판정이 임시 장소를 세지 않게)
PlacePersistenceAdapterTest.loadSpotsWithin은 반경 안에 정차 지점이 있어도 temporary true인 임시 장소는 빼고 돌려준다
PlacePersistenceAdapterTest.loadDefaultSpot은 is_default true인 행이라도 temporary true인 임시 장소면 기본 장소로 돌려주지 않는다
- [x] #102 [web] GET /v1/pickup-spot-recommendations/nearby?academyId=는 출발 위치 없이 학원 기준 근처 후보를 name · location · walkMeters · walkSeconds로 걷는 거리 짧은 순으로 돌려준다(일정 등록 픽업일정등록8 화면용) ← PickupSpotRecommendationControllerTest.GET /v1/pickup-spot-recommendations/nearby는 200과 supported · items[](name · location · walkMeters · walkSeconds)를 유스케이스가 준 걷는 거리 순서 그대로 돌려주고 driveSeconds · totalSeconds 필드는 없다
PickupSpotRecommendationControllerTest.GET /v1/pickup-spot-recommendations/nearby는 차량 토큰으로도 출발 위치 없이 200과 supported · items[]를 돌려준다
PickupSpotRecommendationControllerTest.GET /v1/pickup-spot-recommendations/nearby에 academyId가 없으면 422 VALIDATION_FAILED와 fields의 academyId를 돌려주고 유스케이스를 부르지 않는다
PickupSpotRecommendationControllerTest.GET /v1/pickup-spot-recommendations/nearby를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다
- [x] #103 [서비스] 학원 기준 근처 추천은 카카오 길찾기를 부르지 않고 driveSeconds · totalSeconds를 비운다. 지원 지역 밖 학원이면 supported false와 빈 목록을 준다 ← RecommendPickupSpotsServiceTest.학원 기준 근처 추천은 공영주차장 500m · 길가 450m 후보를 합쳐 name · location · walkMeters · walkSeconds를 걷는 거리 짧은 순으로 돌려주고 카카오 길찾기(DriveTimePort)를 부르지 않는다
RecommendPickupSpotsServiceTest.학원 기준 근처 추천은 학원 좌표가 지원 지역 밖이면 supported false와 빈 items를 돌려주고 후보 조회 · 차량 시간 포트를 부르지 않는다
RecommendPickupSpotsServiceTest.학원 기준 근처 추천은 학원이 없으면 NOT_FOUND로 거절하고 지원 지역 · 후보 조회 · 차량 시간 포트를 부르지 않는다
- [x] #104 [서비스] 장소 추천(SPOT-04 · 학원 기준 근처 추천)은 도로 이름이 빈 길가 후보의 name을 「이름 없는 길」로 돌려준다(null을 주지 않음, 사람 결정 2026-11-13) ← RecommendPickupSpotsServiceTest.도로 이름이 null · 빈 문자열 · 공백뿐인 길가 후보의 name은 「이름 없는 길」이고 이름 있는 길가 · 공영주차장 이름은 그대로다
SpotCandidateMergerTest.후보 합치기 규칙은 도로 이름이 null · 빈 문자열 · 공백뿐인 길가 후보의 name을 「이름 없는 길」로 바꾸고 이름 있는 길가 · 공영주차장 이름은 그대로 둔다
- [x] #105 [서비스] 장소 수정(PATCH /v1/pickup-spots/{spotId})은 temporary=true인 임시 장소면 NOT_FOUND로 거절하고 저장하지 않는다(사람 결정 2026-11-14) ← UpdatePickupSpotServiceTest.임시 장소(temporary=true)를 수정하려 하면 isDefault true를 보내도 NOT_FOUND로 거절하고 이전 기본 장소를 건드리지 않고 저장하지 않는다
- [x] #106 [서비스] 장소 삭제(DELETE /v1/pickup-spots/{spotId})는 temporary=true인 임시 장소면 NOT_FOUND로 거절하고 지우지 않는다(사람 결정 2026-11-14) ← DeletePickupSpotServiceTest.임시 장소(temporary=true)를 삭제하려 하면 같은 가족의 장소여도 NOT_FOUND로 거절하고 지우지 않는다
- [x] #107 [서비스] 임시 장소 저장은 name이 null · 공백뿐이거나 50자를 넘으면 VALIDATION_FAILED(fields name)로 거절하고 저장하지 않는다(사람 결정 2026-11-14: 검사는 place에서) ← SaveTemporarySpotServiceTest.임시 장소 저장은 name이 51자면 VALIDATION_FAILED(fields name)로 거절하고 저장하지 않는다
SaveTemporarySpotServiceTest.임시 장소 저장은 name이 null이면 VALIDATION_FAILED(fields name)로 거절하고 저장하지 않는다
SaveTemporarySpotServiceTest.임시 장소 저장은 name이 공백뿐이면 VALIDATION_FAILED(fields name)로 거절하고 저장하지 않는다
SaveTemporarySpotServiceTest.임시 장소 저장은 name이 정확히 50자면 저장한다

## 미완료 항목
- 없음

## 완료 기준 (공통 규칙 문서)
- [x] C1 설계서의 테스트 케이스가 모두 GREEN
- [x] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [x] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [x] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 2026-11-14 00:55 KST · 도메인 리뷰어 · 결론 조건부 통과 · 상 0 · 중 4 · 하 9 · 리뷰 시점 체크 107/107
- 리뷰 시점 테스트: ● GREEN · 2769 passed · 전체 · 255s · .\gradlew.bat test --console=plain
  - 결론: 조건부 통과
  - 이전 리뷰의 [중] 3건 중 2건(임시 장소 PATCH, #27 문장)은 해소됐습니다. 남은 [중]은 진행 기록 후퇴 1건이고, 코드 동작을 바꿔야 하는 [상]은 없습니다.
  - ## 이전 지적 해소 여부
  - [중] PATCH로 임시 장소의 temporary가 지워짐: 해소. OwnedSpotLookup.java:15가 !spot.isTemporary()로 걸러 PATCH · DELETE 모두 404. UpdatePickupSpotService.java:79도 10인자 restore로 temporary를 넘김.
  - [중] 진행 기록 후퇴: 남음, 범위가 늘어남.
  - [중] #27 문장이 동작과 반대: 해소. 07 L195가 「세션이 없어도 계산」으로 바뀌어 07 L95와 맞음.
  - [하] SaveTemporarySpotService 이름 검사 · @Transactional 없음: 해소.
  - [하] 07 범위 줄 · 구조 절: 해소. [하] 테스트 Javadoc: 해소.
  - [하] 묶음 키 isBlank · temporary 기본값 테스트 · 9인자 restore: 남음(note).
  - [하] 이전 리뷰의 남은 [하]: 남음(ListPickupSpotsService.java:34 필드, 07 L8 · L158, PickupSpotControllerTest.java:73).
  - ## 발견 사항
  - [중] 진행 기록 후퇴가 그대로이고 REVIEW.md까지 번졌습니다. TODO.md 공유모듈 줄 「대기 · 0/4 · 기록 없음」, REVIEW.md L8 공유모듈 「리뷰 없음」. shared.md L4는 「완료 4/4」. place 옛 리뷰 4건은 archive에 없음 → note(커밋 전에 TODO · REVIEW 공유모듈 줄을 되돌림)
  - [하] 02 L176에 새로 넣은 예외 문장이 실제 동작보다 넓습니다. radiusM=abc는 GlobalExceptionHandler가 400 INVALID_REQUEST로 돌려줌 → 사람 결정(문장을 좁힐지, 422로 맞추는 항목을 추가할지)
  - [하] 07 API 표와 핵심 로직에 「임시 장소는 404」가 없습니다(확인 필요 표 L304에만 있음) → note(문서 갱신)
  - [하] FOLLOWUP 확인 필요 표가 07과 다릅니다(L304 행 없음) → note
  - [하] 이미 결정된 후속 과제가 「사람 결정 필요」로 남아 있습니다 → note(FOLLOWUP 정리)
  - [하] 9인자 PickupSpot.restore가 아직 남아 있고 새 테스트(SaveTemporarySpotServiceTest.java:140)에서도 씁니다 → 리팩터링
  - 새 항목 확인: #105 · #106 · #107 테스트가 동작을 실제로 확인함(saveSpot · deleteSpot 0회, 50/51자 경계, fields name).
  - ## 완료 기준
  - C1 충족 — GREEN 2769 passed. #27 문장 고쳐짐.
  - C2 충족 — 임시 장소 PATCH · DELETE 404가 07 L304 결정 · L45 취지와 맞음. SPOT-01~05와 임시 장소 포트 일치.
  - C3 충족(조건) — 02 L176 · L217에 결정 반영. 남은 것은 02 L176 문장 범위(radiusM 숫자 아님)뿐이고 [하].
  - C4 충족 — 07 확인 필요 표 L304에 새 결정 행이 「결정됨」으로 있음.
  - ## 테스트
  - mcp__tdd-guard__tests scope "all": GREEN 2769 passed · 255s.
- 이전 리뷰: 2026-11-14 00:45 KST (조건부 통과)

## 확인 필요 사항 (설계서)
| 항목 | 현재 가정 |
|---|---|
| 지난 세션이 가리키는 장소의 삭제 | DB FK가 없어 지금은 지워지고, 지난 세션의 spotId가 없는 행을 가리킬 수 있음. pickup 설계 때 삭제 대신 표시 등을 정함 |
| 반경 조회 기준 좌표 | near · radiusM이 정차 지점 기준인지 만남 지점 기준인지 소스에 없음. GIST가 걸린 정차 지점 기준으로 가정 |
| ST_MakeEnvelope(지도 범위) 조회 | 소스 2.2는 지도 범위 조회를 말하지만 API에는 near · radiusM만 있음. 범위(사각형) 파라미터는 두지 않음 |
| near · radiusM 중 하나만 온 경우 | 결정됨: 422 VALIDATION_FAILED(빠진 쪽을 fields에) |
| walkMinutes 계산 방법 | [사람 결정 2026-11-13] 학원 출입구 → 만남 지점 블록 우회 거리(남북 + 동서) ÷ 초속 1.1m, 분 올림 |
| 유일한 기본 장소를 PATCH로 isDefault false로 바꾸는 경우 | 결정됨: 남은 장소 수와 상관없이 422 DEFAULT_SPOT_REQUIRED(다른 장소를 기본으로 지정해야 바뀜) |
| PATCH로 memo 지우기 | null은 「안 보냄」이라 지금은 못 지움. 빈 문자열을 「지움」으로 볼지 정해야 함 |
| 기본 장소 유일성을 DB로도 막을지 | 결정됨(사람): DB 부분 유니크 인덱스 uk_pickup_spot_default + 동시 요청으로 부딪히면 409 DEFAULT_SPOT_CONFLICT |
| 배포 전 기존 중복 기본 장소 정리 | 운영 DB에 같은 가족 · 학원의 기본 장소가 두 개 이상 있으면 인덱스 생성이 실패해 서버가 뜨지 않음. 배포 전에 중복을 찾아 하나만 남길지, 남길 기준(최근 수정 · 가장 작은 spotId 등)을 정해야 함 |
| DEFAULT_SPOT_CONFLICT 공통 오류 표 등록 | 02_공통-규칙-global의 오류 코드 표에 없음. place 문서에만 둔다고 가정 |
| SPOT-02의 academyId가 가족 일정에 없는 학원인 경우 | 그대로 둠: Academy가 있으면 받고, 없으면 404 |
| 장소 변경이 settingsVersion을 올리는지 | 04_사용자-user.md와 같은 질문. 올리지 않는다고 가정 |
| 11/4 기획 수정안(소스 7.1) | 확정되면 학원 출입구를 기본 장소로 쓰고 직접 찍는 장소는 대안 0~2개가 됨 → 「첫 장소가 기본」 규칙이 바뀜 |
| SPOT-04 지원 지역 | 결정됨(사람 2026-11-12): 강남구만. 다른 구는 supported false |
| SPOT-04 후보 종류 | 결정됨(사람 2026-11-12): 공영주차장과 길가 정차 구간만. 민영 주차장은 제외(요금 · 운영 시간 · 개방 여부를 받을 공개 데이터가 없음), 어린이보호구역 근처 길가도 제외 |
| SPOT-04 걷는 시간 | 직선거리 × 1.3 ÷ 1.1m/s로 가정. 보행 경로 API는 쓰지 않음 |
| SPOT-04 길가 선 데이터 | 서울시 차선 데이터가 2021-08 파일이라 그 뒤 바뀐 곳은 반영 안 됨. cautions LINE_DATA_2021로 알림. 최신본 확보 필요 |
| SPOT-04 어린이보호구역 판정 | 구역 경계선 데이터가 없어 대상 시설 300m 안을 제외로 가정. 경찰청 보호구역 도형을 받으면 교체 |
| SPOT-04 후보가 3곳보다 적을 때 | 있는 만큼만 돌려줌. 0곳이면 supported true와 빈 items |
| SPOT-04 카카오모빌리티 한도 | 무료 하루 자동차 길찾기 10,000건 · 미래 운행 5,000건. 요청 1건이 최대 8건을 씀. 한도를 넘을 때 대응은 미정. 다중 목적지 길찾기는 반경 10km 한도 때문에 쓰지 않음[팀장 결정 2026-11-12] |
| SPOT-04 카카오 결과 저장 | 카카오 DevTalk 답변 기준 장소 ID · place_url만 저장 가능. 추천 결과는 저장하지 않음 |
| 도로 이름이 빈 길가 후보의 name | 결정됨: null 대신 「이름 없는 길」로 준다(사람 결정 2026-11-13) |
| 추천 장소 선택 · 여기로 부르기의 장소 저장 | 결정됨: 차량 앱이 추천 장소를 고르거나 「여기로 부르기」를 하면 서버가 임시 장소(temporary=true인 PickupSpot)를 만든다. 임시 장소는 가족 장소 목록 · 기본 장소 판정에서 빠진다(사람 결정 2026-11-13) |
| 임시 장소의 SPOT-03 PATCH · DELETE | 결정됨: 임시 장소(temporary=true)는 PATCH · DELETE 모두 404 NOT_FOUND. 임시 장소 이름은 place가 1~50자로 검사해 422 VALIDATION_FAILED(사람 결정 2026-11-14, #105~#107) |
| SPOT-04 참조 데이터 갱신 | CSV를 손으로 다시 만들어 넣음. 자동 갱신 없음 |
| SPOT-04 origin | 클라이언트가 차량 현재 좌표를 보냄. 서버가 가진 SIG-01 위치는 쓰지 않음 |
| SPOT-04 학원 위치 | 결정됨(사람 2026-11-12): 저장된 Academy를 academyId로 조회해 location을 씀. 출입구 좌표(entrance)는 아직 쓰지 않음. 저장된 값이 카카오 검색에서 온 것이라 저장 방식은 06_일정-schedule에서 따로 정함(미정) |

## 다음 단계
- 커밋 · 푸시: /tdd commit (기능별 분할 커밋) · /tdd push (커밋 후 기능 브랜치 푸시)
- 다음 도메인: 픽업 (08_픽업-pickup.md). 시작: /tdd domain pickup

## 후속 과제
- place #100 팀장 결정: 임시 장소 in 포트는 SaveTemporarySpotUseCase.saveTemporarySpot(SaveTemporarySpotCommand(familyId, academyId, name, stopPoint, meetingPoint)) → long spotId. createdBy 0(차량 토큰엔 사용자 없음), isDefault false, 500m · 학원 존재 검사 없음. 설계서 시그니처 절에 반영 필요
- place #100 운영 DB: prod는 ddl-auto validate라 배포 전 ALTER TABLE pickup_spot ADD COLUMN temporary boolean NOT NULL DEFAULT false 를 따로 실행해야 함(마이그레이션 도구 없음)
- place 도메인 리뷰(11-14) [하] 묶음: (1) SpotCandidateMerger 묶음 키가 null만 한 묶음이고 "" · 공백은 따로 묶임 → 키도 isBlank 기준으로 맞추는 리팩터링(실데이터 공백 행 0건) (2) temporary 기본값 false를 매핑 테스트가 보지 않음 (3) 07 범위 줄 · 구조 절에 SPOT-05 · SaveTemporarySpotUseCase · NearbySpotsResponse 반영 (4) SaveTemporarySpotServiceTest.java:31 Javadoc 주석 제거 (5) 9인자 PickupSpot.restore 보조 함수 제거 리팩터링
- place 도메인 리뷰(11-14) [중] 진행 기록 후퇴: docs/followup/TODO.md 공유모듈 줄이 「완료 4/4」에서 「대기 0/4」로 바뀜(세션 시작 전부터 수정된 상태), place.md에서 이전 리뷰 4건 · C2 · C4 체크가 지워짐. 커밋 전에 TODO 공유모듈 줄 되돌리기, place 리뷰 이력 archive 보관 여부 결정
- place 재리뷰(11-14) 처리: 02 L176 예외 문장을 실제 동작에 맞게 좁힘(radiusM 숫자 아님은 400 그대로), 07 API 표 L79 · L80 · 핵심 로직 L106에 임시 장소 404 반영. 남은 것: (1) [중] TODO.md · REVIEW.md 공유모듈 줄 후퇴는 커밋 전에 되돌림(shared.md는 완료 4/4) (2) FOLLOWUP 확인 필요 표에 07 L304 임시 장소 결정 행 동기화 (3) 이미 해소된 후속 과제(422 vs 400, road_name 빈 값, PlaceDomainImportTest, C1 #44 · C3, walkMinutes 늘 null) 정리 (4) 9인자 PickupSpot.restore를 테스트 7곳 + SaveTemporarySpotServiceTest.givenSaveAssignsSpotId에서 10인자로 바꾸고 지우는 리팩터링
- SPOT-04는 필수 query(academyId · origin)가 빠지면 422 VALIDATION_FAILED(fields)를 낸다. 02_공통-규칙-global L173은 필수 파라미터 누락 · 타입 오류를 400 INVALID_REQUEST로 정해 서로 다르다. 구현은 설계서(422)를 따르며 컨트롤러가 문자열로 받아 직접 검사한다. 공통 규칙에 예외를 적을지, SPOT-04를 400으로 맞출지 사람이 정해야 함
- [설계 결정 필요] 길가 후보 3,147행 중 812행(26%)은 road_name이 비어 있다. 이 후보가 뽑히면 SPOT-04 응답 name이 null이고, 이름 없는 도로는 한 묶음으로 쳐서 1곳만 남는다. 대체 이름(예: 이름 없는 길) · 후보에서 빼기 · null 허용 중 사람이 정해야 함
- [운영] prod는 ddl-auto validate라 새 테이블 public_parking · curb_stop_candidate를 운영 DB에 직접 만들어야 서버가 뜬다. 저장소에 DDL 스크립트가 없다(schema.sql은 인덱스만). 배포 전에 생성 스크립트를 준비해야 함
- SPOT-04 후속: (1) 실제 카카오모빌리티 호출은 테스트가 없다(MockRestServiceServer만). 키 · 한도 · 응답 모양은 2026-11-12 수동 호출로만 확인 (2) 공영주차장 좌표가 소수점 7~8자리라 응답 location이 6자리를 넘음. 공통 좌표 반올림 결정과 함께 정리 (3) API 문서 예시 arriveAt의 +09:00은 URL에서 %2B로 바꿔야 함. 실제 HTTP로는 미확인 (4) GangnamServiceAreaAdapter는 classpath 파일을 읽는데 adapter/out/persistence에 있음 (5) 카카오 어댑터 2개(KakaoMapSearchAdapter · KakaoMobilityDriveTimeAdapter)의 RestClient 설정 · 헤더 · 실패 처리가 겹침. common으로 묶을 후보 (6) 「서버가 뜬 뒤 60행 · 3,147행」 테스트는 @AfterEach의 load()로도 통과할 수 있어 ApplicationRunner 연결을 직접 보지 않음 (7) 02_공통-규칙-global에 추천 책임 · 네이티브 쿼리 예외(반경 조회 2개 · 로더 INSERT) · 참조 테이블 PK 예외를 반영해야 함 (8) 참조 데이터(parking_public.csv · curb_stop_candidates.csv)는 손으로 다시 만들어 넣어야 하고, 길가 선 데이터는 2021-08 파일 기준
- [설계 결정 필요] (1) iLink_Project_Package/ 폴더(기획 문서 + 강남구 원본 데이터 약 15MB)가 git에 없고 .gitignore에도 없다. 커밋 · 무시 · 삭제 중 사람이 정해야 함. 2026-11-12 커밋에서는 제외함 (2) PlaceDomainImportTest는 소스 글자를 훑어 의존 규칙을 보는 테스트인데 02_공통-규칙-global L80 · L214는 아키텍처 규칙을 코드 리뷰로 확인한다고 정한다(선례 ScheduleModuleImportTest). 그대로 두고 02에 예외를 적을지, 지울지 사람이 정해야 함 (3) 참조 데이터 로더는 빈 테이블 확인이 트랜잭션 밖이고 잠금이 없어, 서버 2대가 빈 테이블로 동시에 뜨면 한쪽이 기본 키 충돌로 실패할 수 있음. 배포 대수 확인 필요
- [완료 기준 미충족 · 사람 결정 필요] C1: #44 문장(임시 CheckActivePickupPort는 false · 빈 값)이 지금 동작(pickup 연동 어댑터)과 반대라 문장을 고치거나 항목을 정리해야 함. 전체 테스트는 GREEN 2415 passed. C3: 필수 쿼리 누락을 422로 돌려주는 것과 공통 규칙의 400 중 어느 쪽인지, PlaceDomainImportTest를 02 문서 L80 · L214의 예외로 둘지 정해지지 않음. GangnamReferenceDataLoader의 Javadoc 위반은 2026-11-12 정리 리뷰에서 지움
- [2026-11-13] 체크된 #191(「진행 중 세션이 없으면 walkMinutes null」)의 증거 테스트는 사람 결정(블록 우회 도보 시간)으로 동작이 바뀌어 #98 테스트 4건으로 대체됨. #191 문장은 옛 동작. ListPickupSpotsService의 쓰지 않던 loadActiveChildLocation 호출 제거(포트는 PlaceOutPortsTest가 확인하므로 유지)
- [1차 개발 뒤 정리 · 2026-11-13] (1) 체크된 #27(07 L191 「세션 없으면 walkMinutes null」)과 #44 문장이 지금 동작과 반대 — 문장 고칠지 항목 정리할지 결정(C1) (2) 아무도 안 쓰는 경로 place CheckActivePickupPort.loadActiveChildLocation → PlacePickupActivityAdapter → pickup activeChildLocation 정리 여부 (3) ListPickupSpotsService의 쓰지 않는 checkActivePickupPort 필드 · 테스트 lenient stub 제거 (4) 07 L155 시그니처 주석 「(walkMinutes)」, L8 의존 줄 「자녀 현재 위치」, PickupSpotControllerTest DisplayName 「null walkMinutes」 갱신 (5) AcademyInfo 3인자 보조 생성자 제거 후보 (6) SPOT-04도 출입구를 기준점으로 쓸지 (7) FOLLOWUP의 #191 표기는 place #27. 응답 예시(07 L83) walkMinutes는 2로 갱신함
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
- [place 재리뷰 note] web 계층 409 DEFAULT_SPOT_CONFLICT 테스트 없음(공통 처리), 실제 동시 요청 시나리오 테스트 없음, FOLLOWUP 확인 필요 표가 07 L180-182와 다름(유일성 결정 · 배포 전 중복 정리 · DEFAULT_SPOT_CONFLICT 공통 표 등록 여부)
