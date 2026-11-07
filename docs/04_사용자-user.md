# 04. 사용자 (user)

보호자 · 자녀 · 선생님 · 대리보호자와 전화번호, 가족과 역할(role), 차량(VIN), 차량 연결 해제, 합류 안내 방식. 차량은 가족이 소유한 자산이라 Family 아래에 둔다.

| 항목 | 내용 |
|---|---|
| 범위 | 엔티티 Family · User · Vehicle. API FAM-01~03 · FAM-07(가족 멤버), LINK-05(차량 연결 해제), SET-06(합류 안내 방식). 다른 모듈용 in 포트(보호자 · 가족 생성, 차량 등록 · 조회, 멤버 · 가족 조회) |
| 의존 | 03_공유모듈-shared. auth의 in 포트(멤버 기기 상태 조회, 멤버 해제 때 기기 · 토큰 폐기), pickup의 in 포트(진행 중 픽업 여부). 둘 다 user out 포트로 두고 구현 전에는 테스트 대역으로 검증 |
| 진행 | 계층 병렬 |

## 구조

```text
user
├─ adapter/in/web          FamilyController(FAM-01~03 · FAM-07 · SET-06), VehicleLinkController(LINK-05), doc 인터페이스 2개
├─ adapter/out/persistence FamilyEntity · UserEntity(users) · VehicleEntity, 저장소, 전화번호 암호화 변환기, out 포트 구현
├─ adapter/out/(모듈 연결) auth · pickup in 포트를 부르는 out 포트 구현
├─ application/port/in     아래 「시그니처」
├─ application/port/out    아래 「시그니처」
├─ application/service     in 포트 구현
└─ domain                  Family, User, Vehicle, JoinCallMode, 이름 · 전화번호 규칙(Role은 common.security.Role을 씀)
```

## 테이블

가족 (Family)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| familyId | BIGINT | PK | |
| joinCallMode | VARCHAR(20) | 기본 AUTO | AUTO · GUARDIAN_CONFIRM |
| settingsVersion | INT | 기본 0 | 설정이 바뀔 때마다 +1 |

사용자 (User, 테이블 users)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| userId | BIGINT | PK | |
| familyId | BIGINT | FK → Family | |
| role | VARCHAR(20) | | GUARDIAN · CO_GUARDIAN · DELEGATE · TEACHER · CHILD |
| name | VARCHAR(20) | | 1~20자 |
| phone | VARCHAR(255) | NULL | 국제 형식(+8210…). AES-256-GCM 암호문(nonce · 태그 포함, Base64) |

차량 (Vehicle)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| vehicleId | BIGINT | PK | |
| familyId | BIGINT | FK → Family, NULL | 연결 승인 전에는 비어 있음 |
| vin | CHAR(17) | 연결 중인 행끼리 UNIQUE | 차대번호 |
| model | VARCHAR(50) | | |
| plateNumber | VARCHAR(20) | | |
| unlinkedAt | TIMESTAMPTZ | NULL | 연결 해제 시각 |

- VIN 유니크는 연결 중인 행끼리만 건다. schema.sql: `CREATE UNIQUE INDEX IF NOT EXISTS … ON vehicle (vin) WHERE unlinked_at IS NULL`.
- 연결을 해제해도 행은 지우지 않고 unlinkedAt만 채운다. 같은 차를 다시 연결하면 새 행(새 vehicleId)이 생겨 가족별 픽업 기록이 섞이지 않는다.

## API 명세

| ID | Method · 경로 | 호출 | Idempotency-Key | 요청 → 응답 | 오류 |
|---|---|---|---|---|---|
| FAM-01 | GET /v1/families/me | GUARDIAN · CO_GUARDIAN | 불필요 | — → 200 familyId, joinCallMode, vehicle(vehicleId · model · plateNumberMasked, 없으면 null), members[](userId · role · name · phone · deviceStatus) | 공통 |
| FAM-02 | POST /v1/families/me/members | GUARDIAN · CO_GUARDIAN | 필요 | role(CHILD · CO_GUARDIAN · DELEGATE · TEACHER), name(1~20자, 이모지 · 제어문자 불가), phone(국제 형식, 자녀 외 필수) → 201 userId | 422 MEMBER_LIMIT_EXCEEDED(자녀 외 멤버가 이미 5명), 403 FORBIDDEN(공동 보호자가 공동 보호자를 추가) |
| FAM-03 | PATCH /v1/families/me/members/{userId} | GUARDIAN · CO_GUARDIAN | 불필요 | name, phone, role(모두 선택, 보낸 필드만 바뀜) → 200 userId · role · name · phone · deviceStatus | 422 전화번호 형식 오류, role 변경은 GUARDIAN만(403) |
| FAM-07 | DELETE /v1/families/me/members/{userId} | GUARDIAN | 필요 | — → 204 | 409 MEMBER_IN_ACTIVE_PICKUP, 422 CANNOT_REMOVE_GUARDIAN |
| LINK-05 | DELETE /v1/vehicle-links/{vehicleId} | GUARDIAN | 필요 | — → 204 | 409 ACTIVE_PICKUP_EXISTS |
| SET-06 | PUT /v1/families/me/call-mode | GUARDIAN · CO_GUARDIAN | 불필요 | joinCallMode(AUTO · GUARDIAN_CONFIRM) → 200 joinCallMode, settingsVersion | 공통 |

