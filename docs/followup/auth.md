# FOLLOWUP · 인증 (auth)

- 생성: 2026-11-07 20:02 KST · tdd-guard
- 상태: 진행 중 (체크 21/100 · 완료 기준 0/4)
- 테스트: ● GREEN · 483 passed · 전체 · 36s · cd C:/pickup && ./gradlew.bat test --console=plain > "C:/Users/keuye/AppData/Local/Temp/claude/86b70dd9bf0b8321/ed26ec4…
- 설계서: C:/pickup/docs/05_인증-auth.md

## 완료 항목
- [x] #1 [기반] LinkCode는 발급 시각 + 5분을 expiresAt으로 갖고 status ISSUED로 시작한다 ← LinkCodeTest.LinkCode는 발급 시각 + 5분을 expiresAt으로 갖고 status ISSUED로 시작한다
- [x] #2 [기반] LinkCode는 expiresAt이 지나면 만료로 판정된다 ← LinkCodeTest.LinkCode는 expiresAt이 지나면 만료로 판정된다
- [x] #3 [기반] MemberInvite는 발급 시각 + 24시간을 expiresAt으로 갖는다 ← MemberInviteTest.MemberInvite는 발급 시각 + 24시간을 expiresAt으로 갖는다
- [x] #4 [기반] MemberInvite는 usedAt이 채워지면 사용됨으로 판정된다 ← MemberInviteTest.MemberInvite는 usedAt이 채워지면 사용됨으로 판정된다
- [x] #5 [기반] VIN 규칙은 17자 영대문자 · 숫자를 받고 I · O · Q가 들어 있으면 거절한다 ← VinRuleTest.VIN 규칙은 17자 영대문자 · 숫자를 받고 I · O · Q가 들어 있으면 거절한다
- [x] #6 [기반] CHILD의 필수 동의는 TERMS · PRIVACY · LOCATION · PUSH다 ← ConsentsTest.CHILD의 필수 동의는 TERMS · PRIVACY · LOCATION · PUSH다
- [x] #7 [기반] 보호자 연결의 필수 동의는 TERMS · PRIVACY이고 MARKETING은 선택이다 ← ConsentsTest.보호자 연결의 필수 동의는 TERMS · PRIVACY이고 MARKETING은 선택이다
- [x] #8 [기반] DeviceStatus 판정은 살아 있는 USER 기기가 있으면 CONNECTED를 돌려준다 ← DeviceStatusJudgeTest.DeviceStatus 판정은 살아 있는 USER 기기가 있으면 CONNECTED를 돌려준다
- [x] #9 [기반] DeviceStatus 판정은 기기가 없고 최근 초대가 사용 전 · 만료 전이면 INVITE_PENDING을 돌려준다 ← DeviceStatusJudgeTest.DeviceStatus 판정은 기기가 없고 최근 초대가 사용 전 · 만료 전이면 INVITE_PENDING을 돌려준다
- [x] #10 [기반] DeviceStatus 판정은 기기가 없고 최근 초대가 만료됐으면 INVITE_EXPIRED를 돌려준다 ← DeviceStatusJudgeTest.DeviceStatus 판정은 기기가 없고 최근 초대가 만료됐으면 INVITE_EXPIRED를 돌려준다
- [x] #11 [기반] DeviceStatus 판정은 기기도 초대도 없으면 NONE을 돌려준다 ← DeviceStatusJudgeTest.DeviceStatus 판정은 기기도 초대도 없으면 NONE을 돌려준다
- [x] #12 [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다 ← AuthInPortsTest.핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] #13 [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다 ← AuthOutPortsTest.핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] #14 [기반] VehicleLinkCodeEntity · MemberInviteEntity · DeviceEntity가 BaseTimeEntity를 상속하고 테이블 정의대로 매핑된다 ← AuthPersistenceEntitiesTest.VehicleLinkCodeEntity · MemberInviteEntity · DeviceEntity가 BaseTimeEntity를 상속하고 테이블 정의대로 매핑된다
- [x] #15 [기반] 테스트 시드로 연결 전 차량 기기 1개와 연결된 차량 기기 1개를 만든다 ← AuthSeedTest.테스트 시드로 연결 전 차량 기기 1개와 연결된 차량 기기 1개를 만든다
- [x] #91 [기반] LinkCode는 mode(NEW · RELINK)와 tokenDeliveredAt을 갖고, 승인하면 mode가 채워지고 USED가 되며 토큰을 내려주면 tokenDeliveredAt이 채워진다 ← LinkCodeTest.LinkCode는 mode(NEW · RELINK)와 tokenDeliveredAt을 갖고, 승인하면 mode가 채워지고 USED가 되며 토큰을 내려주면 tokenDeliveredAt이 채워진다
- [x] #92 [기반] 공통 IntegrationTestSupport는 도메인 시드를 Import하지 않고 시드가 필요한 테스트가 UserSeed · AuthSeed를 직접 Import한다 ← IntegrationTestSupportTest.공통 IntegrationTestSupport는 도메인 시드를 Import하지 않고 시드가 필요한 테스트가 UserSeed · AuthSeed를 직접 Import한다
- [x] #95 [기반] LinkCode · MemberInvite · Device는 restore로 저장된 값을 그대로 가진 객체를 만든다 ← DeviceTest.LinkCode · MemberInvite · Device는 restore로 저장된 값을 그대로 가진 객체를 만든다
- [x] #96 [기반] LinkCode.expire는 status를 EXPIRED로, MemberInvite.markUsed는 usedAt을, MemberInvite.revoke는 expiresAt을 지금으로 바꾼다 ← MemberInviteTest.LinkCode.expire는 status를 EXPIRED로, MemberInvite.markUsed는 usedAt을, MemberInvite.revoke는 expiresAt을 지금으로 바꾼다
- [x] #97 [기반] Device.revoke는 revokedAt을, Device.updatePushToken은 pushToken · pushUpdatedAt을, Device.rotateRefreshToken은 refreshTokenHash를 바꾼다 ← DeviceTest.Device.revoke는 revokedAt을, Device.updatePushToken은 pushToken · pushUpdatedAt을, Device.rotateRefreshToken은 refreshTokenHash를 바꾼다
- [x] #98 [기반] pickup 구현 전 임시 CheckActivePickupPort는 false를 돌려주고, PublishAuthEventPort는 이전 기기 id를 담은 스프링 이벤트를 발행한다 ← AuthModuleLinkAdaptersTest.pickup 구현 전 임시 CheckActivePickupPort는 false를 돌려주고, PublishAuthEventPort는 이전 기기 id를 담은 스프링 이벤트를 발행한다

