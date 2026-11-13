# 05. 인증 (auth)

QR 연결(가입 · 로그인 · RELINK), 멤버 초대 · 수락, 토큰 발급 · 갱신 · 폐기, 기기 · 푸시 토큰. 별도 로그인 화면이 없어 로그인 API도 없다. 보호자 토큰은 LINK-04, 자녀 · 선생님 · 대리보호자 토큰은 FAM-06, 차량 토큰은 LINK-02 응답에서 발급한다.

| 항목 | 내용 |
|---|---|
| 범위 | 엔티티 VehicleLinkCode · MemberInvite · Device(refresh 토큰 해시 포함). API AUTH-01~04, LINK-01~04, FAM-04~06. 다른 모듈용 in 포트(멤버 기기 상태, 멤버 기기 폐기, 차량 기기 폐기, 푸시 대상 기기 조회) |
| 의존 | 03_공유모듈-shared(JWT 검증), 04_사용자-user(보호자 · 가족 생성, 차량 등록 · 조회, 멤버 조회). pickup의 in 포트(AUTH-04 진행 중 픽업 여부)는 out 포트 CheckActivePickupPort → pickup ActivePickupQueryUseCase로 연동됨(사람 결정 2026-11-13) |
| 진행 | 계층 병렬 |

## 구조

```text
auth
├─ adapter/in/web          AuthController(AUTH-01 · 03), VehicleDeviceController(AUTH-02 · 04), VehicleLinkCodeController(LINK-01~04), InviteController(FAM-04~06), doc 인터페이스
├─ adapter/out/persistence VehicleLinkCodeEntity · MemberInviteEntity · DeviceEntity, 저장소
├─ adapter/out/token       JWT 발급(access), refresh 토큰 생성 · SHA-256 해시
├─ adapter/out/event       RELINK 이전 기기 알림 이벤트 발행
├─ adapter/out/pickup      AuthPickupActivityAdapter(CheckActivePickupPort 구현 → pickup ActivePickupQueryUseCase.hasActivePickup)(사람 결정 2026-11-13)
├─ application/port/in · out · service
└─ domain                  LinkCode, MemberInvite, Device, Consent, MemberDeviceStatus · 기기 상태 판정
```

## 테이블

차량 연결 코드 (VehicleLinkCode)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| codeId | BIGINT | PK | |
| vehicleId | BIGINT | FK → Vehicle | |
| code | CHAR(8) | INDEX | QR · 직접 입력용 8자 |
| status | VARCHAR(20) | | ISSUED · USED · EXPIRED |
| expiresAt | TIMESTAMPTZ | | 발급 후 5분 |
| usedAt | TIMESTAMPTZ | NULL | |
| issuedDeviceId | BIGINT | NULL | 코드를 발급한 차량 기기. 이 컬럼이 생기기 전의 옛 코드는 null |
| issuedBeforeLink | BOOLEAN | | 연결 전 토큰(familyId 없음)으로 발급했는지 |
| mode | VARCHAR(10) | NULL | NEW · RELINK. LINK-04 승인 때 채움 |
| tokenDeliveredAt | TIMESTAMPTZ | NULL | LINK-02가 차량 토큰을 내려준 시각 |

멤버 초대 (MemberInvite)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| inviteId | BIGINT | PK | |
| userId | BIGINT | FK → User | 초대받는 멤버 |
| token | VARCHAR(64) | UNIQUE | 초대 링크 토큰 |
| expiresAt | TIMESTAMPTZ | | 발급 후 24시간 |
| usedAt | TIMESTAMPTZ | NULL | |

기기 (Device)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| deviceId | BIGINT | PK | |
| ownerType | VARCHAR(10) | | USER · VEHICLE |
| ownerId | BIGINT | INDEX(ownerType, ownerId) | userId 또는 vehicleId |
| platform | VARCHAR(10) | | ANDROID · IOS · VEHICLE |
| pushToken | VARCHAR(255) | NULL | FCM 토큰 |
| pushUpdatedAt | TIMESTAMPTZ | NULL | |
| refreshTokenHash | CHAR(64) | | refresh 토큰의 SHA-256 해시 |
| revokedAt | TIMESTAMPTZ | NULL | 폐기 시각 |

## API 명세