응답 예시
- FAM-01: `{"familyId":12,"joinCallMode":"AUTO","vehicle":{"vehicleId":77,"model":"아이오닉 5","plateNumberMasked":"12가 ****"},"members":[{"userId":101,"role":"GUARDIAN","name":"김보호","phone":"+821012345678","deviceStatus":"CONNECTED"},{"userId":131,"role":"CHILD","name":"민준","phone":null,"deviceStatus":"INVITE_PENDING"}]}`
- FAM-03: `{"userId":131,"role":"CHILD","name":"민준","phone":null,"deviceStatus":"CONNECTED"}`
- SET-06: `{"joinCallMode":"GUARDIAN_CONFIRM","settingsVersion":8}`

## 핵심 로직

### 멤버
- FAM-02는 자녀 등록과 인계자 추가를 하나로 합친 것이다. 앱을 안 쓰는 선생님도 전화번호만으로 등록한다(기기 없음 → deviceStatus NONE).
- 자녀 외 멤버(CHILD가 아닌 role)는 가족당 최대 5명. 넘으면 422 MEMBER_LIMIT_EXCEEDED.
- CO_GUARDIAN은 멤버를 추가할 수 있지만 CO_GUARDIAN은 추가하지 못한다(403 FORBIDDEN).
- 이름은 1~20자, 이모지 · 제어문자 불가. 전화번호는 국제 형식(+8210…), CHILD만 생략 가능.
- FAM-03은 보낸 필드만 바꾼다. role 변경은 GUARDIAN만 하고, GUARDIAN 자신의 role은 바꾸지 못한다.
- FAM-07은 GUARDIAN만. 자기 자신은 422 CANNOT_REMOVE_GUARDIAN, 진행 중 픽업에 묶인 멤버는 409 MEMBER_IN_ACTIVE_PICKUP. 해제하면 그 멤버의 기기와 토큰을 auth in 포트로 함께 폐기한다.
- 다른 가족의 userId는 404 NOT_FOUND.

### deviceStatus (FAM-01 · FAM-03 · SET-07)
- 값: CONNECTED · INVITE_PENDING · INVITE_EXPIRED · NONE(기기 없음).
- auth in 포트로 멤버별 상태를 받아 채운다. 판정 규칙은 auth 쪽(05_인증-auth.md 핵심 로직)에 둔다.

### 차량
- AUTH-02(auth)가 차량 기기를 등록할 때 Vehicle 행을 familyId 없이 만든다(RegisterVehicleUseCase).
- LINK-04 NEW(auth)가 승인되면 Family와 User(GUARDIAN)를 만들고 Vehicle.familyId를 채운다(CreateGuardianUseCase).
- LINK-05: GUARDIAN만, 진행 중 픽업이 있으면 409 ACTIVE_PICKUP_EXISTS, 성공하면 unlinkedAt만 채운다. 해제 뒤 같은 VIN은 AUTH-02에서 새 행으로 등록된다.
- 번호판 마스킹: 뒷자리를 가린다(예: 「12가 ****」).

### 합류 안내 방식
- SET-06은 GUARDIAN · CO_GUARDIAN만. Family.joinCallMode를 바꾸고 settingsVersion을 1 올린다.
- 기본값이 AUTO라 처음 설정 흐름에서는 부르지 않는다. 다음 세션부터 적용된다.

### 전화번호 암호화
- users.phone은 AES-256-GCM 암호문(nonce · 태그 포함, Base64)으로 저장하고, 키는 환경 변수로 받는다. JPA 변환기로 저장 때 암호화, 읽을 때 복호화한다.

### 시그니처