## 미완료 항목
- [ ] #16 [persistence] Device를 refreshTokenHash로 찾는다
- [ ] #17 [persistence] loadLiveDevices는 revokedAt이 빈 기기만 돌려준다
- [ ] #18 [persistence] loadLinkCodeByCode는 8자 code로 연결 코드를 찾는다
- [ ] #19 [persistence] loadIssuedCodes는 그 차량의 ISSUED 코드만 돌려준다
- [ ] #20 [persistence] MemberInvite를 token으로 찾고 같은 token 두 개는 유니크 위반이 난다
- [ ] #21 [persistence] loadLatestInvite는 그 멤버의 가장 최근 초대를 돌려준다
- [ ] #22 [서비스] 토큰 발급은 refresh 원문을 돌려주고 Device에는 그 SHA-256 해시만 저장한다
- [ ] #23 [서비스] 발급한 access 토큰에는 주체 · familyId · role이 들어 있다
- [ ] #24 [서비스] 토큰 갱신은 새 refresh 토큰을 주고 이전 refresh 토큰으로 다시 갱신하면 REFRESH_TOKEN_INVALID로 거절한다
- [ ] #25 [서비스] 폐기된 기기의 refresh 토큰으로 갱신하면 REFRESH_TOKEN_INVALID로 거절한다
- [ ] #26 [서비스] 차량 기기 등록은 연결된 VIN이면 VEHICLE_ALREADY_LINKED로 거절한다
- [ ] #27 [서비스] 차량 기기 등록은 같은 VIN의 연결 대기 행이 있으면 새 Vehicle을 만들지 않고 그 vehicleId를 쓴다
- [ ] #28 [서비스] 차량 기기 등록은 ownerType VEHICLE · platform VEHICLE인 Device를 만든다
- [ ] #29 [서비스] 연결 전 기기 토큰으로는 LINK-01 · LINK-02 외의 API를 부를 수 없다
- [ ] #30 [서비스] 푸시 토큰 갱신은 지금 기기의 pushToken과 pushUpdatedAt을 바꾼다
- [ ] #31 [서비스] 차량 로그아웃은 진행 중 픽업이 있으면 ACTIVE_PICKUP_EXISTS로 거절한다
- [ ] #32 [서비스] 차량 로그아웃이 성공하면 그 기기의 revokedAt이 채워진다
- [ ] #33 [서비스] 연결 코드 발급은 그 차량의 이전 ISSUED 코드를 EXPIRED로 바꾼다
- [ ] #34 [서비스] 연결 코드 발급은 첫 발급 때 reissuesLeft 4를 돌려준다
- [ ] #35 [서비스] 연결 코드를 연속 6번째로 발급하면 LINK_CODE_REISSUE_LIMIT으로 거절한다
- [ ] #36 [서비스] 연결 상태 조회는 다른 차량이 발급한 코드면 NOT_CODE_OWNER로 거절한다
- [ ] #37 [서비스] 연결 상태 조회는 승인 전 코드에 WAITING과 pollAfterSeconds 2를 돌려준다
- [ ] #38 [서비스] 연결 상태 조회는 만료된 코드에 EXPIRED를 돌려준다
- [ ] #39 [서비스] 연결 상태 조회는 NEW로 승인된 코드에 LINKED와 vehicleId · familyId · 차량 토큰을 돌려준다
- [ ] #40 [서비스] 연결 상태 조회는 RELINK로 승인된 코드에 RELINKED를 돌려준다
- [ ] #41 [서비스] 차량 토큰을 내려준 코드를 다시 조회하면 LINK_CODE_INVALID로 거절한다
- [ ] #42 [서비스] 연결 코드 확인은 가족에 연결된 차량의 코드면 RELINK, 아니면 NEW를 돌려준다
- [ ] #43 [서비스] 연결 코드 확인은 사용된 코드면 LINK_CODE_EXPIRED로 거절한다
- [ ] #44 [서비스] 연결 코드 확인이 같은 기기에서 5번 실패하면 1분 동안 LINK_CODE_ATTEMPT_LIMIT으로 거절한다
- [ ] #45 [서비스] 연결 승인은 mode가 코드의 실제 상태와 다르면 LINK_MODE_MISMATCH로 거절한다
- [ ] #46 [서비스] 연결 승인은 TERMS나 PRIVACY 동의가 없으면 CONSENT_REQUIRED로 거절한다
- [ ] #47 [서비스] 연결 승인 NEW는 CreateGuardianUseCase를 불러 가족과 GUARDIAN을 만든다
- [ ] #48 [서비스] 연결 승인 RELINK는 기존 GUARDIAN에게 토큰을 주고 그 GUARDIAN의 이전 기기를 폐기한다
- [ ] #49 [서비스] 연결 승인 RELINK는 이전 기기 알림 이벤트를 발행한다
- [ ] #50 [서비스] 연결 승인은 요청의 pushToken으로 Device를 만든다
- [ ] #51 [서비스] 연결 승인이 성공하면 코드가 USED가 되고 같은 코드로 다시 승인하면 LINK_CODE_EXPIRED로 거절한다
- [ ] #52 [서비스] 초대 발급은 기기가 연결된 멤버에 replaceDevice false면 DEVICE_ALREADY_CONNECTED로 거절한다
- [ ] #53 [서비스] 초대를 다시 발급하면 이전 초대 토큰으로는 확인 · 수락할 수 없다
- [ ] #54 [서비스] 초대 발급은 다른 가족의 멤버면 NOT_FOUND로 거절한다
- [ ] #55 [서비스] 초대 확인은 role CHILD 멤버에 requiredConsents TERMS · PRIVACY · LOCATION · PUSH를 돌려준다
- [ ] #56 [서비스] 초대 수락은 requiredConsents 중 하나라도 agreed가 아니면 CONSENT_REQUIRED로 거절한다
- [ ] #57 [서비스] 초대 수락은 Device를 만들고 pushToken을 저장하고 usedAt을 채운다
- [ ] #58 [서비스] 사용된 초대를 다시 수락하면 INVITE_ALREADY_USED로 거절한다
- [ ] #59 [서비스] 만료된 초대를 수락하면 INVITE_EXPIRED로 거절한다
- [ ] #60 [서비스] revokeDevicesOf는 그 멤버의 살아 있는 기기를 모두 폐기한다
- [ ] #61 [서비스] findPushTargets는 폐기됐거나 pushToken이 빈 기기를 빼고 돌려준다
- [ ] #62 [web] POST /v1/auth/refresh는 인증 헤더 없이 200과 accessToken · refreshToken · accessTokenExpiresAt을 돌려준다
- [ ] #63 [web] POST /v1/auth/refresh에 폐기된 토큰을 보내면 401 REFRESH_TOKEN_INVALID를 돌려준다
- [ ] #64 [web] POST /v1/vehicle-devices는 인증 헤더 없이 201과 deviceId · vehicleId · deviceToken을 돌려준다
- [ ] #65 [web] POST /v1/vehicle-devices에 16자 vin을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #66 [web] POST /v1/vehicle-devices가 연결된 VIN이면 409 VEHICLE_ALREADY_LINKED를 돌려준다
- [ ] #67 [web] PUT /v1/devices/me/push-token은 204를 돌려준다
- [ ] #68 [web] PUT /v1/devices/me/push-token에 256자 pushToken을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #69 [web] DELETE /v1/vehicle-devices/me/session은 204를 돌려준다
- [ ] #70 [web] DELETE /v1/vehicle-devices/me/session이 진행 중 픽업이 있으면 409 ACTIVE_PICKUP_EXISTS를 돌려준다
- [ ] #71 [web] POST /v1/vehicle-link-codes는 201과 codeId · code · qrPayload · expiresAt · reissuesLeft를 돌려준다
- [ ] #72 [web] POST /v1/vehicle-link-codes가 한도를 넘으면 429 LINK_CODE_REISSUE_LIMIT을 돌려준다
- [ ] #73 [web] GET /v1/vehicle-link-codes/{codeId}/status는 200과 status를 돌려준다
- [ ] #74 [web] GET /v1/vehicle-link-codes/{codeId}/status를 다른 차량 토큰으로 부르면 403 NOT_CODE_OWNER를 돌려준다
- [ ] #75 [web] GET /v1/vehicle-link-codes/{codeId}/status를 1초 안에 다시 부르면 429 TOO_MANY_REQUESTS를 돌려준다
- [ ] #76 [web] GET /v1/vehicle-link-codes/by-code/{code}는 인증 헤더 없이 200과 status · vehicleModel · plateNumberMasked · expiresAt을 돌려준다
- [ ] #77 [web] GET /v1/vehicle-link-codes/by-code/{code}가 없는 코드면 404 LINK_CODE_INVALID를 돌려준다
- [ ] #78 [web] GET /v1/vehicle-link-codes/by-code/{code}가 만료된 코드면 410 LINK_CODE_EXPIRED를 돌려준다
- [ ] #79 [web] POST /v1/vehicle-links는 인증 헤더 없이 201과 vehicleId · familyId · userId · accessToken · refreshToken을 돌려준다
- [ ] #80 [web] POST /v1/vehicle-links가 mode 불일치면 409 LINK_MODE_MISMATCH를 돌려준다
- [ ] #81 [web] POST /v1/vehicle-links가 필수 동의 누락이면 422 CONSENT_REQUIRED를 돌려준다
- [ ] #82 [web] POST /v1/families/me/members/{userId}/invites는 201과 inviteUrl · expiresAt을 돌려준다
- [ ] #83 [web] POST /v1/families/me/members/{userId}/invites가 기기 연결 멤버에 replaceDevice false면 409 DEVICE_ALREADY_CONNECTED를 돌려준다
- [ ] #84 [web] POST /v1/families/me/members/{userId}/invites를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #85 [web] GET /v1/invites/{token}은 인증 헤더 없이 200과 inviterName · memberName · role · requiredConsents · expiresAt을 돌려준다
- [ ] #86 [web] GET /v1/invites/{token}이 사용된 초대면 409 INVITE_ALREADY_USED를 돌려준다
- [ ] #87 [web] GET /v1/invites/{token}이 만료된 초대면 410 INVITE_EXPIRED를 돌려준다
- [ ] #88 [web] POST /v1/invites/{token}/acceptance는 인증 헤더 없이 201과 userId · familyId · role · accessToken · refreshToken을 돌려준다
- [ ] #89 [web] POST /v1/invites/{token}/acceptance가 필수 동의 누락이면 422 CONSENT_REQUIRED를 돌려준다
- [ ] #90 [web] Idempotency-Key는 POST /v1/vehicle-devices · POST /v1/vehicle-links · POST /v1/families/me/members/{userId}/invites · POST /v1/invites/{token}/acceptance에만 요구된다
- [ ] #93 [서비스] 연결 승인 NEW는 요청의 name으로 GUARDIAN을 만든다
- [ ] #94 [web] POST /v1/vehicle-links에 mode NEW로 name 없이 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려준다
- [ ] #99 [통합] user의 AuthMemberDeviceAdapter는 auth GetMemberDeviceStatusUseCase 결과를 user DeviceStatus로 바꿔 돌려준다
- [ ] #100 [통합] user의 RevokeMemberDevicesPort 구현은 auth RevokeMemberDevicesUseCase를 부른다

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
| access · refresh 토큰 유효 기간 | 소스에 없음. 설정값으로 두고 값은 정해야 함 |
| JWT 서명 방식 | 소스에는 「JWT 키는 환경 변수」만 있음. 대칭키(HS256)로 가정 |
| refresh 토큰 형식 | 예시가 「eyJ…」와 「rt_…」로 섞임. 불투명한 임의 문자열로 가정 |
| access 토큰에 deviceId를 넣는지 | AUTH-03(devices/me) · AUTH-04(vehicle-devices/me)는 「지금 기기」를 알아야 하는데 4.1의 토큰 내용에는 deviceId가 없음. deviceId 클레임 추가로 가정 |
| 연결 전 기기 토큰 구분 | 소스는 「LINK-01 · 02만 호출 가능」만 정함. familyId가 빈 VEHICLE 토큰을 연결 전 토큰으로 본다고 가정 |
| LINK-02의 NEW · RELINK 구분과 「토큰을 이미 내려줌」 기록 | VehicleLinkCode에 해당 컬럼이 없음(status는 ISSUED · USED · EXPIRED뿐). 컬럼을 더할지 정해야 함 |
| LINK-02 「발급한 기기」 판단 | 코드에는 vehicleId만 있고 deviceId가 없음. 토큰의 vehicleId와 코드의 vehicleId를 비교한다고 가정 |
| LINK-01 「연속 5회」 초기화 시점 | 소스에 없음. 연결 승인이 성공하면 0으로 돌아간다고 가정 |
| 연결 코드 문자 구성 | 예시 「K7P2QX9M」뿐. 영대문자 · 숫자로 가정 |
| LINK-03 「기기당 5회 실패」의 기기 식별 · 저장 위치 | 인증 없는 호출이라 기기를 알 수 없고 저장 테이블도 없음. 요청 IP 기준 메모리 카운터로 가정 |
| LINK-02 「1초보다 자주」 판단 저장 위치 | 소스에 없음. 메모리로 가정(서버 1대 전제) |
| LINK-04 VEHICLE_MOVING 판단 근거 | 세션 밖에서는 차량 주행 상태를 받는 API가 없음. 판단 방법을 정해야 함 |
| 동의(consents) 저장 위치 | 저장 테이블이 없음. SIG-04의 LOCATION_CONSENT_REQUIRED가 위치 동의를 읽어야 함. 저장 위치를 정해야 함 |
| LINK-04 NEW로 만드는 GUARDIAN의 name · phone | 요청에 이름 · 전화번호가 없는데 User.name은 1~20자 필수. 받을 곳을 정해야 함 |
| 차량 번호판(plateNumber) 입력 경로 | AUTH-02는 vin · model만 받음. LINK-03이 번호판을 보여 주려면 입력 경로가 필요함 |
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

