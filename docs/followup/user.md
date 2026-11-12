# FOLLOWUP · 사용자 (user)

- 생성: 2026-11-11 11:38 KST · tdd-guard
- 상태: 진행 중 (체크 73/73 · 완료 기준 0/4)
- 테스트: ● GREEN · 2261 passed · 전체 · 90s · .\gradlew.bat test --console=plain
- 설계서: C:/ilink/docs/04_사용자-user.md

## 완료 항목
- [x] #1 [기반] Role은 GUARDIAN · CO_GUARDIAN · DELEGATE · TEACHER · CHILD 다섯 값을 갖는다
- [x] #2 [기반] JoinCallMode는 AUTO · GUARDIAN_CONFIRM 두 값을 갖는다
- [x] #3 [기반] 새로 만든 Family의 joinCallMode는 AUTO이고 settingsVersion은 0이다
- [x] #4 [기반] Family.changeCallMode를 부르면 joinCallMode가 바뀌고 settingsVersion이 1 오른다
- [x] #5 [기반] Vehicle.unlink를 부르면 unlinkedAt이 채워지고 vehicleId · vin은 그대로다
- [x] #6 [기반] unlinkedAt이 채워진 Vehicle은 연결 중이 아니다
- [x] #7 [기반] 이름 규칙은 0자와 21자 이름을 거절하고 1자와 20자 이름을 받는다
- [x] #8 [기반] 이름 규칙은 이모지나 제어문자가 든 이름을 거절한다
- [x] #9 [기반] 전화번호 규칙은 +821012345678을 받고 01012345678을 거절한다
- [x] #10 [기반] 번호판 마스킹은 「12가 3456」을 「12가 ****」로 바꾼다
- [x] #11 [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] #12 [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] #13 [기반] FamilyEntity · UserEntity · VehicleEntity가 BaseTimeEntity를 상속하고 UserEntity는 users 테이블에 매핑된다
- [x] #14 [기반] 테스트 시드로 가족 1개 · GUARDIAN 1명(김보호) · CHILD 2명(민준 · 서윤) · 연결된 차량 1대(아이오닉 5)를 만든다
- [x] #15 [persistence] Family · User · Vehicle을 저장한 뒤 다시 읽으면 도메인 객체의 값이 같다
- [x] #16 [persistence] users.phone 컬럼에는 평문이 아닌 암호문이 저장되고 읽으면 원래 번호로 돌아온다
- [x] #17 [persistence] 같은 전화번호를 두 번 저장하면 두 암호문이 서로 다르다
- [x] #18 [persistence] 서버가 뜨면 schema.sql의 VIN 부분 유니크 인덱스가 vehicle 테이블에 만들어진다
- [x] #19 [persistence] unlinkedAt이 빈 같은 VIN 차량 행을 두 개 저장하면 유니크 위반이 난다
- [x] #20 [persistence] 같은 VIN이라도 기존 행의 unlinkedAt이 채워져 있으면 새 행이 저장된다
- [x] #21 [persistence] loadLinkedVehicleByVin은 unlinkedAt이 빈 행만 찾는다
- [x] #22 [persistence] loadLinkedVehicleOfFamily는 그 가족의 연결 중인 차량을 찾는다
- [x] #23 [persistence] countNonChildMembers는 CHILD를 뺀 가족 멤버 수를 센다
- [x] #24 [서비스] 멤버 등록은 자녀 외 멤버가 이미 5명이면 MEMBER_LIMIT_EXCEEDED로 거절한다
- [x] #25 [서비스] 멤버 등록은 자녀 외 멤버가 5명이어도 CHILD는 등록한다
- [x] #26 [서비스] CO_GUARDIAN이 CO_GUARDIAN을 등록하려 하면 FORBIDDEN으로 거절한다
- [x] #27 [서비스] CHILD가 아닌 멤버를 phone 없이 등록하려 하면 거절한다
- [x] #28 [서비스] CHILD는 phone 없이 등록된다
- [x] #29 [서비스] 기기 없이 등록한 TEACHER의 deviceStatus는 NONE이다
- [x] #30 [서비스] 멤버 수정은 보낸 필드만 바꾸고 보내지 않은 필드는 그대로 둔다
- [x] #31 [서비스] CO_GUARDIAN이 멤버의 role을 바꾸려 하면 FORBIDDEN으로 거절한다
- [x] #32 [서비스] GUARDIAN이 자기 자신의 role을 바꾸려 하면 거절한다
- [x] #33 [서비스] 다른 가족의 userId를 수정하려 하면 NOT_FOUND로 거절한다
- [x] #34 [서비스] 멤버 해제는 GUARDIAN 자신을 대상으로 하면 CANNOT_REMOVE_GUARDIAN으로 거절한다
- [x] #35 [서비스] 멤버 해제는 진행 중 픽업에 묶인 멤버면 MEMBER_IN_ACTIVE_PICKUP으로 거절한다
- [x] #36 [서비스] 멤버 해제가 성공하면 RevokeMemberDevicesPort로 그 멤버의 기기 · 토큰 폐기를 요청한다
- [x] #37 [서비스] 가족 조회는 멤버별 deviceStatus를 LoadMemberDeviceStatusPort 결과로 채운다
- [x] #38 [서비스] 가족 조회는 연결 중인 차량이 없으면 vehicle을 null로 돌려준다
- [x] #39 [서비스] 차량 연결 해제는 진행 중 픽업이 있으면 ACTIVE_PICKUP_EXISTS로 거절한다
- [x] #40 [서비스] 차량 연결 해제가 성공하면 unlinkedAt만 채우고 행은 남긴다
- [x] #41 [서비스] 합류 안내 방식 변경은 settingsVersion을 1 올린 값을 돌려준다
- [x] #42 [서비스] createGuardian은 Family와 role GUARDIAN인 User를 만들고 Vehicle.familyId를 그 가족으로 채운다
- [x] #43 [서비스] registerVehicle은 familyId가 빈 Vehicle 행을 만들고 vehicleId를 돌려준다
- [x] #44 [web] GET /v1/families/me는 200과 familyId · joinCallMode · vehicle · members[]를 돌려준다
- [x] #45 [web] GET /v1/families/me를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] #46 [web] POST /v1/families/me/members는 201과 userId를 돌려준다
- [x] #47 [web] POST /v1/families/me/members에 21자 name을 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려준다
- [x] #48 [web] POST /v1/families/me/members가 한도를 넘으면 422 MEMBER_LIMIT_EXCEEDED를 돌려준다
- [x] #49 [web] PATCH /v1/families/me/members/{userId}는 200과 userId · role · name · phone · deviceStatus를 돌려준다
- [x] #50 [web] PATCH /v1/families/me/members/{userId}에 형식이 틀린 phone을 보내면 422를 돌려준다
- [x] #51 [web] DELETE /v1/families/me/members/{userId}는 204를 돌려준다
- [x] #52 [web] DELETE /v1/families/me/members/{userId}를 CO_GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] #53 [web] DELETE /v1/families/me/members/{userId}가 진행 중 픽업 멤버면 409 MEMBER_IN_ACTIVE_PICKUP을 돌려준다
- [x] #54 [web] DELETE /v1/vehicle-links/{vehicleId}는 204를 돌려준다
- [x] #55 [web] DELETE /v1/vehicle-links/{vehicleId}를 CO_GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] #56 [web] DELETE /v1/vehicle-links/{vehicleId}가 진행 중 픽업이 있으면 409 ACTIVE_PICKUP_EXISTS를 돌려준다
- [x] #57 [web] PUT /v1/families/me/call-mode는 200과 joinCallMode · settingsVersion을 돌려준다
- [x] #58 [web] PUT /v1/families/me/call-mode를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [x] #59 [web] Idempotency-Key는 POST /v1/families/me/members · DELETE /v1/families/me/members/{userId} · DELETE /v1/vehicle-links/{vehicleId}에만 요구된다
- [x] #60 [기반] Family.restore는 familyId · joinCallMode · settingsVersion을 그대로 가진 Family를 만든다
- [x] #61 [기반] User.create는 userId가 빈 User를 만들고 User.restore는 userId · familyId · role · name · phone을 그대로 가진 User를 만든다
- [x] #62 [기반] User.changeName · changePhone · changeRole은 그 필드만 바꾸고 나머지는 그대로 둔다
- [x] #63 [기반] Vehicle.register는 familyId · unlinkedAt이 빈 Vehicle을 만들고 Vehicle.linkTo는 familyId를 채운다
- [x] #64 [기반] auth · pickup 구현 전 임시 어댑터가 빈으로 등록된다: LoadMemberDeviceStatusPort는 모든 userId에 NONE, RevokeMemberDevicesPort는 아무것도 하지 않음, CheckActivePickupPort는 false
- [x] #65 [web] POST /v1/families/me/members에 role GUARDIAN을 보내면 422 VALIDATION_FAILED와 fields의 role을 돌려준다
- [x] #66 [web] 서비스가 필드를 지정해 던진 VALIDATION_FAILED는 422 응답의 fields에 그 field와 reason을 담는다
- [x] #67 [서비스] 자녀 외 멤버를 phone 없이 등록하거나 GUARDIAN이 자기 role을 바꾸려 할 때의 VALIDATION_FAILED는 fields에 phone · role을 담는다
- [x] #68 [서비스] 멤버 수정으로 phone 없는 CHILD를 자녀 외 role로 바꾸면 VALIDATION_FAILED로 거절하고 fields에 phone을 담는다
- [x] #69 [서비스] 멤버 수정으로 CHILD를 자녀 외 role로 바꿀 때 자녀 외 멤버가 이미 5명이면 MEMBER_LIMIT_EXCEEDED로 거절한다
- [x] #70 [web] PATCH /v1/families/me/members/{userId}에 role GUARDIAN을 보내면 422 VALIDATION_FAILED와 fields의 role을 돌려준다
- [x] #71 [서비스] createGuardian은 이미 가족에 연결됐거나 연결이 해제된 차량이면 409 VEHICLE_ALREADY_LINKED로 거절하고 아무것도 저장하지 않는다
- [x] #72 멤버 목록(findMembers · FAM-01 members[])은 userId 오름차순으로 돌려준다
- [x] #73 VEHICLE_ALREADY_LINKED는 common ErrorCode 하나로 정의되고 user · auth 모두 409 「이미 보호자가 연결된 차량입니다」로 응답한다

