# 07. 장소 (place)

보호자가 지도에서 지정한 픽업 장소와 학원별 기본 장소. 가족(Family) 단위로 저장해 형제가 같은 학원이면 같이 쓴다. 학원은 schedule의 Academy를 참조한다.

| 항목 | 내용 |
|---|---|
| 범위 | 엔티티 PickupSpot. API SPOT-01(조회) · SPOT-02(추가) · SPOT-03(수정 · 삭제) · SPOT-04(추천). 참조 데이터 PublicParking · CurbStopCandidate. 다른 모듈용 in 포트(기본 장소 · 장소 조회) |
| 의존 | 03_공유모듈-shared, 04_사용자-user(가족 · 작성자), 06_일정-schedule(Academy 위치. out 포트 LoadAcademyPort → schedule in 포트 LoadAcademyUseCase.findAcademy). pickup의 in 포트(진행 중 세션이 쓰는 장소, 자녀 현재 위치)는 out 포트 CheckActivePickupPort → pickup ActivePickupQueryUseCase로 연동됨(사람 결정 2026-11-13). schedule의 LoadPickupSpotPort는 place LoadPickupSpotUseCase.findSpots에 위임 |
| 진행 | 계층 병렬 |

## 구조

```text
place
├─ adapter/in/web          PickupSpotController(SPOT-01~03), PickupSpotApiDoc,
│                          PickupSpotRecommendationController(SPOT-04), PickupSpotRecommendationApiDoc, LatLngParam("lat,lng" 파싱)
├─ adapter/out/persistence PickupSpotEntity, PlacePersistenceAdapter(LoadSpotPort · SaveSpotPort),
│                          PostgisDistanceAdapter(DistancePort, ST_DWithin), PickupSpotJpaRepository(반경 네이티브 쿼리 2개),
│                          PublicParkingEntity · CurbStopCandidateEntity(추천 참조 데이터), PublicParkingJpaRepository · CurbStopCandidateJpaRepository(반경 네이티브 쿼리),
│                          SpotCandidatePersistenceAdapter(LoadSpotCandidatePort), GangnamServiceAreaAdapter(ServiceAreaPort, classpath 경계 파일),
│                          GangnamReferenceDataLoader(서버가 뜰 때 CSV 적재, JdbcTemplate 묶음 INSERT)
├─ adapter/out/map         KakaoMobilityDriveTimeAdapter(DriveTimePort, 카카오모빌리티 길찾기)
├─ adapter/out/schedule    PlaceAcademyAdapter(LoadAcademyPort → schedule in 포트 LoadAcademyUseCase.findAcademy, academyId · name · location)
├─ adapter/out/pickup      PlacePickupActivityAdapter(CheckActivePickupPort 구현 → pickup ActivePickupQueryUseCase.isSpotInUse · activeChildLocation)(사람 결정 2026-11-13)
├─ application/port/in · out · service
└─ domain                  PickupSpot, 기본 장소 규칙,
                           추천 규칙(StraightDistance · WalkTimeRule · SpotCandidateMerger · SpotRankingRule · SpotCautionRule · ServiceArea)
```

## 테이블

픽업 장소 (PickupSpot)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| spotId | BIGINT | PK | |
| familyId | BIGINT | | Family 참조(DB FK 제약 없음) |
| academyId | BIGINT | | Academy 참조(DB FK 제약 없음) |
| name | VARCHAR(50) | | 1~50자 |
| memo | VARCHAR(200) | NULL | 0~200자 |
| stopPoint | geography(Point,4326) | GIST | 정차 지점 |
| meetingPoint | geography(Point,4326) | | 만남 지점(WGS84) |
| isDefault | BOOLEAN | 부분 유니크 인덱스 uk_pickup_spot_default (family_id, academy_id) WHERE is_default | 가족 · 학원마다 하나만 true |
| createdBy | BIGINT | | User 참조(DB FK 제약 없음) |
| temporary | BOOLEAN | NOT NULL, 기본 false | 차량 세션용 임시 장소면 true. 가족 장소 목록 · 기본 장소 판정에서 빠지고 spotId 단건 조회로는 읽힌다(사람 결정 2026-11-13) |

familyId · academyId · createdBy는 프로젝트 관례대로 DB FK 제약 없이 컬럼만 둔다.
uk_pickup_spot_default는 JPA가 만들지 못하는 부분 유니크 인덱스라 02_공통-규칙-global 관례대로 src/main/resources/schema.sql에 CREATE UNIQUE INDEX IF NOT EXISTS로 적는다.

공영주차장 (PublicParking) — 추천용 참조 데이터 [2026-11-12 추가]

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| parkingId | VARCHAR(20) | PK | 주차장관리번호. 원본에서 번호가 겹치는 행은 뒤에 -2 · -3 · -4를 붙여 구분(122-3-000002가 4곳에 쓰임) |
| name | VARCHAR(100) | | |
| location | geography(Point,4326) | GIST | |
| spaces | INT | NULL | 주차 면수 |
| baseMinutes | INT | NULL | 기본 시간(분) |
| baseFeeWon | INT | NULL | 기본 요금(원) |