```java
// in 포트 — web
FamilyResult getMyFamily(GetMyFamilyQuery query);                 // GetMyFamilyUseCase
long registerMember(RegisterMemberCommand command);               // RegisterMemberUseCase
MemberResult updateMember(UpdateMemberCommand command);           // UpdateMemberUseCase
void removeMember(RemoveMemberCommand command);                   // RemoveMemberUseCase
void unlinkVehicle(UnlinkVehicleCommand command);                 // UnlinkVehicleUseCase
CallModeResult changeCallMode(ChangeCallModeCommand command);     // ChangeCallModeUseCase

// in 포트 — 다른 모듈용
GuardianAccount createGuardian(CreateGuardianCommand command);    // CreateGuardianUseCase (auth LINK-04 NEW: Family · GUARDIAN 생성, 차량 연결)
long registerVehicle(String vin, String model);                   // RegisterVehicleUseCase (auth AUTH-02)
Optional<VehicleInfo> findVehicle(long vehicleId);                // LoadVehicleUseCase
Optional<VehicleInfo> findLinkedVehicleByVin(String vin);         // LoadVehicleUseCase
Optional<MemberInfo> findMember(long userId);                     // LoadMemberUseCase
List<MemberInfo> findMembers(long familyId);                      // LoadMemberUseCase
FamilyInfo findFamily(long familyId);                             // LoadFamilyUseCase (joinCallMode · settingsVersion)

// out 포트
Optional<Family> loadFamily(long familyId);  Family saveFamily(Family family);          // LoadFamilyPort · SaveFamilyPort
Optional<User> loadUser(long userId);  List<User> loadUsers(long familyId);  User saveUser(User user);  void deleteUser(long userId);   // LoadUserPort · SaveUserPort
long countNonChildMembers(long familyId);                                                  // LoadUserPort
Optional<Vehicle> loadVehicle(long vehicleId);  Optional<Vehicle> loadLinkedVehicleByVin(String vin);  Optional<Vehicle> loadLinkedVehicleOfFamily(long familyId);  Vehicle saveVehicle(Vehicle vehicle);   // LoadVehiclePort · SaveVehiclePort
Map<Long, DeviceStatus> loadDeviceStatuses(List<Long> userIds);   // LoadMemberDeviceStatusPort → auth
void revokeDevicesOf(long userId);                                 // RevokeMemberDevicesPort → auth
boolean isInActivePickup(long userId);  boolean hasActivePickup(long vehicleId);   // CheckActivePickupPort → pickup
```