| ID | Method · 경로 | 호출 | Idempotency-Key | 요청 → 응답 | 오류 |
|---|---|---|---|---|---|
| AUTH-01 | POST /v1/auth/refresh | 모든 앱(refresh 토큰으로 인증) | 불필요 | refreshToken → 200 accessToken, refreshToken(이전 것 폐기), accessTokenExpiresAt | 401 REFRESH_TOKEN_INVALID |
| AUTH-02 | POST /v1/vehicle-devices | 차량 앱(인증 없음) | 필요 | vin(17자, 영대문자 · 숫자, I · O · Q 제외), model(1~50자) → 201 deviceId, vehicleId, deviceToken | 409 VEHICLE_ALREADY_LINKED |
| AUTH-03 | PUT /v1/devices/me/push-token | 보호자 · 자녀 앱, 차량 앱 | 불필요 | platform(ANDROID · IOS · VEHICLE), pushToken(1~255자) → 204 | 공통 |
| AUTH-04 | DELETE /v1/vehicle-devices/me/session | 차량 앱 | 불필요 | — → 204 | 409 ACTIVE_PICKUP_EXISTS |
| LINK-01 | POST /v1/vehicle-link-codes | 차량 앱(연결 전 토큰 가능) | 불필요 | — → 201 codeId, code(8자), qrPayload, expiresAt(5분), reissuesLeft | 409 VEHICLE_ALREADY_LINKED(연결 전 토큰인데 차량이 이미 가족에 연결됨), 409 VEHICLE_UNLINKED(연결이 해제된 차량 행의 기기), 429 LINK_CODE_REISSUE_LIMIT |
| LINK-02 | GET /v1/vehicle-link-codes/{codeId}/status | 차량 앱(코드를 발급한 기기) | 불필요 | — → 200 status(WAITING · LINKED · RELINKED · EXPIRED), vehicleId, familyId, accessToken, refreshToken(LINKED · RELINKED일 때 한 번만), pollAfterSeconds(WAITING일 때, 기본 2) | 403 NOT_CODE_OWNER, 404 LINK_CODE_INVALID, 429 TOO_MANY_REQUESTS(1초보다 자주) |
| LINK-03 | GET /v1/vehicle-link-codes/by-code/{code} | 보호자 앱(인증 없음) | 불필요 | — → 200 status(NEW · RELINK), vehicleModel, plateNumberMasked, expiresAt | 404 LINK_CODE_INVALID, 410 LINK_CODE_EXPIRED, 429 LINK_CODE_ATTEMPT_LIMIT |
| LINK-04 | POST /v1/vehicle-links | 보호자 앱(인증 없음) | 필요 | code, mode(NEW · RELINK), name(mode NEW일 때 필수, 1~20자, 이모지 불가, RELINK면 무시), consents[](type TERMS · PRIVACY · MARKETING, agreed), platform(ANDROID · IOS), pushToken(선택) → 201 vehicleId, familyId, userId, accessToken, refreshToken | 404 LINK_CODE_INVALID, 410 LINK_CODE_EXPIRED, 409 LINK_MODE_MISMATCH, 409 VEHICLE_ALREADY_LINKED(같은 대기 차량에 NEW 승인이 동시에 옴), 422 CONSENT_REQUIRED, 422 VEHICLE_MOVING(미구현, 확인 필요 사항 참조) |
| FAM-04 | POST /v1/families/me/members/{userId}/invites | GUARDIAN · CO_GUARDIAN | 필요 | replaceDevice(기본 false) → 201 inviteUrl, expiresAt(24시간) | 403 FORBIDDEN(대상 멤버 role이 GUARDIAN), 409 DEVICE_ALREADY_CONNECTED |
| FAM-05 | GET /v1/invites/{token} | 자녀 · 선생님 · 대리보호자 앱(인증 없음) | 불필요 | — → 200 inviterName, memberName, role, requiredConsents[], expiresAt | 409 INVITE_ALREADY_USED, 410 INVITE_EXPIRED |
| FAM-06 | POST /v1/invites/{token}/acceptance | 자녀 · 선생님 · 대리보호자 앱(인증 없음) | 필요 | consents[](type, agreed), platform(ANDROID · IOS), pushToken(선택) → 201 userId, familyId, role, accessToken, refreshToken | 409 INVITE_ALREADY_USED, 410 INVITE_EXPIRED, 422 CONSENT_REQUIRED |

응답 예시
- LINK-01: `{"codeId":5012,"code":"K7P2QX9M","qrPayload":"pickup://link?code=K7P2QX9M","expiresAt":"2026-11-05T21:10:00+09:00","reissuesLeft":4}`
- LINK-02: `{"status":"LINKED","vehicleId":77,"familyId":12,"accessToken":"eyJ…","refreshToken":"rt_…","pollAfterSeconds":null}`
- LINK-03: `{"status":"NEW","vehicleModel":"아이오닉 5","plateNumberMasked":"12가 ****","expiresAt":"2026-11-05T21:10:00+09:00"}`
- FAM-05: `{"inviterName":"김보호","memberName":"민준","role":"CHILD","requiredConsents":["TERMS","PRIVACY","LOCATION","PUSH"],"expiresAt":"2026-11-06T21:05:00+09:00"}`

## 핵심 로직

### 토큰
- access 토큰(JWT): 주체(VEHICLE · USER), familyId, role을 담는다. 만료 시각은 응답 accessTokenExpiresAt으로 알린다.
- refresh 토큰: 기기(Device)마다 하나. 원문은 내려주기만 하고 DB에는 SHA-256 해시(refreshTokenHash)만 저장한다.
- AUTH-01은 회전 방식이다. 새 access · refresh를 주고 이전 refresh는 폐기한다. 만료 · 폐기 · 모르는 토큰이면 401 REFRESH_TOKEN_INVALID. 보호자는 QR 재연결, 자녀는 재초대로 복구한다.
- 연결 전 기기 토큰(AUTH-02 deviceToken)은 LINK-01 · LINK-02만 부를 수 있다.

### 차량 기기 등록 (AUTH-02)
- 이미 보호자가 연결된 VIN(familyId가 있고 unlinkedAt이 빈 행)은 409 VEHICLE_ALREADY_LINKED. VIN만 알면 AUTH-02 → LINK-01 → LINK-04(RELINK)로 남의 계정을 가져가는 것을 막는다.
- 이 409는 RELINK 탈취만 막는다. 아직 연결 안 된 차량을 VIN만 알고 AUTH-02 → LINK-01 → LINK-04(NEW)로 먼저 차지하는 것은 막지 못한다(확인 필요 사항 「VIN만 아는 사람의 NEW 선점」).
- 연결 대기 행(familyId가 빈 행)이 있으면 그 행을 쓰고, 없으면 user의 RegisterVehicleUseCase로 새 행을 만든다(VIN 부분 유니크 때문에 대기 행은 하나뿐).
- Device(ownerType VEHICLE, platform VEHICLE)를 만들고 deviceToken을 준다.
- 차량 앱을 다시 설치해 기기 토큰을 잃으면 보호자가 LINK-05로 해제한 뒤 NEW로 다시 연결한다.
- LINK-05(user)로 해제하면 그 차량의 살아 있는 기기를 모두 폐기한다. 해제된 차량 앱은 refresh로 이전 가족의 토큰을 받을 수 없고 AUTH-02로 다시 등록한다(사람 결정 2026-11-07).