길가 정차 후보 (CurbStopCandidate) — 추천용 참조 데이터 [2026-11-12 추가]

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| candidateId | BIGINT | PK | |
| location | geography(Point,4326) | GIST | 도로 중심 좌표 |
| roadName | VARCHAR(100) | NULL | |
| lineKind | VARCHAR(20) | | DOTTED_YELLOW(주차금지 선 · 5분 이내 정차만) · NONE(선 표시 없음) |
| nearEnforcementCctv | BOOLEAN | | 단속 카메라 100m 안 |

두 테이블의 데이터는 src/main/resources/reference/gangnam/의 parking_public.csv(60행) · curb_stop_candidates.csv(3,147행)이다. 공개 데이터(공공데이터포털 전국주차장정보표준데이터의 강남구 파일, 서울 열린데이터광장의 차선 · 횡단보도 · 버스정류소 · 소방용수시설 · 버스전용차로 데이터, OpenStreetMap 도로)를 2026-11-12에 받아 가공한 것이다. 길가 후보는 주정차금지 선 · 횡단보도 · 교차점 · 소화전 · 버스정류소 · 버스전용차로 · 정차금지지대 근처와 어린이보호구역 대상 시설 300m 안을 미리 뺀 목록이다. 서버가 뜰 때 테이블이 비어 있으면 classpath reference/gangnam/의 같은 CSV에서 읽어 넣는다. CSV는 UTF-8이고 맨 앞에 BOM이 있다. 읽는 컬럼은 parking_public.csv의 parking_id · name · latitude · longitude · spaces · base_minutes · base_fee_won, curb_stop_candidates.csv의 candidate_id · latitude · longitude · road_name · line_kind · near_enforcement_cctv(Y 또는 빈 값)이고 나머지 컬럼은 무시한다. 강남구 경계는 classpath reference/gangnam/boundary_gangnam.geojson(Polygon 1개, 좌표 순서 [경도, 위도])이다. 세 파일은 src/main/resources/reference/gangnam/에 둔다.

## API 명세

| ID | Method · 경로 | 호출 | Idempotency-Key | 요청 → 응답 | 오류 |
|---|---|---|---|---|---|
| SPOT-01 | GET /v1/families/me/pickup-spots | GUARDIAN · CO_GUARDIAN · DELEGATE, 차량 앱 | 불필요 | query academyId(선택), near(한 파라미터 "lat,lng" 형식 · radiusM과 함께), radiusM(50~3000 · near와 함께) → 200 academies[](academyId · academyName · spots[](spotId · name · stopPoint · meetingPoint · memo · isDefault · walkMinutes)). 차량 토큰은 토큰의 familyId | 422 VALIDATION_FAILED(near · radiusM 중 하나만, radiusM 50~3000 밖, near 형식 오류) |
| SPOT-02 | POST /v1/families/me/pickup-spots | GUARDIAN · CO_GUARDIAN | 필요 | academyId, name(1~50자), stopPoint{lat, lng}, meetingPoint{lat, lng}, memo(0~200자, 선택), isDefault(기본 false) → 201 spotId, isDefault | 404 NOT_FOUND(학원 없음), 409 DEFAULT_SPOT_CONFLICT(동시 요청으로 기본 장소가 겹침), 422 VALIDATION_FAILED, 422 SPOT_TOO_FAR |
| SPOT-03 | PATCH /v1/pickup-spots/{spotId} | GUARDIAN · CO_GUARDIAN | 불필요 | name, stopPoint, meetingPoint, memo, isDefault(모두 선택) → 200 spotId · name · stopPoint · meetingPoint · memo · isDefault | 404 NOT_FOUND(다른 가족 · 없는 spotId, 좌표를 바꿀 때 학원 없음), 409 DEFAULT_SPOT_CONFLICT(동시 요청으로 기본 장소가 겹침), 422 VALIDATION_FAILED, 422 SPOT_TOO_FAR, 422 DEFAULT_SPOT_REQUIRED(기본 장소를 isDefault false로) |
| SPOT-03 | DELETE /v1/pickup-spots/{spotId} | GUARDIAN · CO_GUARDIAN | 필요 | — → 204 | 404 NOT_FOUND, 409 SPOT_IN_USE, 422 DEFAULT_SPOT_REQUIRED |
| SPOT-04 | GET /v1/pickup-spot-recommendations | GUARDIAN · CO_GUARDIAN · DELEGATE, 차량 앱 | 불필요 | query academyId(저장된 학원, 필수), origin(차량 현재 좌표 "lat,lng", 필수), arriveAt(선택, ISO 시각) → 200 supported, items[](rank · kind(PUBLIC_PARKING · CURB_STOP) · name · location{lat, lng} · walkMeters · walkSeconds · driveSeconds · totalSeconds · cautions[]) 최대 3개 | 404 NOT_FOUND(학원 없음), 422 VALIDATION_FAILED(academyId · origin이 없거나 형식 오류, arriveAt이 ISO 형식이 아니거나 과거) |