## 테스트 케이스
- [x] [기반] Role은 GUARDIAN · CO_GUARDIAN · DELEGATE · TEACHER · CHILD 다섯 값을 갖는다
- [x] [기반] JoinCallMode는 AUTO · GUARDIAN_CONFIRM 두 값을 갖는다
- [x] [기반] 새로 만든 Family의 joinCallMode는 AUTO이고 settingsVersion은 0이다
- [x] [기반] Family.changeCallMode를 부르면 joinCallMode가 바뀌고 settingsVersion이 1 오른다
- [x] [기반] Vehicle.unlink를 부르면 unlinkedAt이 채워지고 vehicleId · vin은 그대로다
- [x] [기반] unlinkedAt이 채워진 Vehicle은 연결 중이 아니다
- [x] [기반] 이름 규칙은 0자와 21자 이름을 거절하고 1자와 20자 이름을 받는다
- [x] [기반] 이름 규칙은 이모지나 제어문자가 든 이름을 거절한다
- [x] [기반] 전화번호 규칙은 +821012345678을 받고 01012345678을 거절한다
- [x] [기반] 번호판 마스킹은 「12가 3456」을 「12가 ****」로 바꾼다
- [x] [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] [기반] FamilyEntity · UserEntity · VehicleEntity가 BaseTimeEntity를 상속하고 UserEntity는 users 테이블에 매핑된다
- [x] [기반] 테스트 시드로 가족 1개 · GUARDIAN 1명(김보호) · CHILD 2명(민준 · 서윤) · 연결된 차량 1대(아이오닉 5)를 만든다
- [ ] [persistence] Family · User · Vehicle을 저장한 뒤 다시 읽으면 도메인 객체의 값이 같다
- [x] [persistence] users.phone 컬럼에는 평문이 아닌 암호문이 저장되고 읽으면 원래 번호로 돌아온다
- [x] [persistence] 같은 전화번호를 두 번 저장하면 두 암호문이 서로 다르다
- [ ] [persistence] 서버가 뜨면 schema.sql의 VIN 부분 유니크 인덱스가 vehicle 테이블에 만들어진다
- [ ] [persistence] unlinkedAt이 빈 같은 VIN 차량 행을 두 개 저장하면 유니크 위반이 난다
- [ ] [persistence] 같은 VIN이라도 기존 행의 unlinkedAt이 채워져 있으면 새 행이 저장된다
- [ ] [persistence] loadLinkedVehicleByVin은 unlinkedAt이 빈 행만 찾는다
- [ ] [persistence] loadLinkedVehicleOfFamily는 그 가족의 연결 중인 차량을 찾는다
- [ ] [persistence] countNonChildMembers는 CHILD를 뺀 가족 멤버 수를 센다
- [ ] [서비스] 멤버 등록은 자녀 외 멤버가 이미 5명이면 MEMBER_LIMIT_EXCEEDED로 거절한다
- [ ] [서비스] 멤버 등록은 자녀 외 멤버가 5명이어도 CHILD는 등록한다
- [ ] [서비스] CO_GUARDIAN이 CO_GUARDIAN을 등록하려 하면 FORBIDDEN으로 거절한다
- [ ] [서비스] CHILD가 아닌 멤버를 phone 없이 등록하려 하면 거절한다
- [ ] [서비스] CHILD는 phone 없이 등록된다
- [ ] [서비스] 기기 없이 등록한 TEACHER의 deviceStatus는 NONE이다
- [ ] [서비스] 멤버 수정은 보낸 필드만 바꾸고 보내지 않은 필드는 그대로 둔다
- [ ] [서비스] CO_GUARDIAN이 멤버의 role을 바꾸려 하면 FORBIDDEN으로 거절한다
- [ ] [서비스] GUARDIAN이 자기 자신의 role을 바꾸려 하면 거절한다
- [ ] [서비스] 다른 가족의 userId를 수정하려 하면 NOT_FOUND로 거절한다
- [ ] [서비스] 멤버 해제는 GUARDIAN 자신을 대상으로 하면 CANNOT_REMOVE_GUARDIAN으로 거절한다
- [ ] [서비스] 멤버 해제는 진행 중 픽업에 묶인 멤버면 MEMBER_IN_ACTIVE_PICKUP으로 거절한다
- [ ] [서비스] 멤버 해제가 성공하면 RevokeMemberDevicesPort로 그 멤버의 기기 · 토큰 폐기를 요청한다
- [ ] [서비스] 가족 조회는 멤버별 deviceStatus를 LoadMemberDeviceStatusPort 결과로 채운다
- [ ] [서비스] 가족 조회는 연결 중인 차량이 없으면 vehicle을 null로 돌려준다
- [ ] [서비스] 차량 연결 해제는 진행 중 픽업이 있으면 ACTIVE_PICKUP_EXISTS로 거절한다
- [ ] [서비스] 차량 연결 해제가 성공하면 unlinkedAt만 채우고 행은 남긴다
- [ ] [서비스] 합류 안내 방식 변경은 settingsVersion을 1 올린 값을 돌려준다
- [ ] [서비스] createGuardian은 Family와 role GUARDIAN인 User를 만들고 Vehicle.familyId를 그 가족으로 채운다
- [ ] [서비스] registerVehicle은 familyId가 빈 Vehicle 행을 만들고 vehicleId를 돌려준다
- [ ] [web] GET /v1/families/me는 200과 familyId · joinCallMode · vehicle · members[]를 돌려준다
- [ ] [web] GET /v1/families/me를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] POST /v1/families/me/members는 201과 userId를 돌려준다
- [ ] [web] POST /v1/families/me/members에 21자 name을 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려준다
- [ ] [web] POST /v1/families/me/members가 한도를 넘으면 422 MEMBER_LIMIT_EXCEEDED를 돌려준다
- [ ] [web] PATCH /v1/families/me/members/{userId}는 200과 userId · role · name · phone · deviceStatus를 돌려준다
- [ ] [web] PATCH /v1/families/me/members/{userId}에 형식이 틀린 phone을 보내면 422를 돌려준다
- [ ] [web] DELETE /v1/families/me/members/{userId}는 204를 돌려준다
- [ ] [web] DELETE /v1/families/me/members/{userId}를 CO_GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] DELETE /v1/families/me/members/{userId}가 진행 중 픽업 멤버면 409 MEMBER_IN_ACTIVE_PICKUP을 돌려준다
- [ ] [web] DELETE /v1/vehicle-links/{vehicleId}는 204를 돌려준다
- [ ] [web] DELETE /v1/vehicle-links/{vehicleId}를 CO_GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] DELETE /v1/vehicle-links/{vehicleId}가 진행 중 픽업이 있으면 409 ACTIVE_PICKUP_EXISTS를 돌려준다
- [ ] [web] PUT /v1/families/me/call-mode는 200과 joinCallMode · settingsVersion을 돌려준다
- [ ] [web] PUT /v1/families/me/call-mode를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] Idempotency-Key는 POST /v1/families/me/members · DELETE /v1/families/me/members/{userId} · DELETE /v1/vehicle-links/{vehicleId}에만 요구된다
- [x] [기반] Family.restore는 familyId · joinCallMode · settingsVersion을 그대로 가진 Family를 만든다
- [x] [기반] User.create는 userId가 빈 User를 만들고 User.restore는 userId · familyId · role · name · phone을 그대로 가진 User를 만든다
- [x] [기반] User.changeName · changePhone · changeRole은 그 필드만 바꾸고 나머지는 그대로 둔다
- [x] [기반] Vehicle.register는 familyId · unlinkedAt이 빈 Vehicle을 만들고 Vehicle.linkTo는 familyId를 채운다
- [x] [기반] auth · pickup 구현 전 임시 어댑터가 빈으로 등록된다: LoadMemberDeviceStatusPort는 모든 userId에 NONE, RevokeMemberDevicesPort는 아무것도 하지 않음, CheckActivePickupPort는 false

