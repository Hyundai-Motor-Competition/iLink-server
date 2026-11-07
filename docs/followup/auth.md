# FOLLOWUP · 인증 (auth)

- 생성: 2026-11-07 23:03 KST · tdd-guard
- 상태: 완료 (체크 120/120 · 완료 기준 4/4)
- 테스트: ● GREEN · 725 passed · 전체 · 1.5s · cd C:/pickup && ./gradlew.bat test --rerun --console=plain > "C:/Users/keuye/AppData/Local/Temp/claude/86b70dd9bf0b8321…
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
- [x] #16 [persistence] Device를 refreshTokenHash로 찾는다 ← AuthPersistenceAdapterTest.Device를 refreshTokenHash로 찾는다
- [x] #17 [persistence] loadLiveDevices는 revokedAt이 빈 기기만 돌려준다 ← AuthPersistenceAdapterTest.loadLiveDevices는 revokedAt이 빈 기기만 돌려준다
- [x] #18 [persistence] loadLinkCodeByCode는 8자 code로 연결 코드를 찾는다 ← AuthPersistenceAdapterTest.loadLinkCodeByCode는 8자 code로 연결 코드를 찾는다
- [x] #19 [persistence] loadIssuedCodes는 그 차량의 ISSUED 코드만 돌려준다 ← AuthPersistenceAdapterTest.loadIssuedCodes는 그 차량의 ISSUED 코드만 돌려준다
- [x] #20 [persistence] MemberInvite를 token으로 찾고 같은 token 두 개는 유니크 위반이 난다 ← AuthPersistenceAdapterTest.MemberInvite를 token으로 찾고 같은 token 두 개는 유니크 위반이 난다
- [x] #21 [persistence] loadLatestInvite는 그 멤버의 가장 최근 초대를 돌려준다 ← AuthPersistenceAdapterTest.loadLatestInvite는 그 멤버의 가장 최근 초대를 돌려준다
- [x] #22 [서비스] 토큰 발급은 refresh 원문을 돌려주고 Device에는 그 SHA-256 해시만 저장한다 ← RefreshTokenServiceTest.토큰 발급은 refresh 원문을 돌려주고 Device에는 그 SHA-256 해시만 저장한다
- [x] #23 [서비스] 발급한 access 토큰에는 주체 · familyId · role이 들어 있다 ← JwtTokenIssuerTest.발급한 access 토큰에는 주체 · familyId · role이 들어 있다
- [x] #24 [서비스] 토큰 갱신은 새 refresh 토큰을 주고 이전 refresh 토큰으로 다시 갱신하면 REFRESH_TOKEN_INVALID로 거절한다 ← RefreshTokenServiceTest.토큰 갱신은 새 refresh 토큰을 주고 이전 refresh 토큰으로 다시 갱신하면 REFRESH_TOKEN_INVALID로 거절한다
- [x] #25 [서비스] 폐기된 기기의 refresh 토큰으로 갱신하면 REFRESH_TOKEN_INVALID로 거절한다 ← RefreshTokenServiceTest.폐기된 기기의 refresh 토큰으로 갱신하면 REFRESH_TOKEN_INVALID로 거절한다
- [x] #26 [서비스] 차량 기기 등록은 연결된 VIN이면 VEHICLE_ALREADY_LINKED로 거절한다 ← RegisterVehicleDeviceServiceTest.차량 기기 등록은 연결된 VIN이면 VEHICLE_ALREADY_LINKED로 거절한다
- [x] #27 [서비스] 차량 기기 등록은 같은 VIN의 연결 대기 행이 있으면 새 Vehicle을 만들지 않고 그 vehicleId를 쓴다 ← RegisterVehicleDeviceServiceTest.차량 기기 등록은 같은 VIN의 연결 대기 행이 있으면 새 Vehicle을 만들지 않고 그 vehicleId를 쓴다
- [x] #28 [서비스] 차량 기기 등록은 ownerType VEHICLE · platform VEHICLE인 Device를 만든다 ← RegisterVehicleDeviceServiceTest.차량 기기 등록은 ownerType VEHICLE · platform VEHICLE인 Device를 만든다
- [x] #29 [서비스] 연결 전 기기 토큰으로는 LINK-01 · LINK-02 외의 API를 부를 수 없다 ← PreLinkTokenInterceptorTest.연결 전 기기 토큰으로는 LINK-01 · LINK-02 외의 API를 부를 수 없다
- [x] #30 [서비스] 푸시 토큰 갱신은 지금 기기의 pushToken과 pushUpdatedAt을 바꾼다 ← UpdatePushTokenServiceTest.푸시 토큰 갱신은 지금 기기의 pushToken과 pushUpdatedAt을 바꾼다
- [x] #31 [서비스] 차량 로그아웃은 진행 중 픽업이 있으면 ACTIVE_PICKUP_EXISTS로 거절한다 ← LogoutVehicleServiceTest.차량 로그아웃은 진행 중 픽업이 있으면 ACTIVE_PICKUP_EXISTS로 거절한다
- [x] #32 [서비스] 차량 로그아웃이 성공하면 그 기기의 revokedAt이 채워진다 ← LogoutVehicleServiceTest.차량 로그아웃이 성공하면 그 기기의 revokedAt이 채워진다
- [x] #33 [서비스] 연결 코드 발급은 같은 기기가 발급한 이전 ISSUED 코드를 EXPIRED로 바꾼다 ← IssueLinkCodeServiceTest.연결 코드 발급은 같은 기기가 발급한 이전 ISSUED 코드를 EXPIRED로 바꾼다
- [x] #34 [서비스] 연결 코드 발급은 첫 발급 때 reissuesLeft 4를 돌려준다 ← IssueLinkCodeServiceTest.연결 코드 발급은 첫 발급 때 reissuesLeft 4를 돌려준다
- [x] #35 [서비스] 연결 코드를 연속 6번째로 발급하면 LINK_CODE_REISSUE_LIMIT으로 거절한다 ← IssueLinkCodeServiceTest.연결 코드를 연속 6번째로 발급하면 LINK_CODE_REISSUE_LIMIT으로 거절한다
- [x] #36 [서비스] 연결 상태 조회는 다른 차량이 발급한 코드면 NOT_CODE_OWNER로 거절한다 ← GetLinkStatusServiceTest.연결 상태 조회는 다른 차량이 발급한 코드면 NOT_CODE_OWNER로 거절한다
- [x] #37 [서비스] 연결 상태 조회는 승인 전 코드에 WAITING과 pollAfterSeconds 2를 돌려준다 ← GetLinkStatusServiceTest.연결 상태 조회는 승인 전 코드에 WAITING과 pollAfterSeconds 2를 돌려준다
- [x] #38 [서비스] 연결 상태 조회는 만료된 코드에 EXPIRED를 돌려준다 ← GetLinkStatusServiceTest.연결 상태 조회는 만료된 코드에 EXPIRED를 돌려준다
- [x] #39 [서비스] 연결 상태 조회는 NEW로 승인된 코드에 LINKED와 vehicleId · familyId · 차량 토큰을 돌려준다 ← GetLinkStatusServiceTest.연결 상태 조회는 NEW로 승인된 코드에 LINKED와 vehicleId · familyId · 차량 토큰을 돌려준다
- [x] #40 [서비스] 연결 상태 조회는 RELINK로 승인된 코드에 RELINKED를 돌려준다 ← GetLinkStatusServiceTest.연결 상태 조회는 RELINK로 승인된 코드에 RELINKED를 돌려준다
- [x] #41 [서비스] 차량 토큰을 내려준 코드를 다시 조회하면 LINK_CODE_INVALID로 거절한다 ← GetLinkStatusServiceTest.차량 토큰을 내려준 코드를 다시 조회하면 LINK_CODE_INVALID로 거절한다
- [x] #42 [서비스] 연결 코드 확인은 가족에 연결된 차량의 코드면 RELINK, 아니면 NEW를 돌려준다 ← CheckLinkCodeServiceTest.연결 코드 확인은 가족에 연결된 차량의 코드면 RELINK, 아니면 NEW를 돌려준다
- [x] #43 [서비스] 연결 코드 확인은 사용된 코드면 LINK_CODE_EXPIRED로 거절한다 ← CheckLinkCodeServiceTest.연결 코드 확인은 사용된 코드면 LINK_CODE_EXPIRED로 거절한다
- [x] #44 [서비스] 연결 코드 확인이 같은 기기에서 5번 실패하면 1분 동안 LINK_CODE_ATTEMPT_LIMIT으로 거절한다 ← CheckLinkCodeServiceTest.연결 코드 확인이 같은 기기에서 5번 실패하면 1분 동안 LINK_CODE_ATTEMPT_LIMIT으로 거절한다
- [x] #45 [서비스] 연결 승인은 mode가 코드의 실제 상태와 다르면 LINK_MODE_MISMATCH로 거절한다 ← ApproveLinkServiceTest.연결 승인은 mode가 코드의 실제 상태와 다르면 LINK_MODE_MISMATCH로 거절한다
- [x] #46 [서비스] 연결 승인은 TERMS나 PRIVACY 동의가 없으면 CONSENT_REQUIRED로 거절한다 ← ApproveLinkServiceTest.연결 승인은 TERMS나 PRIVACY 동의가 없으면 CONSENT_REQUIRED로 거절한다
- [x] #47 [서비스] 연결 승인 NEW는 CreateGuardianUseCase를 불러 가족과 GUARDIAN을 만든다 ← ApproveLinkServiceTest.연결 승인 NEW는 CreateGuardianUseCase를 불러 가족과 GUARDIAN을 만든다
- [x] #48 [서비스] 연결 승인 RELINK는 기존 GUARDIAN에게 토큰을 주고 그 GUARDIAN의 이전 기기를 폐기한다 ← ApproveLinkServiceTest.연결 승인 RELINK는 기존 GUARDIAN에게 토큰을 주고 그 GUARDIAN의 이전 기기를 폐기한다
- [x] #49 [서비스] 연결 승인 RELINK는 이전 기기 알림 이벤트를 발행한다 ← ApproveLinkServiceTest.연결 승인 RELINK는 이전 기기 알림 이벤트를 발행한다
- [x] #50 [서비스] 연결 승인은 요청의 pushToken으로 Device를 만든다 ← ApproveLinkServiceTest.연결 승인은 요청의 pushToken으로 Device를 만든다
- [x] #51 [서비스] 연결 승인이 성공하면 코드가 USED가 되고 같은 코드로 다시 승인하면 LINK_CODE_EXPIRED로 거절한다 ← ApproveLinkServiceTest.연결 승인이 성공하면 코드가 USED가 되고 같은 코드로 다시 승인하면 LINK_CODE_EXPIRED로 거절한다
- [x] #52 [서비스] 초대 발급은 기기가 연결된 멤버에 replaceDevice false면 DEVICE_ALREADY_CONNECTED로 거절한다 ← IssueInviteServiceTest.초대 발급은 기기가 연결된 멤버에 replaceDevice false면 DEVICE_ALREADY_CONNECTED로 거절한다
- [x] #53 [서비스] 초대를 다시 발급하면 이전 초대 토큰으로는 확인 · 수락할 수 없다 ← IssueInviteServiceTest.초대를 다시 발급하면 이전 초대 토큰으로는 확인 · 수락할 수 없다
- [x] #54 [서비스] 초대 발급은 다른 가족의 멤버면 NOT_FOUND로 거절한다 ← IssueInviteServiceTest.초대 발급은 다른 가족의 멤버면 NOT_FOUND로 거절한다
- [x] #55 [서비스] 초대 확인은 role CHILD 멤버에 requiredConsents TERMS · PRIVACY · LOCATION · PUSH를 돌려준다 ← GetInviteServiceTest.초대 확인은 role CHILD 멤버에 requiredConsents TERMS · PRIVACY · LOCATION · PUSH를 돌려준다
- [x] #56 [서비스] 초대 수락은 requiredConsents 중 하나라도 agreed가 아니면 CONSENT_REQUIRED로 거절한다 ← AcceptInviteServiceTest.초대 수락은 requiredConsents 중 하나라도 agreed가 아니면 CONSENT_REQUIRED로 거절한다
- [x] #57 [서비스] 초대 수락은 Device를 만들고 pushToken을 저장하고 usedAt을 채운다 ← AcceptInviteServiceTest.초대 수락은 Device를 만들고 pushToken을 저장하고 usedAt을 채운다
- [x] #58 [서비스] 사용된 초대를 다시 수락하면 INVITE_ALREADY_USED로 거절한다 ← AcceptInviteServiceTest.사용된 초대를 다시 수락하면 INVITE_ALREADY_USED로 거절한다
- [x] #59 [서비스] 만료된 초대를 수락하면 INVITE_EXPIRED로 거절한다 ← AcceptInviteServiceTest.만료된 초대를 수락하면 INVITE_EXPIRED로 거절한다
- [x] #60 [서비스] revokeDevicesOf는 그 멤버의 살아 있는 기기를 모두 폐기한다 ← RevokeMemberDevicesServiceTest.revokeDevicesOf는 그 멤버의 살아 있는 기기를 모두 폐기한다
- [x] #61 [서비스] findPushTargets는 폐기됐거나 pushToken이 빈 기기를 빼고 돌려준다 ← LoadPushTargetServiceTest.findPushTargets는 폐기됐거나 pushToken이 빈 기기를 빼고 돌려준다
- [x] #62 [web] POST /v1/auth/refresh는 인증 헤더 없이 200과 accessToken · refreshToken · accessTokenExpiresAt을 돌려준다 ← AuthControllerTest.POST /v1/auth/refresh는 인증 헤더 없이 200과 accessToken · refreshToken · accessTokenExpiresAt을 돌려준다
- [x] #63 [web] POST /v1/auth/refresh에 폐기된 토큰을 보내면 401 REFRESH_TOKEN_INVALID를 돌려준다 ← AuthControllerTest.POST /v1/auth/refresh에 폐기된 토큰을 보내면 401 REFRESH_TOKEN_INVALID를 돌려준다
- [x] #64 [web] POST /v1/vehicle-devices는 인증 헤더 없이 201과 deviceId · vehicleId · deviceToken을 돌려준다 ← VehicleDeviceControllerTest.POST /v1/vehicle-devices는 인증 헤더 없이 201과 deviceId · vehicleId · deviceToken을 돌려준다
- [x] #65 [web] POST /v1/vehicle-devices에 16자 vin을 보내면 422 VALIDATION_FAILED를 돌려준다 ← VehicleDeviceControllerTest.POST /v1/vehicle-devices에 16자 vin을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] #66 [web] POST /v1/vehicle-devices가 연결된 VIN이면 409 VEHICLE_ALREADY_LINKED를 돌려준다 ← VehicleDeviceControllerTest.POST /v1/vehicle-devices가 연결된 VIN이면 409 VEHICLE_ALREADY_LINKED를 돌려준다
- [x] #67 [web] PUT /v1/devices/me/push-token은 204를 돌려준다 ← AuthControllerTest.PUT /v1/devices/me/push-token은 204를 돌려준다
- [x] #68 [web] PUT /v1/devices/me/push-token에 256자 pushToken을 보내면 422 VALIDATION_FAILED를 돌려준다 ← AuthControllerTest.PUT /v1/devices/me/push-token에 256자 pushToken을 보내면 422 VALIDATION_FAILED를 돌려준다
- [x] #69 [web] DELETE /v1/vehicle-devices/me/session은 204를 돌려준다 ← VehicleDeviceControllerTest.DELETE /v1/vehicle-devices/me/session은 204를 돌려준다
- [x] #70 [web] DELETE /v1/vehicle-devices/me/session이 진행 중 픽업이 있으면 409 ACTIVE_PICKUP_EXISTS를 돌려준다 ← VehicleDeviceControllerTest.DELETE /v1/vehicle-devices/me/session이 진행 중 픽업이 있으면 409 ACTIVE_PICKUP_EXISTS를 돌려준다
- [x] #71 [web] POST /v1/vehicle-link-codes는 201과 codeId · code · qrPayload · expiresAt · reissuesLeft를 돌려준다 ← VehicleLinkCodeControllerTest.POST /v1/vehicle-link-codes는 201과 codeId · code · qrPayload · expiresAt · reissuesLeft를 돌려준다
- [x] #72 [web] POST /v1/vehicle-link-codes가 한도를 넘으면 429 LINK_CODE_REISSUE_LIMIT을 돌려준다 ← VehicleLinkCodeControllerTest.POST /v1/vehicle-link-codes가 한도를 넘으면 429 LINK_CODE_REISSUE_LIMIT을 돌려준다
- [x] #73 [web] GET /v1/vehicle-link-codes/{codeId}/status는 200과 status를 돌려준다 ← VehicleLinkCodeControllerTest.GET /v1/vehicle-link-codes/{codeId}/status는 200과 status를 돌려준다
- [x] #74 [web] GET /v1/vehicle-link-codes/{codeId}/status를 다른 차량 토큰으로 부르면 403 NOT_CODE_OWNER를 돌려준다 ← VehicleLinkCodeControllerTest.GET /v1/vehicle-link-codes/{codeId}/status를 다른 차량 토큰으로 부르면 403 NOT_CODE_OWNER를 돌려준다
- [x] #75 [web] GET /v1/vehicle-link-codes/{codeId}/status를 1초 안에 다시 부르면 429 TOO_MANY_REQUESTS를 돌려준다 ← VehicleLinkCodeControllerTest.GET /v1/vehicle-link-codes/{codeId}/status를 1초 안에 다시 부르면 429 TOO_MANY_REQUESTS를 돌려준다
- [x] #76 [web] GET /v1/vehicle-link-codes/by-code/{code}는 인증 헤더 없이 200과 status · vehicleModel · plateNumberMasked · expiresAt을 돌려준다 ← VehicleLinkCodeControllerTest.GET /v1/vehicle-link-codes/by-code/{code}는 인증 헤더 없이 200과 status · vehicleModel · plateNumberMasked · expiresAt을 돌려준다
- [x] #77 [web] GET /v1/vehicle-link-codes/by-code/{code}가 없는 코드면 404 LINK_CODE_INVALID를 돌려준다 ← VehicleLinkCodeControllerTest.GET /v1/vehicle-link-codes/by-code/{code}가 없는 코드면 404 LINK_CODE_INVALID를 돌려준다
- [x] #78 [web] GET /v1/vehicle-link-codes/by-code/{code}가 만료된 코드면 410 LINK_CODE_EXPIRED를 돌려준다 ← VehicleLinkCodeControllerTest.GET /v1/vehicle-link-codes/by-code/{code}가 만료된 코드면 410 LINK_CODE_EXPIRED를 돌려준다
- [x] #79 [web] POST /v1/vehicle-links는 인증 헤더 없이 201과 vehicleId · familyId · userId · accessToken · refreshToken을 돌려준다 ← VehicleLinkCodeControllerTest.POST /v1/vehicle-links는 인증 헤더 없이 201과 vehicleId · familyId · userId · accessToken · refreshToken을 돌려준다
- [x] #80 [web] POST /v1/vehicle-links가 mode 불일치면 409 LINK_MODE_MISMATCH를 돌려준다 ← VehicleLinkCodeControllerTest.POST /v1/vehicle-links가 mode 불일치면 409 LINK_MODE_MISMATCH를 돌려준다
- [x] #81 [web] POST /v1/vehicle-links가 필수 동의 누락이면 422 CONSENT_REQUIRED를 돌려준다 ← VehicleLinkCodeControllerTest.POST /v1/vehicle-links가 필수 동의 누락이면 422 CONSENT_REQUIRED를 돌려준다
- [x] #82 [web] POST /v1/families/me/members/{userId}/invites는 201과 inviteUrl · expiresAt을 돌려준다 ← InviteControllerTest.POST /v1/families/me/members/{userId}/invites는 201과 inviteUrl · expiresAt을 돌려준다
- [x] #83 [web] POST /v1/families/me/members/{userId}/invites가 기기 연결 멤버에 replaceDevice false면 409 DEVICE_ALREADY_CONNECTED를 돌려준다 ← InviteControllerTest.POST /v1/families/me/members/{userId}/invites가 기기 연결 멤버에 replaceDevice false면 409 DEVICE_ALREADY_CONNECTED를 돌려준다
- [x] #84 [web] POST /v1/families/me/members/{userId}/invites를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← InviteControllerTest.POST /v1/families/me/members/{userId}/invites를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] #85 [web] GET /v1/invites/{token}은 인증 헤더 없이 200과 inviterName · memberName · role · requiredConsents · expiresAt을 돌려준다 ← InviteControllerTest.GET /v1/invites/{token}은 인증 헤더 없이 200과 inviterName · memberName · role · requiredConsents · expiresAt을 돌려준다
- [x] #86 [web] GET /v1/invites/{token}이 사용된 초대면 409 INVITE_ALREADY_USED를 돌려준다 ← InviteControllerTest.GET /v1/invites/{token}이 사용된 초대면 409 INVITE_ALREADY_USED를 돌려준다
- [x] #87 [web] GET /v1/invites/{token}이 만료된 초대면 410 INVITE_EXPIRED를 돌려준다 ← InviteControllerTest.GET /v1/invites/{token}이 만료된 초대면 410 INVITE_EXPIRED를 돌려준다
- [x] #88 [web] POST /v1/invites/{token}/acceptance는 인증 헤더 없이 201과 userId · familyId · role · accessToken · refreshToken을 돌려준다 ← InviteControllerTest.POST /v1/invites/{token}/acceptance는 인증 헤더 없이 201과 userId · familyId · role · accessToken · refreshToken을 돌려준다
- [x] #89 [web] POST /v1/invites/{token}/acceptance가 필수 동의 누락이면 422 CONSENT_REQUIRED를 돌려준다 ← InviteControllerTest.POST /v1/invites/{token}/acceptance가 필수 동의 누락이면 422 CONSENT_REQUIRED를 돌려준다
- [x] #90 [web] Idempotency-Key는 POST /v1/vehicle-devices · POST /v1/vehicle-links · POST /v1/families/me/members/{userId}/invites · POST /v1/invites/{token}/acceptance에만 요구된다 ← AuthIdempotencyKeyTest.Idempotency-Key는 POST /v1/vehicle-devices · POST /v1/vehicle-links · POST /v1/families/me/members/{userId}/invites · POST /v1/invites/{token}/acceptance에만 요구된다
- [x] #91 [기반] LinkCode는 mode(NEW · RELINK)와 tokenDeliveredAt을 갖고, 승인하면 mode가 채워지고 USED가 되며 토큰을 내려주면 tokenDeliveredAt이 채워진다 ← LinkCodeTest.LinkCode는 mode(NEW · RELINK)와 tokenDeliveredAt을 갖고, 승인하면 mode가 채워지고 USED가 되며 토큰을 내려주면 tokenDeliveredAt이 채워진다
- [x] #92 [기반] 공통 IntegrationTestSupport는 도메인 시드를 Import하지 않고 시드가 필요한 테스트가 UserSeed · AuthSeed를 직접 Import한다 ← IntegrationTestSupportTest.공통 IntegrationTestSupport는 도메인 시드를 Import하지 않고 시드가 필요한 테스트가 UserSeed · AuthSeed를 직접 Import한다
- [x] #93 [서비스] 연결 승인 NEW는 요청의 name으로 GUARDIAN을 만든다 ← ApproveLinkServiceTest.연결 승인 NEW는 요청의 name으로 GUARDIAN을 만든다
- [x] #94 [web] POST /v1/vehicle-links에 mode NEW로 name 없이 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려준다 ← VehicleLinkCodeControllerTest.POST /v1/vehicle-links에 mode NEW로 name 없이 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려준다
- [x] #95 [기반] LinkCode · MemberInvite · Device는 restore로 저장된 값을 그대로 가진 객체를 만든다 ← DeviceTest.LinkCode · MemberInvite · Device는 restore로 저장된 값을 그대로 가진 객체를 만든다
- [x] #96 [기반] LinkCode.expire는 status를 EXPIRED로, MemberInvite.markUsed는 usedAt을, MemberInvite.revoke는 expiresAt을 지금으로 바꾼다 ← MemberInviteTest.LinkCode.expire는 status를 EXPIRED로, MemberInvite.markUsed는 usedAt을, MemberInvite.revoke는 expiresAt을 지금으로 바꾼다
- [x] #97 [기반] Device.revoke는 revokedAt을, Device.updatePushToken은 pushToken · pushUpdatedAt을, Device.rotateRefreshToken은 refreshTokenHash를 바꾼다 ← DeviceTest.Device.revoke는 revokedAt을, Device.updatePushToken은 pushToken · pushUpdatedAt을, Device.rotateRefreshToken은 refreshTokenHash를 바꾼다
- [x] #98 [기반] pickup 구현 전 임시 CheckActivePickupPort는 false를 돌려주고, PublishAuthEventPort는 이전 기기 id를 담은 스프링 이벤트를 발행한다 ← AuthModuleLinkAdaptersTest.pickup 구현 전 임시 CheckActivePickupPort는 false를 돌려주고, PublishAuthEventPort는 이전 기기 id를 담은 스프링 이벤트를 발행한다
- [x] #99 [통합] user의 AuthMemberDeviceAdapter는 auth GetMemberDeviceStatusUseCase 결과를 user DeviceStatus로 바꿔 돌려준다 ← AuthMemberDeviceAdapterTest.user의 AuthMemberDeviceAdapter는 auth GetMemberDeviceStatusUseCase 결과를 user DeviceStatus로 바꿔 돌려준다
- [x] #100 [통합] user의 RevokeMemberDevicesPort 구현은 auth RevokeMemberDevicesUseCase를 부른다 ← AuthMemberDeviceAdapterTest.user의 RevokeMemberDevicesPort 구현은 auth RevokeMemberDevicesUseCase를 부른다
- [x] #101 [서비스] user의 LoadVehicleUseCase는 findVehicle · findLinkedVehicleByVin으로 VehicleInfo를 돌려준다 ← LoadVehicleServiceTest.user의 LoadVehicleUseCase는 findVehicle · findLinkedVehicleByVin으로 VehicleInfo를 돌려준다
- [x] #102 [서비스] user의 LoadMemberUseCase는 findMember · findMembers로 MemberInfo를 돌려주고 LoadFamilyUseCase는 findFamily로 FamilyInfo를 돌려준다 ← LoadMemberServiceTest.user의 LoadMemberUseCase는 findMember · findMembers로 MemberInfo를 돌려주고 LoadFamilyUseCase는 findFamily로 FamilyInfo를 돌려준다
- [x] #103 [통합] 이름 규칙(NameRule)은 common에 한 번만 두고 user · auth web 검증이 그것을 쓴다 ← NameRuleTest.이름 규칙(NameRule)은 common에 한 번만 두고 user · auth web 검증이 그것을 쓴다
- [x] #104 [통합] 번호판 마스킹(PlateNumberMasker)은 common에 한 번만 두고 user 가족 조회 · auth 연결 코드 확인이 그것을 쓴다 ← PlateNumberMaskerTest.번호판 마스킹(PlateNumberMasker)은 common에 한 번만 두고 user 가족 조회 · auth 연결 코드 확인이 그것을 쓴다
- [x] #105 [서비스] 연결 코드 발급은 연결 전 토큰(familyId 없음)인데 그 차량이 이미 가족에 연결돼 있으면 VEHICLE_ALREADY_LINKED로 거절한다 ← IssueLinkCodeServiceTest.연결 코드 발급은 연결 전 토큰(familyId 없음)인데 그 차량이 이미 가족에 연결돼 있으면 VEHICLE_ALREADY_LINKED로 거절한다
- [x] #106 [서비스] 초대 발급은 대상 멤버의 role이 GUARDIAN이면 FORBIDDEN으로 거절한다 ← IssueInviteServiceTest.초대 발급은 대상 멤버의 role이 GUARDIAN이면 FORBIDDEN으로 거절한다
- [x] #107 [서비스] 연결 코드는 발급한 deviceId를 기록하고, 연결 상태 조회는 발급한 기기가 아니면 NOT_CODE_OWNER로 거절하며 차량 토큰은 그 기기에만 준다 ← GetLinkStatusServiceTest.연결 코드는 발급한 deviceId를 기록하고, 연결 상태 조회는 발급한 기기가 아니면 NOT_CODE_OWNER로 거절하며 차량 토큰은 그 기기에만 준다
- [x] #108 [서비스] 초대 수락은 그 멤버의 살아 있는 이전 기기를 모두 폐기한다 ← AcceptInviteServiceTest.초대 수락은 그 멤버의 살아 있는 이전 기기를 모두 폐기한다
- [x] #109 [서비스] 연결 전 토큰으로 발급한 코드는 승인 때 mode가 RELINK면 LINK_MODE_MISMATCH로 거절한다 ← ApproveLinkServiceTest.연결 전 토큰으로 발급한 코드는 승인 때 mode가 RELINK면 LINK_MODE_MISMATCH로 거절한다
- [x] #110 [서비스] 연결 코드 발급 횟수는 기기(deviceId) 단위로 센다 ← IssueLinkCodeServiceTest.연결 코드 발급 횟수는 기기(deviceId) 단위로 센다
- [x] #111 [서비스] 연결 코드 발급은 같은 기기가 발급한 이전 ISSUED 코드만 EXPIRED로 바꾼다 ← IssueLinkCodeServiceTest.연결 코드 발급은 같은 기기가 발급한 이전 ISSUED 코드만 EXPIRED로 바꾼다
- [x] #112 [web] GET /v1/vehicle-link-codes/{codeId}/status의 1초 호출 제한은 기기(deviceId) 단위로 센다 ← VehicleLinkCodeControllerTest.GET /v1/vehicle-link-codes/{codeId}/status의 1초 호출 제한은 기기(deviceId) 단위로 센다
- [x] #113 LINK-01: 발급 기기 정보가 없는 옛 ISSUED 코드(issuedDeviceId null)는 같은 차량에서 새 코드를 발급할 때 함께 EXPIRED가 된다 ← IssueLinkCodeServiceTest.LINK-01: 발급 기기 정보가 없는 옛 ISSUED 코드(issuedDeviceId null)는 같은 차량에서 새 코드를 발급할 때 함께 EXPIRED가 된다
- [x] #114 LINK-04: 발급 기기 정보가 없는 옛 코드로 승인하면 발급 카운터 초기화를 건너뛰고 승인은 성공한다 ← ApproveLinkServiceTest.LINK-04: 발급 기기 정보가 없는 옛 코드로 승인하면 발급 카운터 초기화를 건너뛰고 승인은 성공한다
- [x] #115 LINK-04: 같은 연결 대기 차량에 서로 다른 코드로 NEW 승인이 동시에 들어오면 하나만 성공하고 나머지는 409 VEHICLE_ALREADY_LINKED로 실패한다(vehicle에 @Version 낙관적 잠금, 충돌은 409로 바꾼다) ← VehicleLinkCodeControllerConcurrencyTest.같은 연결 대기 차량에 서로 다른 코드로 NEW 승인이 동시에 들어오면 하나만 201이고 나머지는 409 VEHICLE_ALREADY_LINKED이며 가족은 1개만 남는다
- [x] #116 LINK-01: 연결이 해제된 차량 행(unlinkedAt 있음)의 기기가 코드를 발급하면 409 VEHICLE_UNLINKED로 거절한다(앱을 다시 등록해야 함) ← IssueLinkCodeServiceTest.LINK-01: 연결이 해제된 차량 행(unlinkedAt 있음)의 기기가 연결된 토큰(familyId 있음)으로 코드를 발급하면 409 VEHICLE_UNLINKED로 거절한다
- [x] #117 [서비스] LINK-05로 차량 연결을 해제하면 그 차량의 살아 있는 기기를 모두 폐기한다(해제된 차량 기기는 refresh로 이전 가족의 토큰을 받을 수 없고 AUTH-02로 다시 등록해야 함) ← UnlinkVehicleServiceTest.LINK-05로 차량 연결을 해제하면 그 차량의 살아 있는 기기를 모두 폐기하도록 vehicleId로 요청한다
- [x] #118 [persistence] vehicle.version 컬럼은 NOT NULL에 기본값 0을 가진다(행이 있는 DB에도 컬럼을 추가할 수 있음) ← UserPersistenceEntitiesMappingTest.vehicle.version 컬럼은 BIGINT NOT NULL이고 DB 기본값 0을 가져 행이 있는 테이블에도 추가할 수 있다
- [x] #119 [web] POST /v1/vehicle-link-codes를 연결 전 토큰(familyId 없음)으로 부르는데 차량이 이미 가족에 연결돼 있으면 409 VEHICLE_ALREADY_LINKED를 돌려준다 ← VehicleLinkCodeControllerTest.POST /v1/vehicle-link-codes를 연결 전 토큰(familyId 없음)으로 부르는데 차량이 이미 가족에 연결돼 있으면 409 VEHICLE_ALREADY_LINKED를 돌려준다
- [x] #120 [web] POST /v1/families/me/members/{userId}/invites의 대상 멤버가 GUARDIAN이면 403 FORBIDDEN을 돌려준다 ← InviteControllerTest.POST /v1/families/me/members/{userId}/invites의 대상 멤버가 GUARDIAN이면 403 FORBIDDEN을 돌려준다