응답 예시
- SPOT-01: `{"academies":[{"academyId":55,"academyName":"해솔영어학원","spots":[{"spotId":21,"name":"정문 앞","stopPoint":{"lat":37.501234,"lng":127.039876},"meetingPoint":{"lat":37.501301,"lng":127.039901},"memo":"편의점 앞","isDefault":true,"walkMinutes":1}]}]}`
- SPOT-02: `{"spotId":21,"isDefault":true}`

## 핵심 로직

### 조회 (SPOT-01)
- 내 가족(차량은 차량 토큰의 familyId)의 장소를 학원별로 묶어 돌려준다. academyId를 주면 그 학원 장소만.
- 순서: 학원 묶음은 academyId 오름차순, 묶음 안 장소는 spotId 오름차순.
- 학원 이름은 학원마다 한 번만 읽는다. 학원 정보가 없으면 academyName은 null.
- near · radiusM을 주면 정차 지점이 반경 안에 있는 장소만 공간 인덱스로 조회한다(PostGIS 네이티브 쿼리, ST_DWithin). 둘 중 하나만 주면 422 VALIDATION_FAILED(빠진 쪽을 fields에).
- walkMinutes는 학원 출입구(없으면 학원 위치)에서 그 장소의 만남 지점까지 걷는 시간(분, 올림)이다. 직선은 건물을 가로질러 너무 짧게 나오므로, 두 점의 남북 거리와 동서 거리를 더한 「블록 우회 거리」(건물 블록을 돌아가는 길)를 초속 1.1m로 나눈다. 진행 중 세션 여부와 관계없이 늘 계산한다(#191의 「세션이 없으면 null」을 대신함) [사람 결정 2026-11-13]. 실제 도보 경로 API(카카오 도보 길찾기는 유료 제휴 필요)를 쓰게 되면 이 계산만 바꾼다.

### 추가 (SPOT-02)
- 학원이 없으면 404 NOT_FOUND.
- 지도에서 찍은 WGS84 좌표를 geography로 저장한다. createdBy는 호출한 보호자.
- 정차 지점과 만남 지점을 모두 검사해 하나라도 학원 위치에서 500m를 넘으면 422 SPOT_TOO_FAR(ST_DWithin으로 검사).
- 가족 · 학원별 첫 장소는 isDefault 값과 상관없이 기본 장소가 된다.
- isDefault = true로 추가하면 같은 가족 · 학원의 이전 기본 장소는 해제된다. 이전 기본 장소를 먼저 isDefault false로 저장한 뒤 새 기본 장소를 저장해 uk_pickup_spot_default와 부딪히지 않게 한다.
- 동시 요청으로 uk_pickup_spot_default가 부딪히면 409 DEFAULT_SPOT_CONFLICT.

### 수정 · 삭제 (SPOT-03)
- 다른 가족의 spotId와 없는 spotId는 404 NOT_FOUND.
- PATCH는 보낸(null 아닌) 필드만 바꾼다. null은 「안 보냄」이라 memo를 지우는 방법은 아직 없다.
- PATCH로 기본 장소를 isDefault false로 바꾸면 남은 장소 수와 상관없이 422 DEFAULT_SPOT_REQUIRED. 장소가 있는 학원엔 기본 장소가 늘 하나이고, 다른 장소를 기본으로 지정해야 바뀐다. 이 거절은 500m 검사보다 먼저 한다.
- PATCH는 바꾼 좌표만 500m를 검사해 넘으면 422 SPOT_TOO_FAR. 좌표를 바꿀 때 학원이 없으면 404 NOT_FOUND이고 저장하지 않는다.
- PATCH로 isDefault = true로 바꾸면 같은 학원의 이전 기본 장소는 해제된다. 추가와 같이 이전 기본 장소를 먼저 해제해 저장한 뒤 이 장소를 저장하고, 동시 요청으로 uk_pickup_spot_default가 부딪히면 409 DEFAULT_SPOT_CONFLICT.
- DELETE 검사 순서: 404 NOT_FOUND(다른 가족 · 없음) → 409 SPOT_IN_USE(진행 중 세션이 쓰는 장소) → 422 DEFAULT_SPOT_REQUIRED(같은 학원에 다른 장소가 남은 기본 장소. 먼저 다른 장소를 기본으로 지정).
- 저장소의 deleteSpot은 없는 spotId면 조용히 무시한다(서비스가 먼저 404를 돌려줌).
- 장소를 고쳐도 진행 중 세션과 지난 결과는 세션의 스냅샷(spotName · stopPoint · meetingPoint)을 쓴다.

### 장소 추천 (SPOT-04) [2026-11-12 추가]
- 학원을 고르면 차를 댈 수 있는 장소를 「걷는 시간 + 차량 도착 시간」이 짧은 순으로 3곳까지 돌려준다. 추천 결과는 저장하지 않는다. 고른 장소는 SPOT-02로 저장한다.
- 학원 좌표는 저장된 Academy의 location이다. academyId로 LoadAcademyPort.loadAcademy를 불러 읽고, 학원이 없으면 404 NOT_FOUND. 일정 등록(SET-02)으로 학원이 저장된 뒤에만 추천할 수 있다.
- 지원 지역은 강남구다. 학원 좌표가 강남구 경계 도형(classpath reference/gangnam/boundary_gangnam.geojson) 밖이면 supported false와 빈 items를 돌려주고 외부 API를 부르지 않는다.
- 후보는 두 종류다. 민영 · 건물 주차장은 요금 · 운영 시간 · 외부 차량 개방 여부를 알 수 없어 추천에 넣지 않는다.
  - PUBLIC_PARKING: PublicParking 반경 500m.
  - CURB_STOP: CurbStopCandidate 반경 450m. 도로 이름마다 학원에 가장 가까운 1곳, 최대 4곳. name은 roadName.
- walkMeters = 학원 좌표와 후보의 직선거리 × 1.3(반올림). walkSeconds = walkMeters ÷ 1.1(올림).
- 합친 후보를 walkMeters가 작은 순 8곳으로 줄인 뒤 차량 시간을 구한다.
- driveSeconds: arriveAt이 없으면 카카오모빌리티 자동차 길찾기(GET /v1/directions, origin · destination은 "경도,위도", summary=true)를 후보마다 1회. arriveAt이 있으면 미래 운행 정보 길찾기(GET /v1/future/directions, origin · destination + departure_time = arriveAt, yyyyMMddHHmm, Asia/Seoul. summary는 붙이지 않는다. 2026-11-12 실제 호출에서 summary 없이 routes[0].summary.duration이 오는 것을 확인)를 후보마다 1회. 응답의 routes[0].result_code가 0이면 routes[0].summary.duration(초)을 쓴다. 한 곳이라도 호출이 실패하거나 result_code가 0이 아니면 빈 목록으로 본다. 헤더는 Authorization: KakaoAK {키}.
- 다중 목적지 길찾기는 쓰지 않는다. 출발지 반경 10km가 한도라 차가 멀리 있으면 result_code 304가 온다(2026-11-12 실제 호출로 확인).
- totalSeconds = walkSeconds + driveSeconds. totalSeconds 오름차순, 같으면 walkMeters가 작은 쪽이 앞. 앞에서 3곳에 rank 1~3을 붙인다.
- cautions: CURB_STOP은 LINE_DATA_2021, lineKind가 DOTTED_YELLOW면 STOP_5MIN_ONLY, nearEnforcementCctv면 ENFORCEMENT_CAMERA_NEAR. PUBLIC_PARKING은 빈 배열.
- 외부 API가 실패해도 오류를 내지 않는다. 카카오모빌리티가 실패하면 driveSeconds · totalSeconds를 null로 두고 walkMeters가 작은 순 3곳을 돌려준다.
- 추천 서비스는 DB 트랜잭션을 열지 않는다. 외부 길찾기를 최대 8번 부르는 동안 DB 연결을 잡고 있지 않기 위해서다. 학원 · 후보 조회는 각자 짧은 조회로 끝낸다.
- 직선거리 계산은 place.domain 안에 두고 JTS를 쓰는 common.geo.GeoPoints에 기대지 않는다.
- 참조 데이터 적재는 테이블마다 한 트랜잭션으로 한다. 중간에 실패하면 그 테이블에 일부 행을 남기지 않는다.
- 카카오모빌리티 주소는 pickup.map.kakao-mobility.base-url(https://apis-navi.kakaomobility.com), 키는 pickup.map.kakao.api-key를 같이 쓴다.

### 다른 모듈용 조회
- findSpots는 가족 장소를 한 번 읽어 받은 학원 목록으로 거른다.

### 시그니처

```java
// in 포트 — web
SpotGroups listSpots(ListSpotsQuery q);                         // ListPickupSpotsUseCase (familyId, academyId?, near?, radiusM?)
SpotCreated addSpot(AddSpotCommand c);                          // AddPickupSpotUseCase
SpotView updateSpot(UpdateSpotCommand c);                       // UpdatePickupSpotUseCase
void deleteSpot(DeleteSpotCommand c);                           // DeletePickupSpotUseCase

// in 포트 — 다른 모듈용
Optional<SpotInfo> findDefaultSpot(long familyId, long academyId);   // LoadPickupSpotUseCase (pickup SES-02)
Optional<SpotInfo> findSpot(long spotId);                             // LoadPickupSpotUseCase (pickup SES-04 · 05)
List<SpotSummary> findSpots(long familyId, List<Long> academyIds);    // LoadPickupSpotUseCase (schedule SET-07 · 09)

// out 포트
Optional<PickupSpot> loadSpot(long spotId); List<PickupSpot> loadSpots(long familyId, Long academyId); List<PickupSpot> loadSpotsWithin(long familyId, Long academyId, LatLng near, int radiusM); Optional<PickupSpot> loadDefaultSpot(long familyId, long academyId);   // LoadSpotPort
PickupSpot saveSpot(PickupSpot s); void deleteSpot(long spotId);                       // SaveSpotPort
boolean isWithinMeters(LatLng from, LatLng to, int meters);                            // DistancePort (ST_DWithin)
Optional<AcademyInfo> loadAcademy(long academyId);                                     // LoadAcademyPort → schedule
boolean isUsedByActiveSession(long spotId);                                            // CheckActivePickupPort → pickup
Optional<LatLng> loadActiveChildLocation(long familyId);                               // CheckActivePickupPort → pickup (walkMinutes)

// SPOT-04 추천 [2026-11-12 추가]
SpotRecommendations recommendSpots(RecommendSpotsQuery q);                             // RecommendPickupSpotsUseCase (academyId, origin, arriveAt?)
boolean isSupported(LatLng point);                                                     // ServiceAreaPort (강남구 경계 도형)
List<PublicParking> loadPublicParkingWithin(LatLng center, int radiusM); List<CurbStopCandidate> loadCurbStopsWithin(LatLng center, int radiusM);   // LoadSpotCandidatePort
List<Integer> driveSecondsNow(LatLng origin, List<LatLng> destinations); List<Integer> driveSecondsAt(LatLng origin, List<LatLng> destinations, Instant departAt);   // DriveTimePort → 카카오모빌리티 (목적지 순서대로, 실패 시 빈 목록)
```

## 테스트 케이스
- [x] [기반] 기본 장소 규칙은 가족 · 학원의 첫 장소를 isDefault true로 만든다
- [x] [기반] 기본 장소 규칙은 새 기본 장소를 지정하면 같은 가족 · 학원의 이전 기본 장소를 false로 바꾼다
- [x] [기반] 삭제 규칙은 같은 학원에 다른 장소가 남아 있는 기본 장소의 삭제를 거절한다
- [x] [기반] 삭제 규칙은 학원에 하나 남은 기본 장소의 삭제를 허용한다
- [x] [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] [기반] PickupSpotEntity가 BaseTimeEntity를 상속하고 stopPoint · meetingPoint는 geography(Point,4326)로 매핑된다
- [x] [기반] 테스트 시드로 해솔영어학원의 기본 장소 정문 앞과 다른 장소 1개를 만든다
- [x] [persistence] PickupSpot을 저장하고 읽으면 stopPoint · meetingPoint의 lat · lng가 소수점 6자리까지 같다
- [x] [persistence] stop_point 컬럼에 GIST 인덱스가 있다
- [x] [persistence] loadSpots는 그 가족의 장소만 돌려주고 academyId를 주면 그 학원 장소만 돌려준다
- [x] [persistence] loadSpotsWithin은 near에서 radiusM 안에 정차 지점이 있는 장소만 돌려준다
- [x] [persistence] isWithinMeters는 학원에서 499m인 좌표에 true, 501m인 좌표에 false를 돌려준다
- [x] [persistence] loadDefaultSpot은 가족 · 학원의 isDefault true 장소를 돌려준다
- [x] [서비스] 장소 추가는 학원의 첫 장소면 isDefault false로 보내도 isDefault true로 저장한다
- [x] [서비스] 장소 추가는 isDefault true면 같은 학원의 이전 기본 장소를 해제한다
- [x] [서비스] 장소 추가는 정차 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [x] [서비스] 장소 추가는 만남 지점이 학원에서 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [x] [서비스] 장소 추가는 호출한 보호자를 createdBy로 저장한다
- [x] [서비스] 장소 수정은 보낸 필드만 바꾼다
- [x] [서비스] 장소 수정은 바꾼 좌표가 500m를 넘으면 SPOT_TOO_FAR로 거절한다
- [x] [서비스] 장소 수정은 isDefault true로 바꾸면 같은 학원의 이전 기본 장소를 해제한다
- [x] [서비스] 장소 삭제는 진행 중 세션이 쓰는 장소면 SPOT_IN_USE로 거절한다
- [x] [서비스] 장소 삭제는 다른 장소가 남은 기본 장소면 DEFAULT_SPOT_REQUIRED로 거절한다
- [x] [서비스] 다른 가족의 spotId를 수정 · 삭제하려 하면 NOT_FOUND로 거절한다
- [x] [서비스] 장소 조회는 학원별로 묶어 academyName과 함께 돌려준다
- [x] [서비스] 장소 조회는 진행 중 세션이 없으면 walkMinutes를 null로 돌려준다
- [x] [서비스] findDefaultSpot은 가족 · 학원의 기본 장소를 돌려주고 없으면 빈 값을 돌려준다
- [x] [web] GET /v1/families/me/pickup-spots는 200과 academies[]를 돌려준다
- [x] [web] GET /v1/families/me/pickup-spots는 차량 토큰으로도 200을 돌려준다
- [x] [web] GET /v1/families/me/pickup-spots에 near만 보내고 radiusM을 빼면 422 VALIDATION_FAILED를 돌려준다
- [x] [web] GET /v1/families/me/pickup-spots에 radiusM 3001을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] [web] GET /v1/families/me/pickup-spots를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] [web] POST /v1/families/me/pickup-spots는 201과 spotId · isDefault를 돌려준다
- [x] [web] POST /v1/families/me/pickup-spots에 51자 name을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] [web] POST /v1/families/me/pickup-spots가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다
- [x] [web] POST /v1/families/me/pickup-spots를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] [web] PATCH /v1/pickup-spots/{spotId}는 200과 spotId · name · stopPoint · meetingPoint · memo · isDefault를 돌려준다
- [x] [web] PATCH /v1/pickup-spots/{spotId}가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다
- [x] [web] DELETE /v1/pickup-spots/{spotId}는 204를 돌려준다
- [x] [web] DELETE /v1/pickup-spots/{spotId}가 진행 중 세션의 장소면 409 SPOT_IN_USE를 돌려준다
- [x] [web] DELETE /v1/pickup-spots/{spotId}가 다른 장소가 남은 기본 장소면 422 DEFAULT_SPOT_REQUIRED를 돌려준다
- [x] [web] Idempotency-Key는 POST /v1/families/me/pickup-spots · DELETE /v1/pickup-spots/{spotId}에만 요구된다
- [x] [persistence] CheckActivePickupPort 어댑터는 isUsedByActiveSession · loadActiveChildLocation에 pickup ActivePickupQueryUseCase.isSpotInUse · activeChildLocation 결과를 그대로 돌려준다(사람 결정 2026-11-13)
- [x] [persistence] LoadAcademyPort 어댑터는 schedule의 LoadAcademyUseCase.findAcademy 결과를 place가 쓰는 학원 정보(academyId · name · location)로 바꿔 돌려주고 없으면 빈 값을 돌려준다
- [x] [서비스] findSpot은 spotId의 장소를 돌려주고 없으면 빈 값을, findSpots는 가족의 학원 목록에 속한 장소를 spotId · academyId · name · isDefault로 돌려준다
- [x] schedule의 임시 LoadPickupSpotPort를 place findSpots 연동으로 바꿔 설정 요약 · 주간 일정의 PICKUP_SPOT 판정이 실제 장소를 본다
- [x] [서비스] 장소 수정은 기본 장소를 isDefault false로 바꾸려 하면 DEFAULT_SPOT_REQUIRED로 거절한다(다른 장소를 기본으로 지정해야 바뀜)
- [x] [persistence] deleteSpot은 그 장소 행만 지우고 같은 가족 · 학원의 다른 장소는 남긴다
- [x] 장소 삭제는 진행 중 세션이 쓰는 기본 장소면 같은 학원에 다른 장소가 남아 있어도 SPOT_IN_USE를 먼저 돌려준다
- [x] 장소 수정은 좌표를 바꿀 때 학원 정보가 없으면 NOT_FOUND로 거절하고 저장하지 않는다
- [x] [web] GET /v1/families/me/pickup-spots에 lat,lng 형식이 아닌 near(abc · 37.5)를 보내면 422 VALIDATION_FAILED와 fields의 near를 돌려준다
- [x] [서비스] 장소 조회는 학원 정보가 없는 학원의 장소도 academyName null로 묶어 돌려준다
- [x] [서비스] 장소 수정은 기본 장소를 isDefault false로 바꾸면서 500m 밖 좌표를 보내도 DEFAULT_SPOT_REQUIRED로 먼저 거절하고 거리 검사를 하지 않는다
- [x] [persistence] saveSpot으로 spotId가 있는 장소를 다시 저장하면 새 행을 만들지 않고 그 행의 이름 · 좌표 · isDefault가 바뀐다
- [x] pickup_spot에 (family_id, academy_id) WHERE is_default 부분 유니크 인덱스가 있어 같은 가족 · 학원에 기본 장소 두 개를 저장할 수 없다
- [x] 장소 추가 · 수정에서 isDefault true로 지정하면 이전 기본 장소를 먼저 해제한 뒤 저장해 유니크 인덱스와 부딪히지 않는다(실제 DB로 확인)
- [x] 동시 요청으로 기본 장소 유니크 인덱스가 부딪히면 409 DEFAULT_SPOT_CONFLICT로 거절한다
- [x] [기반] 걷는 시간 규칙은 직선거리에 1.3을 곱해 반올림한 walkMeters와 그것을 1.1로 나눠 올림한 walkSeconds를 돌려준다
- [x] [기반] 후보 합치기 규칙은 길가 후보를 도로 이름마다 학원에 가장 가까운 1곳씩 최대 4곳만 남긴다
- [x] [기반] 후보 합치기 규칙은 합친 후보를 walkMeters가 작은 순 8곳으로 줄인다
- [x] [기반] 추천 순위 규칙은 totalSeconds가 작은 순으로 3곳에 rank 1~3을 붙이고 totalSeconds가 같으면 walkMeters가 작은 쪽을 앞에 둔다
- [x] [기반] 추천 순위 규칙은 driveSeconds가 없으면 totalSeconds를 null로 두고 walkMeters가 작은 순으로 3곳을 고른다
- [x] [기반] 주의 표시 규칙은 CURB_STOP에 LINE_DATA_2021, DOTTED_YELLOW 길가에 STOP_5MIN_ONLY, 단속 카메라 근처 길가에 ENFORCEMENT_CAMERA_NEAR를 붙이고 PUBLIC_PARKING에는 붙이지 않는다
- [x] [기반] 지원 지역 규칙은 강남구 경계 도형 안의 좌표에 true, 밖의 좌표에 false를 돌려준다
- [x] [기반] 핵심 로직 「시그니처」의 SPOT-04 in 포트 · out 포트 인터페이스와 Query · Result record가 있다
- [x] [기반] PublicParkingEntity · CurbStopCandidateEntity가 테이블 정의대로 매핑되고 location은 geography(Point,4326)이다
- [x] [persistence] 서버가 뜰 때 PublicParking · CurbStopCandidate가 비어 있으면 classpath CSV에서 60행 · 3,147행을 읽어 넣고 이미 행이 있으면 다시 넣지 않는다
- [x] [persistence] loadPublicParkingWithin은 중심에서 radiusM 안의 공영주차장만 돌려준다
- [x] [persistence] loadCurbStopsWithin은 중심에서 radiusM 안의 길가 후보만 돌려준다
- [x] [persistence] public_parking · curb_stop_candidate의 location 컬럼에 GIST 인덱스가 있다
- [x] [서비스] 차량 시간 어댑터는 자동차 길찾기를 목적지마다 불러 routes[0].summary.duration을 목적지 순서대로 돌려준다
- [x] [서비스] 차량 시간 어댑터는 departAt이 있으면 미래 운행 정보 길찾기를 목적지마다 departure_time(yyyyMMddHHmm, Asia/Seoul)으로 부른다
- [x] [서비스] 차량 시간 어댑터는 외부 API가 실패하거나 result_code가 0이 아니면 빈 목록을 돌려준다
- [x] [서비스] 장소 추천은 학원 좌표가 강남구 밖이면 supported false와 빈 items를 돌려주고 외부 API를 부르지 않는다
- [x] [서비스] 장소 추천은 공영주차장 · 길가 후보를 합쳐 totalSeconds가 작은 3곳을 rank 1~3으로 돌려준다
- [x] [서비스] 장소 추천은 arriveAt이 없으면 driveSecondsNow를, 있으면 driveSecondsAt을 부른다
- [x] [서비스] 장소 추천은 차량 시간이 빈 목록이면 driveSeconds · totalSeconds를 null로 두고 walkMeters가 작은 순으로 돌려준다
- [x] [web] GET /v1/pickup-spot-recommendations는 200과 supported · items[](rank · kind · name · location · walkMeters · walkSeconds · driveSeconds · totalSeconds · cautions)를 돌려준다
- [x] [web] GET /v1/pickup-spot-recommendations는 차량 토큰으로도 200을 돌려준다
- [x] [web] GET /v1/pickup-spot-recommendations에 academyId나 origin이 없거나 origin이 lat,lng 형식이 아니면 422 VALIDATION_FAILED와 fields를 돌려준다
- [x] [web] GET /v1/pickup-spot-recommendations에 과거 arriveAt을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] [web] GET /v1/pickup-spot-recommendations를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] [web] Idempotency-Key는 GET /v1/pickup-spot-recommendations에 요구되지 않는다
- [x] [서비스] 장소 추천은 academyId의 학원을 LoadAcademyPort로 읽어 그 location을 학원 좌표로 쓰고 학원이 없으면 NOT_FOUND로 거절한다
- [x] [web] GET /v1/pickup-spot-recommendations에 없는 academyId를 보내면 404 NOT_FOUND를 돌려준다
- [x] [서비스] 장소 추천은 후보가 3곳보다 적으면 있는 만큼만 돌려주고 0곳이면 supported true와 빈 items를 돌려준다
- [x] [기반] classpath reference/gangnam에 boundary_gangnam.geojson · parking_public.csv(60행) · curb_stop_candidates.csv(3,147행)가 있다
- [x] [persistence] ServiceAreaPort 어댑터는 classpath reference/gangnam/boundary_gangnam.geojson을 읽어 강남구 안의 좌표에 true, 밖의 좌표에 false를 돌려준다
- [x] [기반] parking_public.csv의 parking_id와 curb_stop_candidates.csv의 candidate_id는 겹치지 않는다
- [x] 직선거리 규칙은 두 좌표 사이 거리를 m로 돌려주고(위도 0.001도는 약 111m) place.domain은 JTS를 쓰는 GeoPoints 없이 계산한다
- [x] 장소 추천은 차량 시간 포트를 부를 때 DB 트랜잭션이 열려 있지 않다
- [x] 장소 추천은 실제 참조 데이터와 강남구 경계로 대치동 학원 좌표 주변의 공영주차장 · 길가 후보를 돌려준다(차량 시간은 목)
- [x] GET /v1/pickup-spot-recommendations에 ISO 형식이 아닌 arriveAt을 보내면 422 VALIDATION_FAILED와 fields의 arriveAt을 돌려준다
- [x] 차량 시간 어댑터는 미래 운행 정보 길찾기에 summary 파라미터를 붙이지 않는다
- [x] 참조 데이터 로더는 테이블별 적재를 한 트랜잭션으로 해서 중간에 실패하면 그 테이블에 일부 행을 남기지 않는다
- [x] [기반] 블록 우회 도보 거리 규칙은 두 좌표 사이 남북 거리와 동서 거리를 더한 값을 m로 돌려준다(북쪽 300m · 동쪽 400m면 직선 500m가 아니라 700m)
- [x] [서비스] 장소 조회는 walkMinutes를 학원 출입구(없으면 학원 위치)에서 만남 지점까지 블록 우회 도보 거리를 초속 1.1m로 나눈 시간을 분으로 올림해 돌려준다
- [x] [persistence] LoadAcademyPort 어댑터는 schedule 학원의 출입구 좌표(entrance)를 place AcademyInfo.entrance로 넘기고, 출입구가 없으면 null로 넘긴다
- [ ] [서비스] 차량 세션용 임시 장소 저장 in 포트는 familyId · academyId · name · stopPoint · meetingPoint로 temporary=true인 PickupSpot을 저장하고 spotId를 돌려준다(최종디자인 11/13 V02 · V03, 팀장 결정 2026-11-13)
- [ ] [persistence] 가족 장소 목록(GET /v1/families/me/pickup-spots)과 기본 장소 판정은 temporary=true인 임시 장소를 빼고 돌려준다
- [ ] [web] GET /v1/pickup-spot-recommendations/nearby?academyId=는 출발 위치 없이 학원 기준 근처 후보를 name · location · walkMeters · walkSeconds로 걷는 거리 짧은 순으로 돌려준다(일정 등록 픽업일정등록8 화면용)
- [ ] [서비스] 학원 기준 근처 추천은 카카오 길찾기를 부르지 않고 driveSeconds · totalSeconds를 비운다. 지원 지역 밖 학원이면 supported false와 빈 목록을 준다
- [ ] [서비스] 장소 추천(SPOT-04 · 학원 기준 근처 추천)은 도로 이름이 빈 길가 후보의 name을 「이름 없는 길」로 돌려준다(null을 주지 않음, 사람 결정 2026-11-13)