### 푸시 토큰 · 차량 로그아웃
- AUTH-03은 지금 기기의 pushToken · pushUpdatedAt을 바꾼다. 첫 등록은 LINK-04 · FAM-06 요청에 실려 오고, 이 API는 앱 실행 때와 Firebase onNewToken 때의 갱신용이다.
- AUTH-04는 차량 기기를 폐기(revokedAt)한다. 진행 중 픽업이 있으면 409 ACTIVE_PICKUP_EXISTS.

### QR 연결
- LINK-01: 8자 코드를 ISSUED로 만들고 expiresAt = 발급 + 5분. 발급한 기기를 issuedDeviceId에, 연결 전 토큰으로 발급했는지를 issuedBeforeLink에 기록한다. qrPayload = `pickup://link?code={code}`.
  - 새 코드를 받으면 같은 기기가 발급한 이전 ISSUED 코드와 발급 기기 정보가 없는 옛 ISSUED 코드(issuedDeviceId null)만 바로 EXPIRED. 같은 차량의 다른 기기가 발급한 코드는 그대로 둔다.
  - 발급 횟수는 기기(deviceId) 단위로 센다. 같은 기기가 연속 5번 넘게 발급하면 429 LINK_CODE_REISSUE_LIMIT. reissuesLeft는 남은 재발급 횟수(첫 발급 뒤 4).
  - 연결 전 토큰(familyId 없음)인데 그 차량이 이미 가족에 연결돼 있으면 409 VEHICLE_ALREADY_LINKED.
  - 연결이 해제된 차량 행(unlinkedAt 있음)의 기기면 토큰 종류와 상관없이 409 VEHICLE_UNLINKED. 차량 앱을 다시 등록(AUTH-02)해야 한다.
- RELINK 코드는 이미 연결된 차량 기기만 만들 수 있다. 연결 전 토큰으로 만든 코드(issuedBeforeLink)는 항상 NEW다.
- LINK-02: 코드를 발급한 기기(issuedDeviceId)가 아니면 403 NOT_CODE_OWNER. 승인 전 WAITING(pollAfterSeconds 2), 만료 EXPIRED, 승인되면 LINKED(mode NEW) 또는 RELINKED(mode RELINK)와 차량 토큰을 발급한 기기에만 한 번 내려주고 tokenDeliveredAt을 채운다. tokenDeliveredAt이 찬 코드를 다시 조회하면 404 LINK_CODE_INVALID. 같은 기기가 1초보다 자주 부르면 429 TOO_MANY_REQUESTS(기기 단위로 센다).
- LINK-03: 연결 전 토큰으로 발급한 코드(issuedBeforeLink)는 항상 NEW. 그 밖의 코드는 연결 중인 차량(unlinkedAt이 빈 행)만 보고 정한다. 가족에 연결돼 있으면 RELINK, 아니면 NEW. 없는 코드 404 LINK_CODE_INVALID, 만료 · 사용된 코드 410 LINK_CODE_EXPIRED. 같은 기기에서 5번 실패하면 1분 동안 429 LINK_CODE_ATTEMPT_LIMIT. 연결은 만들지 않는다.
- LINK-04: 코드를 다시 검증한다. mode가 실제 상태와 다르면 409 LINK_MODE_MISMATCH(연결 전 토큰으로 발급한 코드를 RELINK로 보내도 409 LINK_MODE_MISMATCH), 필수 동의(TERMS · PRIVACY) 누락 422 CONSENT_REQUIRED, 차량 주행 중 422 VEHICLE_MOVING(미구현, 확인 필요 사항 참조).
  - NEW: 요청의 name(필수)으로 user의 CreateGuardianUseCase를 불러 Family · GUARDIAN을 만들고 차량을 잇는다. 같은 대기 차량에 서로 다른 코드로 NEW 승인이 동시에 오면 하나만 성공하고 나머지는 409 VEHICLE_ALREADY_LINKED(vehicle의 @Version 낙관적 잠금 충돌을 409로 바꾼다).
  - RELINK: 그 차량 가족의 기존 GUARDIAN에게 새 토큰을 주고 이전 기기를 폐기한 뒤, 이전 기기에 「다른 기기에서 로그인했어요」 푸시를 보내도록 이벤트를 발행한다(계정 복구). 차 안에서 QR을 찍는 사람만 RELINK할 수 있다는 전제는 AUTH-02의 409가 있어야 성립한다.
  - 공통: 요청의 platform · pushToken으로 Device를 바로 만들고(AUTH-03 생략), 성공 즉시 코드를 USED(usedAt)로 바꾸고 mode를 채운다. 그 코드를 발급한 기기의 발급 카운터만 0으로 되돌린다. 발급 기기 정보가 없는 옛 코드면 초기화를 건너뛰고 승인은 그대로 성공한다.

### 초대
- FAM-04: 대상 멤버의 role이 GUARDIAN이면 요청자와 상관없이 403 FORBIDDEN(보호자 복구는 QR RELINK로만). 그 멤버에게 기기가 연결돼 있는데 replaceDevice = false면 409 DEVICE_ALREADY_CONNECTED. 24시간 · 1회용 토큰을 만들고, 다시 발급하면 이전 링크는 폐기한다. inviteUrl은 초대 링크 기본 주소 + token.
- FAM-05: 로그인 전 호출. 이미 수락 409 INVITE_ALREADY_USED, 만료 410 INVITE_EXPIRED. requiredConsents는 role별로 다르고 CHILD는 TERMS · PRIVACY · LOCATION · PUSH.
- FAM-06: requiredConsents를 모두 agreed = true로 보내야 한다(아니면 422 CONSENT_REQUIRED). Device 생성과 FCM 토큰 저장, 토큰 발급까지 한 번에 하고 usedAt을 채운다.

### 멤버 기기 상태 (user · schedule이 씀)