## 확인 필요 사항

| 항목 | 현재 가정 |
|---|---|
| 「자녀 외 멤버 최대 5명」에 GUARDIAN 자신이 들어가는지 | 소스에 없음. GUARDIAN 포함 CHILD가 아닌 멤버 전체를 센다고 가정 |
| GUARDIAN이 자기 role을 바꾸려 할 때 오류 코드 | 소스에 없음(「못 바꿈」만 있음). 422 VALIDATION_FAILED로 가정 |
| 전화번호 국제 형식 범위 | 예시가 +8210…뿐. +로 시작하는 국제 형식(E.164)을 받는다고 가정 |
| 멤버 해제 때 User 행 처리 | 소스에 없음. SessionChild · PickupSchedule이 User를 FK로 가리켜 지난 기록이 있는 멤버는 행을 지울 수 없음. 삭제 대신 표시만 하는 방식이 필요한지 정해야 함 |
| 멤버 해제 때 그 자녀의 일정 · 장소 처리 | 소스에 없음. 그대로 둔다고 가정 |
| LINK-05를 차량 앱이 부를 수 있는지 | 4.3 표의 사용 화면에 「차량 설정 요약 화면」이 있으나 4.14 호출은 GUARDIAN뿐. 4.14대로 USER 주체 GUARDIAN만 허용 |
| LINK-05 때 차량 기기 토큰 폐기 | 소스에 없음. 폐기하지 않는다고 가정 |
| settingsVersion을 올리는 변경의 범위 | SET-06만 명시됨. 일정(SET-02 · 03) · 장소(SPOT-02 · 03) · 멤버 변경도 올리는지 정해야 함(SET-08 충돌 판단에 영향) |
| 번호판 마스킹 규칙 | 예시 「12가 ****」뿐. 마지막 숫자 4자리를 *로 바꾼다고 가정 |
| user ↔ auth 서로 부름 | auth는 User 생성을 user에 요청하고, user는 기기 상태 · 폐기를 auth에 요청. user 쪽은 out 포트로 두고 auth 구현 전에는 테스트 대역으로 검증 |
| 진행 중 픽업 확인(FAM-07 · LINK-05)을 pickup in 포트로 부르는 것 | pickup이 user보다 뒤에 구현되므로 out 포트 + 테스트 대역으로 먼저 검증 |
| 포트 · 유스케이스 이름 | 소스 2.2 예시(StartPickupUseCase, LoadSessionPort)의 이름 규칙을 따른 제안 |
| LINK-04 NEW로 만드는 GUARDIAN의 name | LINK-04 요청에 이름이 없음(05_인증-auth.md에도 기록). 이름을 받을 곳을 정해야 함 |
| Role enum 위치 | 해소: 02에 「common 순수 타입(Role · PrincipalType · LatLng)은 domain에서 써도 된다」 예외를 둠. user/domain은 common.security.Role을 그대로 쓰고 Role enum을 따로 만들지 않음(사람 결정 2026-11-07, 03 확인 필요 사항) |