## 확인 필요 사항

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
| 임시 장소 저장 in 포트 모양 | [팀장 결정 2026-11-14] SaveTemporarySpotUseCase.saveTemporarySpot(SaveTemporarySpotCommand(familyId, academyId, createdBy, name, stopPoint, meetingPoint)) → long spotId. createdBy는 pickup이 넘기는 가족 GUARDIAN userId(세션 뷰 driver와 같은 사람). memo는 null. 학원 거리 검사(SPOT_TOO_FAR) · 기본 장소 자동 지정 · 강등 · 개수 제한은 임시 장소에 걸지 않는다(추천 장소 · 차량 정차 위치라 사용자가 고른 값이 아님) |
| SPOT-04 참조 데이터 갱신 | CSV를 손으로 다시 만들어 넣음. 자동 갱신 없음 |
| SPOT-04 origin | 클라이언트가 차량 현재 좌표를 보냄. 서버가 가진 SIG-01 위치는 쓰지 않음 |
| SPOT-04 학원 위치 | 결정됨(사람 2026-11-12): 저장된 Academy를 academyId로 조회해 location을 씀. 출입구 좌표(entrance)는 아직 쓰지 않음. 저장된 값이 카카오 검색에서 온 것이라 저장 방식은 06_일정-schedule에서 따로 정함(미정) |