## 다음 단계
- 남은 항목 79건부터 계속: /tdd domain auth → /tdd go
- 다음 도메인: 일정 (06_일정-schedule.md). 시작: /tdd domain schedule

## 후속 과제
- [사람 결정 2026-11-07] (1) VehicleLinkCode에 mode(NEW · RELINK, LINK-04 승인 때 채움) · tokenDeliveredAt(LINK-02가 토큰을 내려준 시각) 컬럼 추가 — 승인 시 USED, 토큰 내려준 뒤 다시 조회하면 404 LINK_CODE_INVALID. (2) LINK-04 요청에 name(1~20자, 이모지 불가) 추가, mode NEW일 때 필수 · RELINK면 무시. (3) 도메인 시드는 공통 IntegrationTestSupport에서 빼고 필요한 테스트가 직접 @Import(user C3 해소용). (4) 「연결 중」 차이: user loadLinkedVehicleByVin 결과의 familyId로 auth가 구분(familyId 있음 → 409 VEHICLE_ALREADY_LINKED, 없음 → 대기 행 재사용). 설계서 테이블 · API 명세 갱신은 도메인 끝날 때 반영.
- [설계 확인 · auth] 멤버 기기 상태: 사용된 초대만 있고 살아 있는 기기가 없을 때(수락 뒤 기기 폐기)는 설계서 표에 없음 → NONE으로 가정(DeviceStatusJudgeTest). 설계서 표에 행 추가 필요. 판정은 USER 기기만 넘긴다는 전제.
- [문서] 05 설계서 「시그니처」 갱신 필요: IssueTokenPort.hashRefreshToken(String) 추가, TokenSubject(type, id, familyId, role, deviceId) · IssuedTokens(accessToken, accessTokenExpiresAt, refreshToken, refreshTokenHash) 컴포넌트, 기기 상태 반환 타입 MemberDeviceStatus(auth.domain). [주의] out 포트 11개는 Bash로 만들어 tdd-guard 기반 파일로 등록되지 않았을 수 있음 — lane에서 포트를 고치지 않도록 프롬프트로 막을 것.