| 상태 | 판정(현재 가정) |
|---|---|
| CONNECTED | revokedAt이 빈 USER 기기가 있음 |
| INVITE_PENDING | 살아 있는 기기가 없고, 가장 최근 초대가 사용 전 · 만료 전 |
| INVITE_EXPIRED | 살아 있는 기기가 없고, 가장 최근 초대가 사용 전 · 만료됨 |
| NONE | 기기도 초대도 없음 |
| NONE | 살아 있는 기기가 없고, 가장 최근 초대가 사용됨(수락 뒤 기기가 폐기된 경우). 판정에는 USER 기기만 넘긴다 |

### 시그니처

```java
// in 포트 — web
TokenResult refresh(String refreshToken);                                   // RefreshTokenUseCase
VehicleDeviceResult registerVehicleDevice(RegisterVehicleDeviceCommand c);  // RegisterVehicleDeviceUseCase
void updatePushToken(UpdatePushTokenCommand c);                             // UpdatePushTokenUseCase
void logoutVehicle(long deviceId);                                          // LogoutVehicleUseCase
LinkCodeResult issueLinkCode(IssueLinkCodeCommand c);                       // IssueLinkCodeUseCase
LinkStatusResult getLinkStatus(long codeId, long requesterVehicleId, long requesterDeviceId);   // GetLinkStatusUseCase
LinkCodeInfo checkLinkCode(String code, String clientKey);                  // CheckLinkCodeUseCase
VehicleLinkResult approveLink(ApproveLinkCommand c);                        // ApproveLinkUseCase
InviteResult issueInvite(IssueInviteCommand c);                             // IssueInviteUseCase
InviteInfo getInvite(String token);                                         // GetInviteUseCase
AcceptInviteResult acceptInvite(String token, AcceptInviteCommand c);       // AcceptInviteUseCase

// in 포트 — 다른 모듈용
Map<Long, MemberDeviceStatus> getDeviceStatuses(List<Long> userIds);        // GetMemberDeviceStatusUseCase (user · schedule), MemberDeviceStatus는 auth.domain
void revokeDevicesOf(long userId);                                          // RevokeMemberDevicesUseCase (user FAM-07)
void revokeDevicesOfVehicle(long vehicleId);                                // RevokeVehicleDevicesUseCase (user LINK-05: 그 차량(ownerType VEHICLE)의 살아 있는 기기를 모두 폐기)
List<PushTarget> findPushTargets(List<Long> userIds);                       // LoadPushTargetUseCase (notification: 살아 있고 pushToken이 있는 기기)

// out 포트
Optional<Device> loadDevice(long deviceId); List<Device> loadLiveDevices(OwnerType type, long ownerId); Optional<Device> loadByRefreshTokenHash(String hash); Device saveDevice(Device d);   // LoadDevicePort · SaveDevicePort
Optional<LinkCode> loadLinkCode(long codeId); Optional<LinkCode> loadLinkCodeByCode(String code); List<LinkCode> loadIssuedCodes(long vehicleId); LinkCode saveLinkCode(LinkCode c);           // LoadLinkCodePort · SaveLinkCodePort
Optional<MemberInvite> loadInviteByToken(String token); Optional<MemberInvite> loadLatestInvite(long userId); MemberInvite saveInvite(MemberInvite i);                                        // LoadInvitePort · SaveInvitePort
IssuedTokens issueTokens(TokenSubject subject);                             // IssueTokenPort (JWT + refresh 원문 · 해시)
String hashRefreshToken(String refreshToken);                               // IssueTokenPort (refresh 원문 → SHA-256 해시, AUTH-01에서 loadByRefreshTokenHash 전에 씀)
// TokenSubject(type, id, familyId, role, deviceId) · IssuedTokens(accessToken, accessTokenExpiresAt, refreshToken, refreshTokenHash)
boolean hasActivePickup(long vehicleId);                                    // CheckActivePickupPort → pickup
void publishDeviceReplaced(List<Long> previousDeviceIds);                   // PublishAuthEventPort (RELINK 이전 기기 알림)
```

