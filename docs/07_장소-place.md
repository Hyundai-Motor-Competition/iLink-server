# 07. 장소 (place)

보호자가 지도에서 지정한 픽업 장소와 학원별 기본 장소. 가족(Family) 단위로 저장해 형제가 같은 학원이면 같이 쓴다. 학원은 schedule의 Academy를 참조한다.

| 항목 | 내용 |
|---|---|
| 범위 | 엔티티 PickupSpot. API SPOT-01(조회) · SPOT-02(추가) · SPOT-03(수정 · 삭제). 다른 모듈용 in 포트(기본 장소 · 장소 조회) |
| 의존 | 03_공유모듈-shared, 04_사용자-user(가족 · 작성자), 06_일정-schedule(Academy 위치. out 포트 LoadAcademyPort → schedule in 포트 LoadAcademyUseCase.findAcademy). pickup의 in 포트(진행 중 세션이 쓰는 장소, 자녀 현재 위치)는 out 포트 CheckActivePickupPort + 임시 어댑터(pickup 구현 전). schedule의 LoadPickupSpotPort는 place LoadPickupSpotUseCase.findSpots에 위임 |
| 진행 | 계층 병렬 |

## 구조

```text
place
├─ adapter/in/web          PickupSpotController(SPOT-01~03), PickupSpotApiDoc
├─ adapter/out/persistence PickupSpotEntity, PlacePersistenceAdapter(LoadSpotPort · SaveSpotPort),
│                          PostgisDistanceAdapter(DistancePort, ST_DWithin), PickupSpotJpaRepository(반경 네이티브 쿼리 2개)
├─ adapter/out/schedule    PlaceAcademyAdapter(LoadAcademyPort → schedule in 포트 LoadAcademyUseCase.findAcademy, academyId · name · location)
├─ adapter/out/pickup      PlacePickupActivityAdapter(CheckActivePickupPort, pickup 구현 전 임시: isUsedByActiveSession false, loadActiveChildLocation 빈 값)
├─ application/port/in · out · service
└─ domain                  PickupSpot, 기본 장소 규칙
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

familyId · academyId · createdBy는 프로젝트 관례대로 DB FK 제약 없이 컬럼만 둔다.
uk_pickup_spot_default는 JPA가 만들지 못하는 부분 유니크 인덱스라 02_공통-규칙-global 관례대로 src/main/resources/schema.sql에 CREATE UNIQUE INDEX IF NOT EXISTS로 적는다.

## API 명세

| ID | Method · 경로 | 호출 | Idempotency-Key | 요청 → 응답 | 오류 |
|---|---|---|---|---|---|
| SPOT-01 | GET /v1/families/me/pickup-spots | GUARDIAN · CO_GUARDIAN · DELEGATE, 차량 앱 | 불필요 | query academyId(선택), near(한 파라미터 "lat,lng" 형식 · radiusM과 함께), radiusM(50~3000 · near와 함께) → 200 academies[](academyId · academyName · spots[](spotId · name · stopPoint · meetingPoint · memo · isDefault · walkMinutes)). 차량 토큰은 토큰의 familyId | 422 VALIDATION_FAILED(near · radiusM 중 하나만, radiusM 50~3000 밖, near 형식 오류) |
| SPOT-02 | POST /v1/families/me/pickup-spots | GUARDIAN · CO_GUARDIAN | 필요 | academyId, name(1~50자), stopPoint{lat, lng}, meetingPoint{lat, lng}, memo(0~200자, 선택), isDefault(기본 false) → 201 spotId, isDefault | 404 NOT_FOUND(학원 없음), 409 DEFAULT_SPOT_CONFLICT(동시 요청으로 기본 장소가 겹침), 422 VALIDATION_FAILED, 422 SPOT_TOO_FAR |
| SPOT-03 | PATCH /v1/pickup-spots/{spotId} | GUARDIAN · CO_GUARDIAN | 불필요 | name, stopPoint, meetingPoint, memo, isDefault(모두 선택) → 200 spotId · name · stopPoint · meetingPoint · memo · isDefault | 404 NOT_FOUND(다른 가족 · 없는 spotId, 좌표를 바꿀 때 학원 없음), 409 DEFAULT_SPOT_CONFLICT(동시 요청으로 기본 장소가 겹침), 422 VALIDATION_FAILED, 422 SPOT_TOO_FAR, 422 DEFAULT_SPOT_REQUIRED(기본 장소를 isDefault false로) |
| SPOT-03 | DELETE /v1/pickup-spots/{spotId} | GUARDIAN · CO_GUARDIAN | 필요 | — → 204 | 404 NOT_FOUND, 409 SPOT_IN_USE, 422 DEFAULT_SPOT_REQUIRED |

응답 예시
- SPOT-01: `{"academies":[{"academyId":55,"academyName":"해솔영어학원","spots":[{"spotId":21,"name":"정문 앞","stopPoint":{"lat":37.501234,"lng":127.039876},"meetingPoint":{"lat":37.501301,"lng":127.039901},"memo":"편의점 앞","isDefault":true,"walkMinutes":null}]}]}`
- SPOT-02: `{"spotId":21,"isDefault":true}`

## 핵심 로직

### 조회 (SPOT-01)
- 내 가족(차량은 차량 토큰의 familyId)의 장소를 학원별로 묶어 돌려준다. academyId를 주면 그 학원 장소만.
- 순서: 학원 묶음은 academyId 오름차순, 묶음 안 장소는 spotId 오름차순.
- 학원 이름은 학원마다 한 번만 읽는다. 학원 정보가 없으면 academyName은 null.
- near · radiusM을 주면 정차 지점이 반경 안에 있는 장소만 공간 인덱스로 조회한다(PostGIS 네이티브 쿼리, ST_DWithin). 둘 중 하나만 주면 422 VALIDATION_FAILED(빠진 쪽을 fields에).
- walkMinutes는 계산 방법이 정해질 때까지 늘 null이다.

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
- [x] [persistence] pickup 구현 전 임시 CheckActivePickupPort는 isUsedByActiveSession false, loadActiveChildLocation 빈 값을 돌려준다
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

## 확인 필요 사항

| 항목 | 현재 가정 |
|---|---|
| 지난 세션이 가리키는 장소의 삭제 | DB FK가 없어 지금은 지워지고, 지난 세션의 spotId가 없는 행을 가리킬 수 있음. pickup 설계 때 삭제 대신 표시 등을 정함 |
| 반경 조회 기준 좌표 | near · radiusM이 정차 지점 기준인지 만남 지점 기준인지 소스에 없음. GIST가 걸린 정차 지점 기준으로 가정 |
| ST_MakeEnvelope(지도 범위) 조회 | 소스 2.2는 지도 범위 조회를 말하지만 API에는 near · radiusM만 있음. 범위(사각형) 파라미터는 두지 않음 |
| near · radiusM 중 하나만 온 경우 | 결정됨: 422 VALIDATION_FAILED(빠진 쪽을 fields에) |
| walkMinutes 계산 방법 | 걷는 속도 · 경로 계산 방법이 소스에 없음. 정해야 함. 지금은 늘 null |
| 유일한 기본 장소를 PATCH로 isDefault false로 바꾸는 경우 | 결정됨: 남은 장소 수와 상관없이 422 DEFAULT_SPOT_REQUIRED(다른 장소를 기본으로 지정해야 바뀜) |
| PATCH로 memo 지우기 | null은 「안 보냄」이라 지금은 못 지움. 빈 문자열을 「지움」으로 볼지 정해야 함 |
| 기본 장소 유일성을 DB로도 막을지 | 결정됨(사람): DB 부분 유니크 인덱스 uk_pickup_spot_default + 동시 요청으로 부딪히면 409 DEFAULT_SPOT_CONFLICT |
| 배포 전 기존 중복 기본 장소 정리 | 운영 DB에 같은 가족 · 학원의 기본 장소가 두 개 이상 있으면 인덱스 생성이 실패해 서버가 뜨지 않음. 배포 전에 중복을 찾아 하나만 남길지, 남길 기준(최근 수정 · 가장 작은 spotId 등)을 정해야 함 |
| DEFAULT_SPOT_CONFLICT 공통 오류 표 등록 | 02_공통-규칙-global의 오류 코드 표에 없음. place 문서에만 둔다고 가정 |
| SPOT-02의 academyId가 가족 일정에 없는 학원인 경우 | 그대로 둠: Academy가 있으면 받고, 없으면 404 |
| 장소 변경이 settingsVersion을 올리는지 | 04_사용자-user.md와 같은 질문. 올리지 않는다고 가정 |
| 11/4 기획 수정안(소스 7.1) | 확정되면 학원 출입구를 기본 장소로 쓰고 직접 찍는 장소는 대안 0~2개가 됨 → 「첫 장소가 기본」 규칙이 바뀜 |