## 미완료 항목
- 없음

## 완료 기준 (공통 규칙 문서)
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 2026-11-11 11:38 KST · 도메인 리뷰어 · 결론 조건부 통과 · 상 0 · 중 6 · 하 19 · 리뷰 시점 체크 73/73
- 리뷰 시점 테스트: ● GREEN · 2261 passed · 전체 · 90s · .\gradlew.bat test --console=plain
  - 결론: 조건부 통과
  - user 쪽에서 동작을 바꿔야 하는 [상]은 없습니다. 지난 리뷰(2026-11-08 02:39)의 [중] 1건과 [하] 2건은 해소됐습니다. 다만 pickup · schedule · notification이 병합된 뒤 설계서 문장이 실제 코드와 어긋난 곳, 사람 결정이 필요한 곳이 [중] 5건 생겼습니다.
  - **지난 리뷰 지적 해소 여부**
  - [중] 03:100 ErrorCode 규칙 — 해소: 03:18 · :95 · :100 · :230 갱신, 02:169 추가
  - [하] 04 「항목 #201」 — 해소: 04 확인 필요 표가 「#32 · #67」
  - [하] FOLLOWUP 확인 필요 표 3행 — 해소: user.md:112가 04와 같음
  - [하] 소스 문자열 검사 테스트 — 남음
  - [하] 해제된 차량도 같은 문구 — 남음 (CreateGuardianService.java:35-36)
  - [하] CreateGuardianServiceTest가 #71 상황을 반복 — 남음
  - **발견 사항**
  - [중] #64 항목 문장과 증거 테스트가 지금 코드와 정반대 — 근거 아래 3가지 → 사람 결정(테스트 케이스 문장 수정) + note
  - 04:202 「임시 어댑터 NONE · 아무것도 안 함 · false」 문장과 증거 DisplayName(user.md:72)이 테스트에 없음
  - 지금 UserModuleLinkAdaptersTest:62 · :90 · :107은 auth · pickup에 실제로 연결됐는지를 확인함
  - 04:8 · 04:227 「pickup만 구현 전 임시 false」도 c08c65a 이후 사실과 다름(PickupActivityAdapter.java:13-22)
  - [중] SET-06 「다음 세션부터 적용된다」가 지켜지지 않음 — 04:102 · FamilyApiDoc.java:53은 다음 세션부터라고 하는데, pickup은 판정할 때마다 joinCallMode를 새로 읽음(CallEngine.java:64, SendCallService.java:41, 08:681 가정) → 진행 중인 세션에 바로 반영됨 → 사람 결정(세션에 스냅샷을 둘지, 04 · Swagger 문장을 고칠지)
  - [중] JoinCallMode · DeviceStatus가 모듈마다 따로 있고 pickup이 user.domain을 직접 import함 — 02:70 · :73 · :79와 어긋남 → 사람 결정(common 순수 타입으로 옮기고 02:70 예외 목록에 추가하거나, in 포트가 domain 타입을 내보내지 않게 함)
  - user in 포트 FamilyInfo.java · CallModeResult.java가 user.domain.JoinCallMode를 내보냄
  - pickup이 그대로 import: SessionCallView.java:3, CallEngine.java:20, SendCallService.java:14, SessionViewResponse.java:19
  - schedule은 자체 복사본을 둠(schedule/domain/JoinCallMode.java, 06:8)
  - DeviceStatus는 3벌: user · schedule · auth MemberDeviceStatus
  - [중] ACTIVE_PICKUP_EXISTS를 두 모듈이 따로 정의하고 문구도 다름 — UserErrors.java:26-28, AuthErrors.java:20-23. 02:169 「여러 모듈이 같은 뜻으로 쓰는 도메인 오류는 common ErrorCode에 한 번」과 어긋남 → 사람 결정(같은 뜻인지) 뒤 리팩터링
  - [중] FAM-07 실제 삭제로 고아 행이 생기는데 결정 기한이 지남 → 사람 결정(삭제 표시 방식, 자녀 일정 처리)
  - DB FK가 없음(schema.sql, @ManyToOne 없음). 그래서 04:220 「FK 때문에 지울 수 없음」은 사실과 다름
  - 삭제 뒤 남는 행: session_child.childUserId(SessionChildEntity.java:37), pickup_schedule, member_invite.userId(MemberInviteEntity.java:27), guardian_data_setting.userId
  - 「pickup · schedule 구현 전 결정」(FOLLOWUP 사람 결정 2026-11-07)이었으나 둘 다 이미 병합됨
  - [하] 설명 주석 1건 — PickupActivityAdapter.java:8 「pickup의 ActivePickupQueryUseCase에 위임한다」(02:78) → 리팩터링
  - [하] 번호판을 채우는 경로가 없음 — Vehicle.register가 plateNumber를 null로 만들고(Vehicle.java:32) 바꾸는 코드도 없음. 그래서 FAM-01 plateNumberMasked는 늘 null. 05:308에만 확인 필요로 있고 04 확인 필요 표에는 없음 → note(04 표에 추가)
  - [하] 「진행 중 픽업에 묶인 멤버」(04:87) 범위 — ActivePickupQueryService.isInActivePickup은 SessionChild만 봄. DELEGATE · TEACHER · CO_GUARDIAN 해제는 막히지 않음 → note(사람 확인)
  - [하] users.family_id · vehicle.family_id 조회 인덱스 없음 — UserEntity.java:28, VehicleEntity.java:28. pickup 서비스 20곳 이상이 loadMembers → findMembers를 불러 호출이 늘었음 → 항목 추가 검토
  - [하] 동시 요청 처리 없음 → note
  - 5명 한도에 잠금이 없음(NonChildMemberLimit.java:13)
  - Family에 @Version이 없음
  - 같은 차량을 동시에 연결 해제하면 OptimisticLockingFailureException이 매핑 없이 500(UnlinkVehicleService.java:41)
  - [하] web 테스트가 없는 오류 행 — FAM-02 403, FAM-03 403 · 자기 role 422 · MEMBER_LIMIT_EXCEEDED, FAM-07 422 · 404, LINK-05 404. 서비스 테스트만 있음 → 항목 추가(선택)
  - [하] 03 문서에 팩토리 3개 빠짐 — 팩토리 표(03:93-98)와 시그니처(03:231-237)에 invalidField · tooManyRequests(ErrorCode,int) · stateVersionConflict 없음 → note
  - [하] 04:21 domain에 「이름 규칙」이 있다고 적혔지만 실제 위치는 common/text/NameRule.java → note
  - [하] README에 PHONE_ENCRYPTION_KEY 없음 — 환경 변수 표(README:32-38), 실행 명령(:57 · :63 · :74)에 없고 :85 배포 Secret에만 있음. :42 「해당 도메인을 만들 때 더한다」도 그대로. 이 키가 없으면 서버가 뜨지 않음 → note
  - [하] 다른 모듈 테스트가 user 내부 클래스를 씀 — AuthSeed.java:39 `new VehicleEntity(...)`, VehicleLinkCodeControllerConcurrencyTest.java:8-9가 VehicleEntity · LoadVehiclePort를 씀 → note
  - [하] notification이 멤버마다 findMember를 따로 부름(NotificationMemberNameAdapter.java:59-62). LoadMemberUseCase에 여러 id를 한 번에 찾는 메서드가 없음 → note
  - [하] 소스 문자열 검사 테스트 — ErrorCodeTest.java:46-58, NameRuleTest.java:124-125, PlateNumberMaskerTest.java:61-63. 02:80 「ArchUnit 같은 자동 검사는 쓰지 않는다」와 결이 다름 → 사람 결정
  - [하] 작은 정리 거리 → 리팩터링(선택)
  - reason 문구가 다름: RegisterMemberService.java:33 「국제 형식 전화번호가 필요합니다」 vs UpdateMemberService.java:59 「전화번호가 필요합니다」
  - 한도 5가 두 곳에 있음: UserErrors.java:13 문구, NonChildMemberLimit.java:7 상수
  - 제약 이름 MemberRole → ValidMemberRole
  - versionToSave 폴백(UserPersistenceAdapter.java:83-88)
  - [하] 진행 기록이 실제와 다름 — FOLLOWUP user.md:4-6이 C:/pickup 경로 · 996 passed · 완료 기준 4/4(브리핑은 0/4). user.md:72 #64 증거는 존재하지 않음. 후속 과제에 해소된 메모가 정리되지 않음 → note
  - 완료 기준: C1 사람 확인 필요 — 체크 73/73이고 전체 GREEN 2261. 다만 #64 증거 DisplayName이 없고 항목 문장이 지금 동작과 반대(UserModuleLinkAdaptersTest:107) / C2 사람 확인 필요 — user의 6개 API(경로 · 권한 · Idempotency-Key · 오류 코드) · 시그니처 · 정렬 · 암호화는 설계서대로. SET-06 「다음 세션부터 적용」만 pickup 쪽 동작과 어긋남(CallEngine.java:64) / C3 미충족 — [중] 공통 규칙 2건(ACTIVE_PICKUP_EXISTS 중복 02:169, JoinCallMode 노출 · 중복 02:70 · :79), [하] 주석 1건(PickupActivityAdapter.java:8…
  - 후속 과제:
  - 「연결 중」 정의 — 해소됨 (04:56 = 05 LINK-03)
  - #64 임시 어댑터를 실제 연동으로 교체 — 해소됨 (AuthMemberDeviceAdapter, PickupActivityAdapter). #64 문장은 남음
  - vehicle model · plateNumber NULL — 해소됨 (04:51-52). 번호판을 채울 시점 — 설계 결정 필요
  - family_id DB FK — 설계 결정 필요 (04:39 · :49 「FK → Family」, schema.sql에 없음)
  - UserSeed를 IntegrationTestSupport가 @Import — 해소됨 (IntegrationTestSupport.java:11). 02:213 픽스처 문구 — 남음
  - README PHONE_ENCRYPTION_KEY · 키 교체 방법 — 남음
  - VIN 인덱스 이름 — 해소됨 (uk_vehicle_vin_linked)
  - persistence 중간 리뷰: (1) 실제 삭제 — 설계 결정 필요(기한 지남) (2) 가족당 차량 1대 — 남음(LINK-04 NEW만 연결해 구조상 1대, DB 제약 없음) (3) FK — 설계 결정 필요 (4) 정렬 — 해소됨 (#72). [하] 항목들 — 남음
  - [설계 확인 · service] createGuardian 이름 · 연결된 차량 · LoadVehicle/Member/FamilyUseCase 구현 — 해소됨. 중복 VIN 500 — auth가 먼저 막음(RegisterVehicleDeviceService.java:47-52), 동시 요청은 남음
  - [service 리뷰] FAM-03 role 재확인 · fields — 해소됨 (#66~#70). service가 이름 · 전화번호 형식을 직접 확인하지 않음 — 남음
  - [web 리뷰] role 제한 · phone fields — 해소됨. familyId null 500 — 해소됨 (auth가 USER 토큰에 늘 familyId를 넣음: AcceptInviteService.java:67, ApproveLinkService.java:119, RefreshTokenService.java:50). IdempotencyKeyFilter 순서 — 남음(이번에 재현하지 않음). PATCH null과 미전송 구분 — 남음
  - [사람 결정 2026-11-07] — 반영됨. 삭제 방식 — 설계 결정 필요(기한 지남)
  - [도메인 리뷰 하] 동시성 · 인덱스 · web 오류 테스트 · 끝까지 잇는 테스트 — 남음. NameRule 주석 · 커밋 안 된 변경 — 해소됨
  - [문서] 03 invalidField — 남음
  - [재리뷰 · 문서 갱신] 04 사람 결정 반영 · 한도 상수 — 해소됨. MemberRole 이름 · 문구 통일 · CO_GUARDIAN PATCH role GUARDIAN이 403이 아니라 422 — 남음
  - [결정 · 사람, 재리뷰 note] 정렬 · VEHICLE_ALREADY_LINKED · 주석 — 해소됨. #64 문구 · README · 동시 해제 500 · versionToSave · FAM-07 일정 행 · 02 픽스처 · 03 invalidField · FOLLOWUP 낡음 — 남음
  - user #73 02 문서 결정 — 해소됨 (02:169). 해제된 차량 같은 문구 — 남음
  - user #64 문장 · README · UserErrors 숫자 5 — 남음. PickupActivityAdapter 이름 — 해소됨(이제 임시가 아님)
  - [공통] 소스 문자열 검사 테스트 — 설계 결정 필요. CreateGuardianServiceTest 반복 — 남음
  - 테스트: `mcp__tdd-guard__tests` scope "all"(`.\gradlew.bat test --console=plain`) → GREEN 2261 passed. Gradle UP-TO-DATE라 이전 보고서 결과를 재사용했고, git status는 clean입니다. 그 밖에는 git log와 grep 같은 읽기 명령만 썼습니다.
  - 참고 파일: C:/ilink/docs/04_사용자-user.md, C:/ilink/docs/followup/user.md, C:/ilink/src/main/java/com/example/pickupservice/user/adapter/out/pickup/PickupActivityAdapter.java, C:/ilink/src/main/java/com/example/pickupservice/user/application/service/UserErrors.java, C:/ilink/src/main/java/com/example/pickupservice/user/application/port/in/FamilyInfo.java, C:/ilink/src/main/java/com/example/pickupservic…

## 확인 필요 사항 (설계서)
| 항목 | 현재 가정 |
|---|---|
| 「자녀 외 멤버 최대 5명」에 GUARDIAN 자신이 들어가는지 | 소스에 없음. GUARDIAN 포함 CHILD가 아닌 멤버 전체를 센다고 가정 |
| GUARDIAN이 자기 role을 바꾸려 할 때 오류 코드 | 결정됨: 422 VALIDATION_FAILED(fields role), 항목 #32 · #67 |
| 전화번호 국제 형식 범위 | 예시가 +8210…뿐. +로 시작하는 국제 형식(E.164)을 받는다고 가정 |
| 멤버 해제 때 User 행 처리 | 소스에 없음. SessionChild · PickupSchedule이 User를 FK로 가리켜 지난 기록이 있는 멤버는 행을 지울 수 없음. 삭제 대신 표시만 하는 방식이 필요한지 정해야 함 |
| 멤버 해제 때 그 자녀의 일정 · 장소 처리 | 소스에 없음. 그대로 둔다고 가정 |
| LINK-05를 차량 앱이 부를 수 있는지 | 4.3 표의 사용 화면에 「차량 설정 요약 화면」이 있으나 4.14 호출은 GUARDIAN뿐. 4.14대로 USER 주체 GUARDIAN만 허용 |
| LINK-05 때 차량 기기 토큰 폐기 | 결정됨(사람 결정 2026-11-07). 해제하면 그 차량의 살아 있는 기기를 모두 폐기한다. 해제된 차량 앱은 AUTH-02로 다시 등록한다. 포트도 결정됨: RevokeVehicleDevicesPort → auth RevokeVehicleDevicesUseCase |
| settingsVersion을 올리는 변경의 범위 | SET-06만 명시됨. 일정(SET-02 · 03) · 장소(SPOT-02 · 03) · 멤버 변경도 올리는지 정해야 함(SET-08 충돌 판단에 영향) |
| 번호판 마스킹 규칙 | 예시 「12가 ****」뿐. 마지막 숫자 4자리를 *로 바꾼다고 가정 |
| user ↔ auth 서로 부름 | auth는 User 생성을 user에 요청하고, user는 기기 상태 · 폐기를 auth에 요청. user 쪽은 out 포트로 둔다. auth 쪽 임시 어댑터(LoadMemberDeviceStatusPort 기기 상태 · RevokeMemberDevicesPort 기기 폐기)는 auth 연동으로 교체됨 |
| 진행 중 픽업 확인(FAM-07 · LINK-05)을 pickup in 포트로 부르는 것 | pickup이 user보다 뒤에 구현되므로 out 포트 + 테스트 대역으로 먼저 검증. pickup 쪽 CheckActivePickupPort만 아직 임시 어댑터(늘 false). pickup 구현 때 교체해야 함 |
| 포트 · 유스케이스 이름 | 소스 2.2 예시(StartPickupUseCase, LoadSessionPort)의 이름 규칙을 따른 제안 |
| LINK-04 NEW로 만드는 GUARDIAN의 name · phone | name은 결정됨(사람 결정 2026-11-07). LINK-04 요청 name, mode NEW일 때 필수(05_인증-auth.md와 같음). phone은 미정. 요청에 없어 null로 만들고 있어 「자녀 외 phone 필수」 규칙과 어긋남. 보호자 전화번호를 받을 곳을 정해야 함 |
| Role enum 위치 | 해소: 02에 「common 순수 타입(Role · PrincipalType · LatLng)은 domain에서 써도 된다」 예외를 둠. user/domain은 common.security.Role을 그대로 쓰고 Role enum을 따로 만들지 않음(사람 결정 2026-11-07, 03 확인 필요 사항) |

## 다음 단계
- 커밋 · 푸시: /tdd commit (기능별 분할 커밋) · /tdd push (커밋 후 기능 브랜치 푸시)
- 다음 도메인: 인증 (05_인증-auth.md). 시작: /tdd domain auth

## 후속 과제
- 「연결 중」 정의: 지금은 unlinkedAt IS NULL만 본다(VIN 부분 유니크 인덱스와 같음). 그래서 AUTH-02로 등록만 되고 가족 승인 전(familyId null)인 차량도 isLinked true · loadLinkedVehicleByVin 대상이 된다. 승인 전 차량을 따로 볼지 사람 확인 필요.
- [설계 확인] #64 임시 어댑터(기기 상태 NONE · 폐기 no-op · 진행 중 픽업 false)는 auth · pickup 도메인 구현 때 실제 in 포트 호출 어댑터로 바꿔야 함. 바꾸기 전 운영 배포 금지.
- [설계 확인] vehicle.plate_number · model의 NULL 여부가 설계서에 없음. registerVehicle(vin, model)은 plateNumber를 받지 않아 plate_number는 NULL 가능이어야 함 → 설계서 테이블에 「NULL」 표시 반영 필요. 번호판은 언제 · 어디서 채우는지도 미정.
- [설계 확인] users.family_id · vehicle.family_id에 DB FK 제약이 없음(엔티티는 Long 칸만). 설계서 「FK → Family」를 DB 제약(schema.sql)으로 걸지, 연관 매핑으로 할지 결정 필요.
- [리뷰 확인] UserSeed를 user/fixture에 두고 공통 IntegrationTestSupport가 @Import함 → common이 user를 참조. 02 「테스트 픽스처는 common에 한 번만」과 맞는지, 도메인 시드는 각 테스트가 @Import하게 할지 결정 필요.
- [운영] dev · prod 실행에 환경 변수 PHONE_ENCRYPTION_KEY(Base64 32바이트)가 필수 — 없으면 서버가 뜨지 않음. README 실행 방법에 추가 필요. 키 교체(재암호화) 방법은 미정.
- [리뷰] VIN 인덱스 이름 ux_vehicle_vin_linked가 기존 schema.sql 관례(uk_ 접두어)와 다름 → uk_vehicle_vin_linked로 맞출지 lane 병합 리뷰에서 정리.
- [persistence 중간 리뷰 · 설계 확인] (1) 멤버 해제 deleteUser가 실제 삭제 — 지난 기록 FK 시 실패, 삭제 표시 방식 결정 필요 (2) 가족당 연결 중 차량 1대 제한 여부(없으면 loadLinkedVehicleOfFamily가 2대일 때 예외) — family_id WHERE unlinked_at IS NULL 부분 유니크 검토 (3) users/vehicle.family_id DB FK 여부 (4) FAM-01 members[] 정렬 기준 미정. [하] 갱신 save 시 메모리 엔티티 createdAt null(동작 영향 없음), 테스트 컨텍스트별 전화번호 키가 달라 다른 컨텍스트가 남긴 users 행은 복호화 실패 가능, randomVin 헬퍼 중복. dev DB에 옛 인덱스 ux_vehicle_vin_linked가 남으면 DROP 필요.
- [설계 확인 · service] createGuardian: LINK-04에 이름이 없는데 users.name NOT NULL(이름 받을 곳 결정), 이미 다른 가족에 연결된 차량 · 해제된 차량에 createGuardian 시 처리 미정. registerVehicle: 연결 중인 같은 VIN이면 DB 유니크 위반(500) — 서비스에서 먼저 막을지 미정. 이미 해제된 차량 재해제는 404로 가정. 대상 role GUARDIAN 해제는 요청자와 무관하게 422 CANNOT_REMOVE_GUARDIAN으로 가정. LoadVehicleUseCase · LoadMemberUseCase · LoadFamilyUseCase는 체크리스트 항목이 없어 구현 안 됨(다른 모듈이 쓸 때 항목 추가 필요).
- [service 리뷰 · 동작 보강 후보] (중) FAM-03 role 변경 시 GUARDIAN으로 승격 · 자녀 외 5명 한도 · phone 필수를 다시 확인하지 않음 (중) service가 name · phone 형식을 직접 확인하지 않음(web @Valid에만 의존, createGuardian 포함) (중) CHILD가 보낸 phone 형식 미확인 (하) FAM-02 role=GUARDIAN 등록을 service에서 막지 않음(web DTO 확인 필요) (하) service의 VALIDATION_FAILED에 fields 없음 (하) createGuardian이 이미 연결 · 해제된 차량을 덮어씀.
- [web 리뷰] (중) FAM-03 role 변경 값 제한 없음(GUARDIAN 승격 · CHILD↔비자녀 전환 시 phone 필수 · 5명 한도 우회) — 설계 결정 필요 (중) 비자녀 phone 누락 422에 fields 없음 — DTO 클래스 단위 제약으로 옮길지 (하) IdempotencyKeyFilter가 @PreAuthorize보다 먼저 돌아 권한 없는 사용자도 키 없으면 403 대신 422(common) (하) principal.familyId() null이면 500 — auth 토큰 발급 시 familyId 보장 필요 (하) PATCH는 null과 미전송을 구분 못 해 phone 삭제 불가.
- [사람 결정 2026-11-07] FAM-03 role 변경: GUARDIAN 승격 422(fields role) · CHILD→자녀 외 전환 시 5명 한도 422 MEMBER_LIMIT_EXCEEDED · phone 없으면 422 VALIDATION_FAILED. 서비스 필드 오류는 common BusinessException에 fields를 더해 담음. createGuardian은 이미 연결 · 해제된 차량이면 409 VEHICLE_ALREADY_LINKED. 멤버 해제 실제 삭제 vs 삭제 표시는 pickup · schedule 구현 전에 결정(지금은 실제 삭제 유지). 미결: common IntegrationTestSupport의 UserSeed @Import(auth 시작 전 결정), FAM-01 members[] 정렬 기준.
- [도메인 리뷰 하] 5명 한도 동시 요청 시 6명 가능(잠금 없음). Family @Version 없음 → SET-06 동시 요청 시 settingsVersion 증가 유실(SET-08 설계 때). users · vehicle.family_id 조회 인덱스 없음. 설계서 테이블 갱신 필요(vehicle.model · plate_number NULL, family 기본값은 도메인). VIN 길이 · 중복 VIN 500은 auth AUTH-02 때 항목 추가. 일부 오류 행(FAM-02 403, FAM-03 403 · 자기 role 422, FAM-07 422, 404)은 web 테스트 없음. 컨트롤러→서비스→DB 끝까지 잇는 테스트 없음(FAM-02 · FAM-07 1개씩 검토). 주석 규칙(NameRule:32). 메인에 커밋 안 된 변경 있음 → /tdd commit 필요.
- [문서] 03_공유모듈-shared.md의 BusinessException 팩토리 표(93행~)와 시그니처 블록(230~235행)에 invalidField(String field, String reason)(#66, user 도메인에서 추가) 반영 필요.
- [재리뷰 · 문서 갱신] 04 설계서 API 명세 FAM-03 오류 열 · 「멤버」 핵심 로직 · 「차량」 createGuardian 줄에 사람 결정(2026-11-07) 반영 필요: role GUARDIAN 422(fields role) · CHILD→자녀 외 전환 시 422 MEMBER_LIMIT_EXCEEDED · phone 필수 422(fields phone) · 409 VEHICLE_ALREADY_LINKED. 리팩터링 후보: FamilyApiDoc FAM-03 422 설명, 등록 · 수정 phone 확인 방식(PhoneRule) · reason 문구 통일, 제약 이름(MemberRole → ValidMemberRole), 한도 상수 5 중복. 사람 확인: CO_GUARDIAN PATCH role GUARDIAN이 403 대신 422(검증 순서), 「연결 중」 정의 05(familyId 있고 unlinkedAt 빈 행)와 불일치 — auth 시작 전 결정. VEHICLE_ALREADY_LINKED 코드 공유 위치는 auth 때.
- [결정 · 사람, user 재리뷰 뒤] 멤버 목록 userId 오름차순, VEHICLE_ALREADY_LINKED common 하나로(문구 「이미 보호자가 연결된 차량입니다」), 설명 주석 2건 삭제. [재리뷰 note] 04 #64 문구 · 증거(auth 연동, UserModuleLinkAdaptersTest:64 · :92 · :109), 04 API 표 FAM-03 · 멤버 핵심 로직 · createGuardian 줄에 사람 결정 반영, README에 PHONE_ENCRYPTION_KEY, 같은 차량 동시 연결 해제 500(404/409 결정 필요), versionToSave 폴백이 잠금을 끔(테스트 전용 6인자 restore), FAM-07 실제 삭제 시 pickup_schedule 행 남음(pickup 전 결정), 02:211 픽스처 규칙 갱신, 03 invalidField, 04 vehicle model · plateNumber NULL, FOLLOWUP user.md 낡음
- user #73: common ErrorCode에 도메인 오류 VEHICLE_ALREADY_LINKED가 들어감 — 02 공통 오류 표(L171~, 모든 API 공통 오류)에 「여러 모듈이 쓰는 도메인 오류도 ErrorCode에 둔다」를 적을지 문서 결정 필요. 연결 해제된 차량도 같은 문구로 응답(뜻이 좁아짐)
- user: #64 항목 문장(04:198 「NONE · 아무것도 안 함」)과 증거가 지금 auth 연동 코드와 다름 — 테스트 케이스 절이라 사람이 문장 수정 여부 결정. README 실행 방법에 PHONE_ENCRYPTION_KEY 추가 필요(docs 밖). UserErrors.java:13 「최대 5명입니다」 문구에 숫자 5 따로 있음. PickupActivityAdapter가 pickup 전 임시라는 뜻이 이름에 없음
- [공통 · 사람 확인] 소스 문자열 검사 테스트가 02:80 「ArchUnit 같은 자동 검사는 쓰지 않는다」와 결이 다름: user ErrorCodeTest.vehicleAlreadyLinkedIsNotRedefinedOutsideCommonErrorCode, schedule ScheduleModuleImportTest · PickupScheduleTest/InvalidScheduleExceptionTest의 import 검사. 남길지(02 규칙에 예외로 적을지) 결정 필요. CreateGuardianServiceTest 새 테스트가 #71 상황 반복(선택 리팩터링)