## 테스트 케이스
- [x] [기반] LinkCode는 발급 시각 + 5분을 expiresAt으로 갖고 status ISSUED로 시작한다
- [x] [기반] LinkCode는 expiresAt이 지나면 만료로 판정된다
- [x] [기반] MemberInvite는 발급 시각 + 24시간을 expiresAt으로 갖는다
- [x] [기반] MemberInvite는 usedAt이 채워지면 사용됨으로 판정된다
- [x] [기반] VIN 규칙은 17자 영대문자 · 숫자를 받고 I · O · Q가 들어 있으면 거절한다
- [x] [기반] CHILD의 필수 동의는 TERMS · PRIVACY · LOCATION · PUSH다
- [x] [기반] 보호자 연결의 필수 동의는 TERMS · PRIVACY이고 MARKETING은 선택이다
- [x] [기반] DeviceStatus 판정은 살아 있는 USER 기기가 있으면 CONNECTED를 돌려준다
- [x] [기반] DeviceStatus 판정은 기기가 없고 최근 초대가 사용 전 · 만료 전이면 INVITE_PENDING을 돌려준다
- [x] [기반] DeviceStatus 판정은 기기가 없고 최근 초대가 만료됐으면 INVITE_EXPIRED를 돌려준다
- [x] [기반] DeviceStatus 판정은 기기도 초대도 없으면 NONE을 돌려준다
- [x] [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] [기반] VehicleLinkCodeEntity · MemberInviteEntity · DeviceEntity가 BaseTimeEntity를 상속하고 테이블 정의대로 매핑된다
- [x] [기반] 테스트 시드로 연결 전 차량 기기 1개와 연결된 차량 기기 1개를 만든다
- [x] [persistence] Device를 refreshTokenHash로 찾는다
- [x] [persistence] loadLiveDevices는 revokedAt이 빈 기기만 돌려준다
- [x] [persistence] loadLinkCodeByCode는 8자 code로 연결 코드를 찾는다
- [x] [persistence] loadIssuedCodes는 그 차량의 ISSUED 코드만 돌려준다
- [x] [persistence] MemberInvite를 token으로 찾고 같은 token 두 개는 유니크 위반이 난다
- [x] [persistence] loadLatestInvite는 그 멤버의 가장 최근 초대를 돌려준다
- [x] [서비스] 토큰 발급은 refresh 원문을 돌려주고 Device에는 그 SHA-256 해시만 저장한다
- [x] [서비스] 발급한 access 토큰에는 주체 · familyId · role이 들어 있다
- [x] [서비스] 토큰 갱신은 새 refresh 토큰을 주고 이전 refresh 토큰으로 다시 갱신하면 REFRESH_TOKEN_INVALID로 거절한다
- [x] [서비스] 폐기된 기기의 refresh 토큰으로 갱신하면 REFRESH_TOKEN_INVALID로 거절한다
- [x] [서비스] 차량 기기 등록은 연결된 VIN이면 VEHICLE_ALREADY_LINKED로 거절한다
- [x] [서비스] 차량 기기 등록은 같은 VIN의 연결 대기 행이 있으면 새 Vehicle을 만들지 않고 그 vehicleId를 쓴다
- [x] [서비스] 차량 기기 등록은 ownerType VEHICLE · platform VEHICLE인 Device를 만든다
- [x] [서비스] 연결 전 기기 토큰으로는 LINK-01 · LINK-02 외의 API를 부를 수 없다
- [x] [서비스] 푸시 토큰 갱신은 지금 기기의 pushToken과 pushUpdatedAt을 바꾼다
- [x] [서비스] 차량 로그아웃은 진행 중 픽업이 있으면 ACTIVE_PICKUP_EXISTS로 거절한다
- [x] [서비스] 차량 로그아웃이 성공하면 그 기기의 revokedAt이 채워진다
- [x] [서비스] 연결 코드 발급은 같은 기기가 발급한 이전 ISSUED 코드를 EXPIRED로 바꾼다
- [x] [서비스] 연결 코드 발급은 첫 발급 때 reissuesLeft 4를 돌려준다
- [x] [서비스] 연결 코드를 연속 6번째로 발급하면 LINK_CODE_REISSUE_LIMIT으로 거절한다
- [x] [서비스] 연결 상태 조회는 다른 차량이 발급한 코드면 NOT_CODE_OWNER로 거절한다
- [x] [서비스] 연결 상태 조회는 승인 전 코드에 WAITING과 pollAfterSeconds 2를 돌려준다
- [x] [서비스] 연결 상태 조회는 만료된 코드에 EXPIRED를 돌려준다
- [x] [서비스] 연결 상태 조회는 NEW로 승인된 코드에 LINKED와 vehicleId · familyId · 차량 토큰을 돌려준다
- [x] [서비스] 연결 상태 조회는 RELINK로 승인된 코드에 RELINKED를 돌려준다
- [x] [서비스] 차량 토큰을 내려준 코드를 다시 조회하면 LINK_CODE_INVALID로 거절한다
- [x] [서비스] 연결 코드 확인은 가족에 연결된 차량의 코드면 RELINK, 아니면 NEW를 돌려준다
- [x] [서비스] 연결 코드 확인은 사용된 코드면 LINK_CODE_EXPIRED로 거절한다
- [x] [서비스] 연결 코드 확인이 같은 기기에서 5번 실패하면 1분 동안 LINK_CODE_ATTEMPT_LIMIT으로 거절한다
- [x] [서비스] 연결 승인은 mode가 코드의 실제 상태와 다르면 LINK_MODE_MISMATCH로 거절한다
- [x] [서비스] 연결 승인은 TERMS나 PRIVACY 동의가 없으면 CONSENT_REQUIRED로 거절한다
- [x] [서비스] 연결 승인 NEW는 CreateGuardianUseCase를 불러 가족과 GUARDIAN을 만든다
- [x] [서비스] 연결 승인 RELINK는 기존 GUARDIAN에게 토큰을 주고 그 GUARDIAN의 이전 기기를 폐기한다
- [x] [서비스] 연결 승인 RELINK는 이전 기기 알림 이벤트를 발행한다
- [x] [서비스] 연결 승인은 요청의 pushToken으로 Device를 만든다
- [x] [서비스] 연결 승인이 성공하면 코드가 USED가 되고 같은 코드로 다시 승인하면 LINK_CODE_EXPIRED로 거절한다
- [x] [서비스] 초대 발급은 기기가 연결된 멤버에 replaceDevice false면 DEVICE_ALREADY_CONNECTED로 거절한다
- [x] [서비스] 초대를 다시 발급하면 이전 초대 토큰으로는 확인 · 수락할 수 없다
- [x] [서비스] 초대 발급은 다른 가족의 멤버면 NOT_FOUND로 거절한다
- [x] [서비스] 초대 확인은 role CHILD 멤버에 requiredConsents TERMS · PRIVACY · LOCATION · PUSH를 돌려준다
- [x] [서비스] 초대 수락은 requiredConsents 중 하나라도 agreed가 아니면 CONSENT_REQUIRED로 거절한다
- [x] [서비스] 초대 수락은 Device를 만들고 pushToken을 저장하고 usedAt을 채운다
- [x] [서비스] 사용된 초대를 다시 수락하면 INVITE_ALREADY_USED로 거절한다
- [x] [서비스] 만료된 초대를 수락하면 INVITE_EXPIRED로 거절한다
- [x] [서비스] revokeDevicesOf는 그 멤버의 살아 있는 기기를 모두 폐기한다
- [x] [서비스] findPushTargets는 폐기됐거나 pushToken이 빈 기기를 빼고 돌려준다
- [x] [web] POST /v1/auth/refresh는 인증 헤더 없이 200과 accessToken · refreshToken · accessTokenExpiresAt을 돌려준다
- [x] [web] POST /v1/auth/refresh에 폐기된 토큰을 보내면 401 REFRESH_TOKEN_INVALID를 돌려준다
- [x] [web] POST /v1/vehicle-devices는 인증 헤더 없이 201과 deviceId · vehicleId · deviceToken을 돌려준다
- [x] [web] POST /v1/vehicle-devices에 16자 vin을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] [web] POST /v1/vehicle-devices가 연결된 VIN이면 409 VEHICLE_ALREADY_LINKED를 돌려준다
- [x] [web] PUT /v1/devices/me/push-token은 204를 돌려준다
- [x] [web] PUT /v1/devices/me/push-token에 256자 pushToken을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] [web] DELETE /v1/vehicle-devices/me/session은 204를 돌려준다
- [x] [web] DELETE /v1/vehicle-devices/me/session이 진행 중 픽업이 있으면 409 ACTIVE_PICKUP_EXISTS를 돌려준다
- [x] [web] POST /v1/vehicle-link-codes는 201과 codeId · code · qrPayload · expiresAt · reissuesLeft를 돌려준다
- [x] [web] POST /v1/vehicle-link-codes가 한도를 넘으면 429 LINK_CODE_REISSUE_LIMIT을 돌려준다
- [x] [web] GET /v1/vehicle-link-codes/{codeId}/status는 200과 status를 돌려준다
- [x] [web] GET /v1/vehicle-link-codes/{codeId}/status를 다른 차량 토큰으로 부르면 403 NOT_CODE_OWNER를 돌려준다
- [x] [web] GET /v1/vehicle-link-codes/{codeId}/status를 1초 안에 다시 부르면 429 TOO_MANY_REQUESTS를 돌려준다
- [x] [web] GET /v1/vehicle-link-codes/by-code/{code}는 인증 헤더 없이 200과 status · vehicleModel · plateNumberMasked · expiresAt을 돌려준다
- [x] [web] GET /v1/vehicle-link-codes/by-code/{code}가 없는 코드면 404 LINK_CODE_INVALID를 돌려준다
- [x] [web] GET /v1/vehicle-link-codes/by-code/{code}가 만료된 코드면 410 LINK_CODE_EXPIRED를 돌려준다
- [x] [web] POST /v1/vehicle-links는 인증 헤더 없이 201과 vehicleId · familyId · userId · accessToken · refreshToken을 돌려준다
- [x] [web] POST /v1/vehicle-links가 mode 불일치면 409 LINK_MODE_MISMATCH를 돌려준다
- [x] [web] POST /v1/vehicle-links가 필수 동의 누락이면 422 CONSENT_REQUIRED를 돌려준다
- [x] [web] POST /v1/families/me/members/{userId}/invites는 201과 inviteUrl · expiresAt을 돌려준다
- [x] [web] POST /v1/families/me/members/{userId}/invites가 기기 연결 멤버에 replaceDevice false면 409 DEVICE_ALREADY_CONNECTED를 돌려준다
- [x] [web] POST /v1/families/me/members/{userId}/invites를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] [web] GET /v1/invites/{token}은 인증 헤더 없이 200과 inviterName · memberName · role · requiredConsents · expiresAt을 돌려준다
- [x] [web] GET /v1/invites/{token}이 사용된 초대면 409 INVITE_ALREADY_USED를 돌려준다
- [x] [web] GET /v1/invites/{token}이 만료된 초대면 410 INVITE_EXPIRED를 돌려준다
- [x] [web] POST /v1/invites/{token}/acceptance는 인증 헤더 없이 201과 userId · familyId · role · accessToken · refreshToken을 돌려준다
- [x] [web] POST /v1/invites/{token}/acceptance가 필수 동의 누락이면 422 CONSENT_REQUIRED를 돌려준다
- [x] [web] Idempotency-Key는 POST /v1/vehicle-devices · POST /v1/vehicle-links · POST /v1/families/me/members/{userId}/invites · POST /v1/invites/{token}/acceptance에만 요구된다
- [x] [기반] LinkCode는 mode(NEW · RELINK)와 tokenDeliveredAt을 갖고, 승인하면 mode가 채워지고 USED가 되며 토큰을 내려주면 tokenDeliveredAt이 채워진다
- [x] [기반] 공통 IntegrationTestSupport는 도메인 시드를 Import하지 않고 시드가 필요한 테스트가 UserSeed · AuthSeed를 직접 Import한다
- [x] [서비스] 연결 승인 NEW는 요청의 name으로 GUARDIAN을 만든다
- [x] [web] POST /v1/vehicle-links에 mode NEW로 name 없이 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려준다
- [x] [기반] LinkCode · MemberInvite · Device는 restore로 저장된 값을 그대로 가진 객체를 만든다
- [x] [기반] LinkCode.expire는 status를 EXPIRED로, MemberInvite.markUsed는 usedAt을, MemberInvite.revoke는 expiresAt을 지금으로 바꾼다
- [x] [기반] Device.revoke는 revokedAt을, Device.updatePushToken은 pushToken · pushUpdatedAt을, Device.rotateRefreshToken은 refreshTokenHash를 바꾼다
- [x] [기반] CheckActivePickupPort 어댑터는 pickup ActivePickupQueryUseCase.hasActivePickup 결과를 그대로 돌려주고, PublishAuthEventPort는 이전 기기 id를 담은 스프링 이벤트를 발행한다(사람 결정 2026-11-13)
- [x] [통합] user의 AuthMemberDeviceAdapter는 auth GetMemberDeviceStatusUseCase 결과를 user DeviceStatus로 바꿔 돌려준다
- [x] [통합] user의 RevokeMemberDevicesPort 구현은 auth RevokeMemberDevicesUseCase를 부른다
- [x] [서비스] user의 LoadVehicleUseCase는 findVehicle · findLinkedVehicleByVin으로 VehicleInfo를 돌려준다
- [x] [서비스] user의 LoadMemberUseCase는 findMember · findMembers로 MemberInfo를 돌려주고 LoadFamilyUseCase는 findFamily로 FamilyInfo를 돌려준다
- [x] [통합] 이름 규칙(NameRule)은 common에 한 번만 두고 user · auth web 검증이 그것을 쓴다
- [x] [통합] 번호판 마스킹(PlateNumberMasker)은 common에 한 번만 두고 user 가족 조회 · auth 연결 코드 확인이 그것을 쓴다
- [x] [서비스] 연결 코드 발급은 연결 전 토큰(familyId 없음)인데 그 차량이 이미 가족에 연결돼 있으면 VEHICLE_ALREADY_LINKED로 거절한다
- [x] [서비스] 초대 발급은 대상 멤버의 role이 GUARDIAN이면 FORBIDDEN으로 거절한다
- [x] [서비스] 연결 코드는 발급한 deviceId를 기록하고, 연결 상태 조회는 발급한 기기가 아니면 NOT_CODE_OWNER로 거절하며 차량 토큰은 그 기기에만 준다
- [x] [서비스] 초대 수락은 그 멤버의 살아 있는 이전 기기를 모두 폐기한다
- [x] [서비스] 연결 전 토큰으로 발급한 코드는 승인 때 mode가 RELINK면 LINK_MODE_MISMATCH로 거절한다
- [x] [서비스] 연결 코드 발급 횟수는 기기(deviceId) 단위로 센다
- [x] [서비스] 연결 코드 발급은 같은 기기가 발급한 이전 ISSUED 코드만 EXPIRED로 바꾼다
- [x] [web] GET /v1/vehicle-link-codes/{codeId}/status의 1초 호출 제한은 기기(deviceId) 단위로 센다
- [x] LINK-01: 발급 기기 정보가 없는 옛 ISSUED 코드(issuedDeviceId null)는 같은 차량에서 새 코드를 발급할 때 함께 EXPIRED가 된다
- [x] LINK-04: 발급 기기 정보가 없는 옛 코드로 승인하면 발급 카운터 초기화를 건너뛰고 승인은 성공한다
- [x] LINK-04: 같은 연결 대기 차량에 서로 다른 코드로 NEW 승인이 동시에 들어오면 하나만 성공하고 나머지는 409 VEHICLE_ALREADY_LINKED로 실패한다(vehicle에 @Version 낙관적 잠금, 충돌은 409로 바꾼다)
- [x] LINK-01: 연결이 해제된 차량 행(unlinkedAt 있음)의 기기가 코드를 발급하면 409 VEHICLE_UNLINKED로 거절한다(앱을 다시 등록해야 함)
- [x] [서비스] LINK-05로 차량 연결을 해제하면 그 차량의 살아 있는 기기를 모두 폐기한다(해제된 차량 기기는 refresh로 이전 가족의 토큰을 받을 수 없고 AUTH-02로 다시 등록해야 함)
- [x] [persistence] vehicle.version 컬럼은 NOT NULL에 기본값 0을 가진다(행이 있는 DB에도 컬럼을 추가할 수 있음)
- [x] [web] POST /v1/vehicle-link-codes를 연결 전 토큰(familyId 없음)으로 부르는데 차량이 이미 가족에 연결돼 있으면 409 VEHICLE_ALREADY_LINKED를 돌려준다
- [x] [web] POST /v1/families/me/members/{userId}/invites의 대상 멤버가 GUARDIAN이면 403 FORBIDDEN을 돌려준다
- [ ] [web] 초대 수락(FAM-06)과 연결 승인(LINK-04)의 consents에 SERVICE_IMPROVEMENT(선택 동의)를 보낼 수 있고, 빠져도 수락 · 승인은 성공한다(C00 · 가족 프로필 등록4 「(선택) 서비스 개선」)
- [ ] [서비스] refresh 토큰은 발급 14일(P14D) 뒤 만료되고, 만료된 refresh 토큰으로 POST /v1/auth/refresh를 부르면 유효하지 않은 refresh 토큰과 같은 401로 거절한다(사람 결정 2026-11-13)
- [ ] [통합] access 토큰 유효 기간 PT30M · refresh 토큰 유효 기간 P14D는 application.yml 설정값으로 두고, 발급한 access 토큰의 exp가 발급 시각 + 30분이다