## 미완료 항목
- 없음

## 완료 기준 (공통 규칙 문서)
- [x] C1 설계서의 테스트 케이스가 모두 GREEN
- [x] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [x] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [x] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 2026-11-07 23:03 KST · 도메인 리뷰어 · 결론 통과 · 상 1 · 중 0 · 하 5 · 리뷰 시점 체크 120/120
- 리뷰 시점 테스트: ● GREEN · 725 passed · 전체 · 1.5s · cd C:/pickup && ./gradlew.bat test --rerun --console=plain > "C:/Users/keuye/AppData/Local/Temp/claude/86b70dd9bf0b8321…
  - 결론: 통과
  - [상] 0건, [중] 0건입니다. 지난 리뷰의 [중] 3건 가운데 2건은 해결됐고, 남은 1건(user #64)은 user 도메인 일이며 후속 과제로 기록돼 있습니다. 이번에 새로 찾은 것은 [하] 5건이고 모두 문서 · 테스트 보강입니다. 동작을 바꿔야 하는 것은 없습니다.
  - **지난 리뷰(22:43) 지적이 해소됐는지**
  - | 지난 지적 | 상태 | 근거 |
  - |---|---|---|
  - | [중] 05 본문이 구현과 다른 곳 6개 | 해소 | 구조 05:15 · 19(VehicleLinkCodeController, adapter/out/pickup), 시그니처 05:152 MemberDeviceStatus · 05:162 hashRefreshToken, 기기 상태 표 05:133 NONE 행, name 결정 05:307, VEHICLE_MOVING 미구현 표시 05:75 · 115 · 305, #33 문장 05:201. 실제 in · out 포트 메서드 26개가 05:139-165와 모두 같음 |
  - | [중] LINK-05로 해제한 뒤에도 차량 기기가 refresh로 이전 가족 토큰을 받음 | 해소(#117) | UnlinkVehicleService:44가 RevokeVehicleDevicesPort를 부름 → AuthVehicleDeviceAdapter → RevokeVehicleDevicesService:26-31. RevokeVehicleDevicesServiceTest가 폐기 뒤 refresh하면 REFRESH_TOKEN_INVALID 401인 것까지 확인함 |
  - | [중] user #64 증거 테스트 없음 | 후속 과제로 기록됨 | user.md:72의 DisplayName이 UserModuleLinkAdaptersTest(:44 · 53 · 64 · 84 · 92 · 109)에 없음. 04:198 문장은 아직 "임시 어댑터 NONE · no-op"인데 실제는 위임함 |
  - | [하] vehicle.version에 기본값 없음 | 해소(#118) | VehicleEntity에 `@Version @Column(nullable = false) @ColumnDefault("0")`. UserPersistenceEntitiesMappingTest:88-101이 BIGINT · NOT NULL · 기본값 0을 확인함 |
  - | [하] web 테스트 없음(LINK-01 409, FAM-04 GUARDIAN 403) | 해소(#119 · #120) | VehicleLinkCodeControllerTest:187, InviteControllerTest:169. 둘 다 대역(mock)으로 예외 → 응답 변환을 보고, 판정 규칙은 서비스 테스트 #105 · #106이 봄 |
  - | [하] #33 문장이 #111과 모순 | 해소 | 05:201 · auth.md:41 · IssueLinkCodeServiceTest:97이 모두 "같은 기기가 발급한 이전 ISSUED 코드"로 맞춰짐 |
  - | [하] versionToSave 대체 분기 | 후속 과제로 기록됨(사람 결정) | UserPersistenceAdapter:83-88 그대로 |
  - | [하] 동시 요청 500 2개, RELINK · FAM-06 동시 처리, 다른 기기의 ISSUED 코드 · 인덱스, 카운터 Map 정리, 해제 직전 코드가 남음(UnlinkVehicleService가 코드를 만료시키지 않음), 지난 [하] 묶음 | 후속 과제로 기록됨 | auth.md:264-265 · 272-274 |
  - | [하] 02:19 의존 방향에 user → auth 없음, 04:20 구조에 common.text 없음 | 후속 과제로 기록됨 | 02:19, 04:20("이름 · 전화번호 규칙"이 아직 user domain에 있다고 적힘). domain 패키지는 common.text를 import하지 않아 02:70 위반은 아님 |
  - | [하] #113~#116 문장에 lane 태그 없음 | 후속 과제로 기록됨 | 05:281-284 |
  - | [하] 변경이 커밋 안 됨 | 남음 | git status: 수정 51 · 삭제 4 · 추적 안 됨 9(RevokeVehicleDevices* 등 #117 파일 포함) |
  - **발견 사항 (이번 리뷰)**
  - [하] LINK-05 API 문서 설명이 #117 뒤 동작과 다름 — user VehicleLinkApiDoc:13 "행은 지우지 않고 unlinkedAt만 채운다"인데, 이제 그 차량의 살아 있는 기기도 폐기함(05:100, 04:93) → note(설명에 "차량 기기 모두 폐기, 앱은 AUTH-02로 다시 등록"을 더함)
  - [하] #117을 끝까지 잇는 통합 테스트 없음 — UnlinkVehicleServiceTest:89(대역 호출만 확인), AuthVehicleDeviceAdapterTest:25(대역 위임), RevokeVehicleDevicesServiceTest(메모리 저장소)로 쪼개서만 검증함. LINK-05 HTTP 호출 → DB의 device.revoked_at → AUTH-01 401로 이어지는 테스트가 없음. 빈 연결은 Spring 컨텍스트 기동으로 간접 확인됨 → 항목 추가(선택)
  - [하] LINK-05로 해제해 폐기한 차량 기기의 access 토큰이 만료(PT30M)까지 이전 familyId를 담고 유효함 — 05 확인 필요 사항 05:315에는 AUTH-04 뒤의 가정만 있고 LINK-05 뒤의 가정은 없음 → note(같은 가정이면 그 행에 "LINK-05 포함"을 더함)
  - [하] UserModuleLinkAdaptersTest:53 "세 포트는 각각 빈이 정확히 1개"가 새 RevokeVehicleDevicesPort를 보지 않음. AuthInPortsTest:95도 RevokeVehicleDevicesUseCase 시그니처를 보지 않음(후자는 auth.md:280에 기록됨) → note
  - [하] 진행 기록에서 지난 리뷰 개수가 실제 내용과 다름 — TODO.md:19 · REVIEW.md:10은 "상 1"인데, 지난 리뷰 본문은 "[상]은 0건"이고 [상] 줄도 없음 → note(기록 정정)
  - 완료 기준:
  - C1 충족 — 체크 120/120. `--rerun`으로 725 tests · 0 failures · 0 errors · 0 skipped
  - C2 충족 — 지난번 다르던 6곳이 모두 반영됨. 포트 시그니처 26개가 05:139-165와 같음. VEHICLE_MOVING은 명세에 "미구현, 확인 필요 사항 참조"로 표시돼 구현과 어긋나지 않음
  - C3 충족 — auth 코드의 공통 규칙 위반 0건. 새 파일 4개는 생성자 주입 · 주석 없음 · in 포트만 호출을 지킴. 문서가 늦은 곳(02:19 · 04:20 · 04:198)은 FOLLOWUP에 기록됨. 새 [하] VehicleLinkApiDoc:13은 note로 남길 것
  - C4 충족 — 05 확인 필요 사항 29행과 FOLLOWUP 표(auth.md:205-235)를 diff로 비교했더니 똑같음
  - 후속 과제 판정: 해소됨 — 사람 결정 11-07 묶음, 사용된 초대 NONE, 05 시그니처, LINK-05 뒤 refresh 이전 familyId(#117), 05 본문 · #33, API 문서, 연결 전 코드 NEW 승인 409 테스트, 04 name · 의존 칸, VEHICLE_MOVING 미구현 표시, #105 후속, issuedBeforeLink · 기기 단위. 설계 결정 필요 — code UNIQUE, refresh 만료, AUTH-02 refresh · 연결 전 토큰 만료, VEHICLE_MOVING 판단 근거 · 보호자 phone, VIN NEW 선점 소유 확인, versionToSave 분기, 폐기 반복 4곳 · 메서드 이름. 남음 — N+1 · 인덱스 · FK · token 로그, we…
  - 테스트: `cd C:/pickup && ./gradlew.bat test --rerun --console=plain` → BUILD SUCCESSFUL in 47s. 725 tests · 0 failures · 0 errors · 0 skipped.
- 이전 리뷰: 2026-11-07 22:43 KST (조건부 통과), 2026-11-07 22:06 KST (조건부 통과), 2026-11-07 21:41 KST (조건부 통과), 2026-11-07 21:19 KST (수정 필요)

## 확인 필요 사항 (설계서)
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
| 동의(consents) 저장 위치 | 저장 테이블이 없음. SIG-04의 LOCATION_CONSENT_REQUIRED가 위치 동의를 읽어야 함. 저장 위치를 정해야 함 |
| LINK-04 NEW로 만드는 GUARDIAN의 name · phone | name은 결정됨(사람 결정 2026-11-07). LINK-04 요청 name(1~20자, 이모지 불가), mode NEW일 때 필수 · RELINK면 무시. phone은 미정. 요청에 없어 null로 만들고 있어 user의 「자녀 외 phone 필수」 규칙과 어긋남. 보호자 전화번호를 받을 곳을 정해야 함 |
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
| VIN만 아는 사람의 NEW 선점 | VIN만 알면 AUTH-02 → LINK-01 → LINK-04(NEW)로 연결 안 된 차량을 먼저 차지할 수 있음. 진짜 주인은 그 뒤 AUTH-02에서 409 VEHICLE_ALREADY_LINKED를 받음. 차량 소유 확인 방법 미정, 후속 과제(사람 결정 2026-11-07) |

## 다음 단계
- 커밋 · 푸시: /tdd commit (기능별 분할 커밋) · /tdd push (커밋 후 기능 브랜치 푸시)
- 다음 도메인: 일정 (06_일정-schedule.md). 시작: /tdd domain schedule

## 후속 과제
- [사람 결정 2026-11-07] (1) VehicleLinkCode에 mode(NEW · RELINK, LINK-04 승인 때 채움) · tokenDeliveredAt(LINK-02가 토큰을 내려준 시각) 컬럼 추가 — 승인 시 USED, 토큰 내려준 뒤 다시 조회하면 404 LINK_CODE_INVALID. (2) LINK-04 요청에 name(1~20자, 이모지 불가) 추가, mode NEW일 때 필수 · RELINK면 무시. (3) 도메인 시드는 공통 IntegrationTestSupport에서 빼고 필요한 테스트가 직접 @Import(user C3 해소용). (4) 「연결 중」 차이: user loadLinkedVehicleByVin 결과의 familyId로 auth가 구분(familyId 있음 → 409 VEHICLE_ALREADY_LINKED, 없음 → 대기 행 재사용). 설계서 테이블 · API 명세 갱신은 도메인 끝날 때 반영.
- [설계 확인 · auth] 멤버 기기 상태: 사용된 초대만 있고 살아 있는 기기가 없을 때(수락 뒤 기기 폐기)는 설계서 표에 없음 → NONE으로 가정(DeviceStatusJudgeTest). 설계서 표에 행 추가 필요. 판정은 USER 기기만 넘긴다는 전제.
- [문서] 05 설계서 「시그니처」 갱신 필요: IssueTokenPort.hashRefreshToken(String) 추가, TokenSubject(type, id, familyId, role, deviceId) · IssuedTokens(accessToken, accessTokenExpiresAt, refreshToken, refreshTokenHash) 컴포넌트, 기기 상태 반환 타입 MemberDeviceStatus(auth.domain). [주의] out 포트 11개는 Bash로 만들어 tdd-guard 기반 파일로 등록되지 않았을 수 있음 — lane에서 포트를 고치지 않도록 프롬프트로 막을 것.
- [설계 확인 · auth] vehicle_link_code.code는 INDEX일 뿐 UNIQUE가 아니라 같은 8자 코드가 다시 발급될 수 있음. 조회는 codeId가 가장 큰 행으로 처리(findFirstByCodeOrderByCodeIdDesc). 발급 때 살아 있는(ISSUED) 코드와 중복을 막을지, 부분 유니크를 걸지 결정 필요. 「가장 최근 초대」는 inviteId 최대로 정함.
- [설계 확인 · auth] (1) LINK-05(연결 해제) 뒤 남은 차량 기기로 AUTH-01 갱신 시 VehicleInfo.familyId가 이전 가족 값으로 토큰에 담길 수 있음 — 해제 때 차량 기기를 폐기할지(설계서 확인 필요 사항: 폐기 안 함 가정), 갱신 때 linked를 확인할지 결정 필요. (2) access 유효 기간 PT30M 임시값, refresh 만료는 미구현.
- [persistence 중간 리뷰 · auth] 결론: 위반 [상] 없음, 테스트 정리만(493 passed). (중) LINK-01 연속 5회 발급 카운트는 out 포트로 셀 수 없음 → 팀장 결정: 설계서의 다른 횟수 제한(LINK-02 · LINK-03)과 같이 서버 메모리 카운터(차량별, 연결 승인 성공 시 0으로)로 service lane에서 구현. (중) getDeviceStatuses · findPushTargets가 멤버 수만큼 쿼리(N+1) — 여러 명 조회 out 포트 추가 여부 결정 필요. (하) device.refresh_token_hash 인덱스 · 유니크 없음, member_invite.user_id 인덱스 없음, 테이블 FK 표기와 실제(제약 없음) 불일치, 초대 token 중복 시 DB 오류 로그에 token 원문 노출 가능. 설계서 VehicleLinkCode 테이블 · 확인 필요 사항에 mode · tokenDeliveredAt 반영 필요.
- [설계 확인 · auth] AUTH-02 응답에는 deviceToken(access)만 있고 refresh 원문이 없음 → 연결 대기가 access 유효 기간보다 길면 차량 앱이 막힘. 응답에 refreshToken을 더할지, 연결 전 토큰 유효 기간을 따로 둘지 결정 필요.
- [설계 확인 · auth web] (1) deviceId 클레임 없는 토큰으로 AUTH-03 · 04 호출 시 401로 처리(가정). (2) 연결 전 차량 토큰으로 AUTH-04 호출 가능 여부(「LINK-01 · 02만」 규칙이면 403) — #29와 함께 정리. (3) AUTH-03에서 platform과 토큰 주체 불일치(USER 토큰 + VEHICLE) 검사 여부 미정. deviceId는 common 수정 없이 AuthPrincipalAuthenticationToken.getCredentials()의 Jwt에서 읽음.
- [설계 확인 · auth] 이미 폐기된 차량 기기로 AUTH-04 호출 시 응답(401 · 404 · 204) 미정. AUTH-03 platform 값은 Device.platform이 바뀌지 않아 무시됨(설계서에 갱신 대상인지 명시 필요). #29는 [서비스] 태그지만 구현은 adapter/in/web의 PreLinkTokenInterceptor.
- [설계 확인 · auth 승인] VEHICLE_MOVING(422)은 판단 근거가 없어 미구현. LINK-04 NEW로 만드는 GUARDIAN의 phone은 요청에 없어 null로 생성 — 보호자 전화번호를 받을 곳 결정 필요(user의 자녀 외 phone 필수 규칙과 어긋남). RELINK인데 이전 기기 없으면 이벤트 미발행, RELINK 가족에 GUARDIAN이 없으면 404로 가정.
- [service 리뷰 · auth] 결론: 통합 후 605 passed. 고친 것: PreLinkTokenInterceptor 범위를 /v1/**로(common AuthPrincipalTest 403 해결), 토큰 발급 · rotate · 저장을 DeviceTokenRotator로, 동의 검증 ConsentRules, 코드 판정 LinkCodeRules로 모음. (중) LINK-04 동시 승인 방지 장치 없음(@Version 또는 조건부 갱신 결정 필요) · FAM-06 replaceDevice=true 수락 때 이전 기기 폐기 미구현 · 연결 전 deviceToken(access 30분) 만료 뒤 복구 경로 · 발급 카운터가 승인 때만 0(5번 발급 뒤 미연결 차량은 재시작까지 429) — 설계 결정 필요. (하) 카운터 변경이 커밋 전, LinkCodeAttemptLimiter Map 무한 증가 가능, LoadPushTargetService isLive 중복, N+1, push-token platform 미사용, 폐기된 기기 재로그아웃 시 revokedAt 덮어씀, GUARDIAN 찾기 중복, IssueInviteService 생성자 7개. LINK-02 1초 제한은 web(LinkStatusPollGuard)에 있음.
- [web 리뷰 · auth] 결론: 통합 후 678 passed. 고친 것: PreLinkTokenInterceptor 경로를 UrlPathHelper로(servletPath 빈 값 문제), 인터셉터 순서를 맨 뒤로(403이 요청 로그에 남게), PreLinkTokenWebTest 통합 테스트 3개 추가. (하) InviteController에서 familyId 빈 USER 토큰이면 NPE → 500(주체별 필수 클레임 결정 필요), LINK-04 · FAM-06 pushToken 빈 문자열 허용(AUTH-03은 1~255자), AUTH-03 주체 · platform 일치 검사 없음.
- [문서] NameRule · PlateNumberMasker를 common.text로 옮김(#103 · #104). 02 「common 순수 타입」 예외 목록(Role · PrincipalType · LatLng)에 common.text 규칙을 더할지, 04 구조(user domain 「이름 · 전화번호 규칙」) 갱신 필요.
- [사람 결정 2026-11-07 · auth 리뷰] (1) 연결 전 토큰으로 LINK-01을 부를 때 차량이 이미 연결돼 있으면 409 VEHICLE_ALREADY_LINKED. (2) FAM-04 대상이 GUARDIAN이면 요청자와 무관하게 403 FORBIDDEN(보호자 복구는 QR RELINK로만). (3) VehicleLinkCode에 발급 deviceId 컬럼, LINK-02는 요청 기기 deviceId로 비교 · 그 기기에만 토큰(GetLinkStatusUseCase 시그니처 변경). (4) FAM-06 수락 때 그 멤버의 살아 있는 이전 기기 모두 폐기. 미결(후속): LINK-04 · FAM-06 동시 처리 방지, 동의 저장 위치(pickup SIG-04 전), 번호판 입력 경로, LINK-05 뒤 남은 차량 기기 처리, DeviceReplacedEvent 위치(notification 전), 연결 전 토큰 만료 뒤 복구 · 발급 카운터 초기화 · 보호자 phone · VEHICLE_MOVING.
- [auth 리뷰 · 문서 · 하] 05 설계서 갱신(테이블 mode · tokenDeliveredAt · issuedDeviceId, LINK-04 name, 확인 필요 사항 해소분, 시그니처 MemberDeviceStatus · hashRefreshToken · TokenSubject, 구조 VehicleLinkCodeController · adapter/out/pickup · event). 04 설계서 196행(#64 문장)과 user.md 증거 이름 갱신. 02 의존 방향에 user → auth 추가, common.text 예외 목록. VehicleLinkCodeApiDoc의 VEHICLE_MOVING 422는 미구현 표시. 구조 검사 테스트(소스 텍스트 grep)는 02 「코드 리뷰로」와 결 다름. 429 retryAfterSeconds 없음, LINK-03 clientKey remoteAddr(프록시 설정 없음), consents type에 LOCATION · PUSH 허용, CheckActivePickupPort 임시 어댑터 user · auth 각각(pickup 때 교체).
- [#105 후속] 연결 전 토큰 LINK-01 거절은 VehicleInfo.familyId 유무만 본다(linked 미확인). user LINK-05는 unlinkedAt만 채우고 familyId를 남기지만, 연결 전 토큰은 대기 행(familyId null)에만 생기므로 지금 흐름에선 문제 없음. LinkCodeRules.actualModeOf(linked && familyId)와 기준을 맞출지 재리뷰 때 확인. 기존 코드 행(issued_device_id NULL)은 LINK-02에서 NOT_CODE_OWNER(5분 코드라 영향 작음). 05 설계서 테이블 issuedDeviceId · 확인 필요 사항 「LINK-02 발급 기기」 갱신 필요.
- [사람 결정 2026-11-07 · 재리뷰] 연결 전 토큰으로 발급한 코드는 issuedBeforeLink로 기록, LINK-03 · LINK-04 모두 NEW로 판정(RELINK 승인 409 LINK_MODE_MISMATCH, 그 사이 차량이 연결됐으면 NEW 승인은 CreateGuardian의 409 VEHICLE_ALREADY_LINKED). 발급 카운터 · 이전 코드 만료 · LINK-02 1초 제한은 기기(deviceId) 단위. 발급 기기 없는 옛 코드는 발급 때 함께 만료, 승인 때 카운터 초기화 건너뜀. 05 설계서 반영 필요: 테이블(issuedDeviceId · issuedBeforeLink · mode · tokenDeliveredAt), LINK-01 「같은 기기가 발급한 이전 ISSUED 코드」, 확인 필요 사항 「LINK-02 발급 기기」 · 「연속 5회 초기화」 기기 단위로.
- [auth 재리뷰 · 문서 · 중] 05 본문 갱신: 테이블에 issuedDeviceId · issuedBeforeLink · mode · tokenDeliveredAt, LINK-01 「발급 기기의 이전 ISSUED 코드」, getLinkStatus 시그니처 deviceId, LINK-04 요청 name, FAM-04 GUARDIAN 403 · LINK-01 409, 확인 필요 사항 05:276-278 결정 반영. 체크 #33 문장을 #111(기기 단위)에 맞추기
- [auth 재리뷰 · user · 중] user #64 증거 테스트 이름(user.md:72)이 UserModuleLinkAdaptersTest에 없음. 04:196 문장도 실제 AuthMemberDeviceAdapter와 다름 → 문장 · 증거 다시 맞추기
- [auth 재리뷰 · 문서 · 하] API 문서 갱신: VehicleLinkCodeApiDoc:19 · 29 · 34 · 46-48(LINK-01 409, 다른 기기, 연결 전 코드 NEW, CreateGuardian 409, 미구현 VEHICLE_MOVING 422 제거), InviteApiDoc:21(GUARDIAN 대상 403)
- [auth 재리뷰 · 하] 연결 전 코드 NEW 승인 중 차량이 연결되면 409 — auth 쪽 테스트 없음(CreateGuardianServiceTest:146만 있음)
- [auth 재리뷰 · 하] 다른 기기가 만든 ISSUED 코드가 시간이 지나도 ISSUED로 남아 loadIssuedCodes(vehicleId) 결과가 늘어남. (vehicle_id, status) 인덱스 없음(VehicleLinkCodeEntity:23)
- [auth 재리뷰 · 하] LinkCodeIssueCounter Map 정리 코드 없음(승인 때만 삭제). 기기를 새로 등록하면 5회 한도를 피할 수 있음(AUTH-02 횟수 제한 없음)
- [auth 재리뷰 · 문서 · 하] 02 의존 방향에 user → auth(AuthMemberDeviceAdapter → auth in 포트) 추가, 04 구조에 common.text 반영. 인증 없는 Idempotency 키 공간 후속 과제 줄 추가
- [사람 결정 11-07 · 후속] VIN만 알면 AUTH-02 → LINK-01 → LINK-04 NEW로 연결 안 된 남의 차량을 먼저 차지할 수 있음. 차량 소유 확인 방법(차량 화면에서만 시작, 제조사 인증 등)은 설계 전제 변경이라 후속 과제로 미룸. 05 확인 필요 사항에 추가할 것
- [#115 후속 · 하] version이 null인 Vehicle(6인자 restore로 만든 객체)을 저장하면 DB의 현재 version을 읽어 쓰므로 낙관적 잠금 충돌을 잡지 못함. 지금 src/main 호출 없음. 기존 dev DB는 version 컬럼이 null로 추가되므로 다시 만들거나 0으로 채워야 함
- [auth 재리뷰 · 문서] 체크 #33 문장(「그 차량의 이전 ISSUED 코드」)이 #111(같은 기기만)과 모순. 체크된 항목 문장과 증거 DisplayName은 직접 고치지 않음 → 사람 확인 뒤 #33 문장 · 테스트 DisplayName을 함께 바꿀 것
- [중간 리뷰 · #115 후속] UserPersistenceAdapter:83-88 versionToSave 대체 분기(version null이면 DB 값을 읽음)를 지우고, version 없는 Vehicle.restore · VehicleEntity 6인자 오버로드를 테스트 픽스처로 옮길지 결정 필요(테스트 시드 · 서비스 테스트 호출부 함께 수정)
- [중간 리뷰 · 문서] LINK-04 422 VEHICLE_MOVING: 설계서 05:74 · 113에는 있고 구현 · API 문서에는 없음(API 문서에서 제거함). 확인 필요 사항 「VEHICLE_MOVING 판단 근거」가 정해지면 설계서 · 구현 · 문서를 함께 맞출 것
- [auth 재리뷰2 · 하] 동시 요청이 500으로 끝나는 경우: 같은 새 VIN으로 AUTH-02 동시 요청(uk_vehicle_vin_linked 위반), LINK-05 동시 요청(OptimisticLockingFailure). GlobalExceptionHandler:93이 500 처리
- [auth 재리뷰2 · 하] RELINK · FAM-06 동시 처리 방지 없음(LinkCode · MemberInvite @Version 없음). 같은 코드로 RELINK가 동시에 오면 살아 있는 기기가 2개 생길 수 있음
- [auth 재리뷰2 · 하] LINK-05 해제 직전에 발급된 ISSUED 코드(최대 5분)는 LINK-03에서 NEW, LINK-04에서 409. UnlinkVehicleService:38-39가 코드를 만료시키지 않음. #113~#116 문장에 lane 태그 없음. [#115 후속] 설명 정정: version은 NOT NULL · 기본값 없음이라 행이 있는 dev DB에는 컬럼 추가가 실패함
- [auth 재리뷰2 · 문서 · 중] 05 본문 남은 곳: 구조 05:15(VehicleLinkCodeController, adapter/out/pickup), 시그니처 05:149 MemberDeviceStatus · 05:157 hashRefreshToken, 기기 상태 표 05:126-130에 「사용된 초대만 있음 → NONE」, 05:297 name 결정 반영, 05:74 · 113 VEHICLE_MOVING 미구현 표시
- [사람 결정 11-07] versionToSave 분기(UserPersistenceAdapter:83-88)는 후속 과제로 미룸. 지울 때 version 없는 Vehicle.restore · VehicleEntity 오버로드를 테스트 픽스처로 옮기고 호출부를 함께 고칠 것
- [문서 · 하] 04 확인 필요 사항 222행 「LINK-04 GUARDIAN의 name」이 아직 미정으로 남아 05(결정됨)와 다름. 04 의존 칸 8행에 LINK-05 차량 기기 폐기(auth 포트) 추가 필요
- [#118 후속 · 운영] prod는 ddl-auto validate라 vehicle.version을 SQL로 직접 넣어야 함: 컬럼 없으면 ALTER TABLE vehicle ADD COLUMN version BIGINT NOT NULL DEFAULT 0; 이미 있으면 SET DEFAULT 0 → null 행 0 채움 → SET NOT NULL. dev(update)도 기존 컬럼은 바뀌지 않으니 DB 재생성 또는 같은 SQL
- [중간 리뷰 #117~#120 · 중] 「살아 있는 기기 불러와 폐기 → 저장」 반복이 4곳(RevokeVehicleDevicesService · RevokeMemberDevicesService · AcceptInviteService:56-61 · ApproveLinkService:103-111). 폐기 in 포트를 revokeDevicesOf(OwnerType, long)로 합칠지는 사람 결정(테스트 2개 수정 필요). 메서드 이름 revokeDevicesOfVehicle · revokeDevicesOf 불일치
- [중간 리뷰 #117~#120 · 하] AuthInPortsTest:95가 RevokeVehicleDevicesUseCase 시그니처를 검사하지 않음. VehicleLinkCodeControllerTest:192 헬퍼 unlinkedVehicleToken은 실제로 「연결 전 토큰」이라 이름이 헷갈림. InviteControllerTest:175-177 중복 단언
- [auth 재리뷰3 · 하] user VehicleLinkApiDoc:13 LINK-05 설명에 「그 차량의 살아 있는 기기 모두 폐기, 차량 앱은 AUTH-02로 다시 등록」 추가. #117을 끝까지 잇는 통합 테스트(LINK-05 HTTP → device.revoked_at → AUTH-01 401) 없음(선택)
- [auth 재리뷰3 · 하] LINK-05로 폐기한 차량 기기의 access 토큰은 만료(PT30M)까지 이전 familyId로 유효 — 05:315 확인 필요 사항 행에 「LINK-05 포함」 추가 검토. UserModuleLinkAdaptersTest:53이 RevokeVehicleDevicesPort 빈을 보지 않음. TODO.md:19 · REVIEW.md:10의 재리뷰2 「상 1」은 실제 0건(기록 정정)