## 확인 필요 사항

| 항목 | 현재 가정 |
|---|---|
| access · refresh 토큰 유효 기간 | 소스에 없음. 설정값으로 두고 값은 정해야 함 |
| JWT 서명 방식 | 소스에는 「JWT 키는 환경 변수」만 있음. 대칭키(HS256)로 가정 |
| refresh 토큰 형식 | 예시가 「eyJ…」와 「rt_…」로 섞임. 불투명한 임의 문자열로 가정 |
| access 토큰에 deviceId를 넣는지 | AUTH-03(devices/me) · AUTH-04(vehicle-devices/me)는 「지금 기기」를 알아야 하는데 4.1의 토큰 내용에는 deviceId가 없음. deviceId 클레임 추가로 가정 |
| 연결 전 기기 토큰 구분 | 소스는 「LINK-01 · 02만 호출 가능」만 정함. familyId가 빈 VEHICLE 토큰을 연결 전 토큰으로 본다고 가정 |
| LINK-02의 NEW · RELINK 구분과 「토큰을 이미 내려줌」 기록 | 결정됨(2026-11-07). VehicleLinkCode에 mode(NEW · RELINK, LINK-04 승인 때 채움) · tokenDeliveredAt(LINK-02가 토큰을 내려준 시각) 컬럼을 더함. 승인 때 USED, 토큰을 내려준 뒤 다시 조회하면 404 LINK_CODE_INVALID |
| LINK-02 「발급한 기기」 판단 | 결정됨(2026-11-07). VehicleLinkCode에 issuedDeviceId를 기록하고 요청 기기의 deviceId와 비교함. 다르면 403 NOT_CODE_OWNER, 차량 토큰은 발급한 기기에만 줌. 1초 호출 제한도 기기 단위 |
| LINK-01 「연속 5회」 초기화 시점 | 결정됨(2026-11-07). 횟수는 기기(deviceId) 단위로 세고, 연결 승인이 성공하면 그 코드를 발급한 기기의 카운터만 0으로 돌아감. 발급 기기 정보가 없는 옛 코드면 초기화를 건너뜀 |
| 연결 코드 문자 구성 | 예시 「K7P2QX9M」뿐. 영대문자 · 숫자로 가정 |
| LINK-03 「기기당 5회 실패」의 기기 식별 · 저장 위치 | 인증 없는 호출이라 기기를 알 수 없고 저장 테이블도 없음. 요청 IP 기준 메모리 카운터로 가정 |
| LINK-02 「1초보다 자주」 판단 저장 위치 | 소스에 없음. 메모리로 가정(서버 1대 전제) |
| LINK-04 VEHICLE_MOVING 판단 근거 | 세션 밖에서는 차량 주행 상태를 받는 API가 없음. 판단 근거가 없어 미구현(API 문서에서도 뺌). 판단 방법을 정하면 설계서 · 구현 · API 문서를 함께 맞춤 |
| 동의(consents) 저장 위치 | 결정됨: 지금은 저장하지 않는다. FAM-06 CHILD 수락에서 LOCATION 동의가 필수라 연결된 자녀는 동의한 상태로 본다. 그래서 SIG-04의 403 LOCATION_CONSENT_REQUIRED는 위치 동의 철회 기능이 생기기 전까지 실제로 나지 않는다. 철회 기능을 만들 때 저장 위치를 정한다(사람 결정 2026-11-13) |
| LINK-04 NEW로 만드는 GUARDIAN의 name · phone | name은 결정됨(사람 결정 2026-11-07). LINK-04 요청 name(1~20자, 이모지 불가), mode NEW일 때 필수 · RELINK면 무시. phone은 결정됨: 받지 않는다. GUARDIAN phone은 null을 허용하고 user의 「자녀 외 phone 필수」 규칙의 예외로 둔다(사람 결정 2026-11-13) |
| 차량 번호판(plateNumber) 입력 경로 | 결정됨: 서버가 번호판을 받지 않는다. 차대번호(VIN) 조회로 나중에 불러올 수 있어 입력 경로를 두지 않는다(사람 결정 2026-11-13) |
| FAM-05 inviterName | MemberInvite에 초대한 사람 컬럼이 없음. 가족의 GUARDIAN 이름으로 가정 |
| 자녀 외 role의 필수 동의 | 소스는 CHILD만 정함. TERMS · PRIVACY로 가정 |
| 재발급 때 이전 초대 「폐기」 방법 | MemberInvite에 상태 컬럼이 없음. 이전 초대의 expiresAt을 지금으로 당긴다고 가정 |
| replaceDevice = true 수락 때 기존 기기 | MemberInvite에 replaceDevice 컬럼이 없음. 수락 때 그 멤버의 이전 기기를 폐기한다고 가정 |
| 초대 링크 기본 주소 | 예시 https://app.example/invite/… 뿐. 설정값으로 둠 |
| AUTH-03을 차량 앱이 부르는지 | 4.2는 「차량은 푸시 토큰을 등록하지 않음」, 4.14는 호출에 차량 앱 · platform VEHICLE이 있음. 4.14대로 받되 차량 앱은 실제로 부르지 않는다고 가정 |
| AUTH-04 뒤 이미 발급된 access 토큰 | 소스에 없음. 만료될 때까지 유효하고 refresh만 막힌다고 가정 |
| RELINK 이전 기기 푸시의 Notification 행 | Notification.sessionId가 세션 FK라 세션 밖 알림을 담기 어려움. 행 없이 FCM만 보낸다고 가정(09_알림-notification.md에도 기록) |
| 기기 하나에 refresh 토큰 하나 | Device.refreshTokenHash가 한 칸이라 그렇게 가정 |
| JWT 클레임 이름(03 검증기와 맞춤) | common이 sub(주체 id 문자열) · principalType(VEHICLE · USER) · familyId(숫자, 없으면 null) · role(USER만) · iat · exp, HS256, pickup.jwt.secret(환경 변수 JWT_SECRET)으로 검증함. 발급기도 같은 이름 · 키를 쓴다고 가정. exp는 필수라 발급기가 반드시 넣음(exp 없는 토큰은 03이 401, 사람 결정 2026-11-07). 주체 종류별 필수 클레임(role 없는 USER 토큰 등)은 정해야 함 |
| 인증 없는 API 등록 | 03 방식대로 auth가 PublicEndpoints 빈(PublicEndpoint(HttpMethod, pattern))으로 AUTH-02 · LINK-03 · LINK-04 · FAM-05 · FAM-06을 등록하고 common SecurityConfig가 permitAll함. 그 경로는 Bearer 토큰을 읽지 않음(만료 토큰이 붙어도 401 아님). AUTH-01도 access 토큰 없이 refresh 토큰으로 부르므로 같이 등록한다고 가정 |
| LINK-02 요청 로그 | 03 요청 로깅은 @PollingLog로 폴링을 구분함. LINK-02 컨트롤러 메서드에 @PollingLog(stateField = "status")를 단다고 가정 |
| 인증 없는 Idempotency-Key API(AUTH-02 · LINK-04 · FAM-06) | 주체가 null이라 모든 호출자가 같은 키 공간을 씀(03). 다른 기기가 같은 키 · 같은 본문을 보내면 첫 응답(토큰 포함)이 그대로 감. 재응답 대신 409로 할지 정해야 함 |
| VIN만 아는 사람의 NEW 선점 | VIN만 알면 AUTH-02 → LINK-01 → LINK-04(NEW)로 연결 안 된 차량을 먼저 차지할 수 있음. 진짜 주인은 그 뒤 AUTH-02에서 409 VEHICLE_ALREADY_LINKED를 받음. 차량 소유 확인 방법 미정, 후속 과제(사람 결정 2026-11-07) |
