# FOLLOWUP · 사용자 (user)

- 생성: 2026-11-14 01:10 KST · tdd-guard
- 상태: 완료 (체크 87/87 · 완료 기준 4/4)
- 테스트: ● GREEN · 2773 passed · 전체 · 246s · .\gradlew.bat test --console=plain
- 설계서: C:/pw/user/docs/04_사용자-user.md

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
- [x] #64 [기반] auth · pickup 연동 어댑터가 빈으로 등록된다: LoadMemberDeviceStatusPort는 auth GetMemberDeviceStatusUseCase 결과를, RevokeMemberDevicesPort는 auth RevokeMemberDevicesUseCase를, CheckActivePickupPort는 pickup ActivePickupQueryUseCase 결과를 쓴다(사람 결정 2026-11-13)
- [x] #65 [web] POST /v1/families/me/members에 role GUARDIAN을 보내면 422 VALIDATION_FAILED와 fields의 role을 돌려준다
- [x] #66 [web] 서비스가 필드를 지정해 던진 VALIDATION_FAILED는 422 응답의 fields에 그 field와 reason을 담는다
- [x] #67 [서비스] 자녀 외 멤버를 phone 없이 등록하거나 GUARDIAN이 자기 role을 바꾸려 할 때의 VALIDATION_FAILED는 fields에 phone · role을 담는다
- [x] #68 [서비스] 멤버 수정으로 phone 없는 CHILD를 자녀 외 role로 바꾸면 VALIDATION_FAILED로 거절하고 fields에 phone을 담는다
- [x] #69 [서비스] 멤버 수정으로 CHILD를 자녀 외 role로 바꿀 때 자녀 외 멤버가 이미 5명이면 MEMBER_LIMIT_EXCEEDED로 거절한다
- [x] #70 [web] PATCH /v1/families/me/members/{userId}에 role GUARDIAN을 보내면 422 VALIDATION_FAILED와 fields의 role을 돌려준다
- [x] #71 [서비스] createGuardian은 이미 가족에 연결됐거나 연결이 해제된 차량이면 409 VEHICLE_ALREADY_LINKED로 거절하고 아무것도 저장하지 않는다
- [x] #72 멤버 목록(findMembers · FAM-01 members[])은 userId 오름차순으로 돌려준다
- [x] #73 VEHICLE_ALREADY_LINKED는 common ErrorCode 하나로 정의되고 user · auth 모두 409 「이미 보호자가 연결된 차량입니다」로 응답한다
- [x] #74 [기반] Family는 childLocationEnabled(기본 false)를 갖고 changeChildLocation(enabled)을 부르면 값이 바뀌고 settingsVersion이 1 오른다
- [x] #75 [persistence] family 테이블의 child_location_enabled(NOT NULL, 기본 false)를 저장하고 읽는다
- [x] #76 [서비스] 자녀 위치 사용 변경(SET-07)은 GUARDIAN · CO_GUARDIAN의 가족 childLocationEnabled를 바꾸고 settingsVersion을 1 올려 돌려준다
- [x] #77 [web] PUT /v1/families/me/child-location은 200과 childLocationEnabled · settingsVersion을 돌려주고, enabled가 없으면 422 VALIDATION_FAILED, CHILD · DELEGATE · TEACHER 토큰이면 403 FORBIDDEN을 돌려준다
- [x] #78 [web] GET /v1/families/me와 LoadFamilyUseCase.findFamily의 FamilyInfo는 childLocationEnabled를 함께 돌려준다
- [x] #79 [서비스] 가족 조회(FAM-01)는 저장된 Family의 childLocationEnabled를 FamilyResult에 그대로 옮긴다(true면 true)
- [x] #80 [서비스] 멤버 해제(FAM-07)는 User 행을 지우지 않고 removedAt을 채운다(삭제 표시, D7 사람 결정 2026-11-13) ← RemoveMemberServiceTest.멤버 해제는 User 행을 지우지 않고 removedAt을 지금 시각으로 채워 저장하며 다른 값은 그대로 둔다, RemoveMemberServiceTest.이미 해제된(removedAt이 채워진) 멤버를 다시 해제하면 NOT_FOUND로 거절하고 저장 · 폐기를 하지 않는다
- [x] #81 [persistence] 가족 멤버 조회(loadUsers · 다른 모듈용 멤버 조회)는 removedAt이 있는 멤버를 빼고 돌려준다 ← UserPersistenceAdapterTest.loadUsers는 removedAt이 있는 멤버를 빼고 그 가족의 해제되지 않은 멤버만 돌려준다, RemoveMemberServiceIntegrationTest.해제된 멤버는 다른 모듈용 findMembers에서 빠지고 findMember(userId)로는 이름 · role이 그대로 읽힌다
- [x] #82 [서비스] 해제된 멤버는 가족 인원 한도 계산과 가족 조회(FAM-01) members에서 빠진다 ← UserPersistenceAdapterTest.countNonChildMembers는 removedAt이 있는 자녀 외 멤버를 세지 않는다, RemoveMemberServiceIntegrationTest.자녀 외 멤버 5명 중 1명을 해제하면 한도에 자리가 생겨 새 자녀 외 멤버 등록이 성공한다, RemoveMemberServiceIntegrationTest.해제된 멤버는 가족 조회(FAM-01) members에 나오지 않는다
- [x] #83 [persistence] removedAt이 있는 User도 userId로 단건 조회하면 그대로 읽혀 지난 픽업 기록의 이름을 보여줄 수 있다 ← UserPersistenceAdapterTest.removedAt이 있는 User도 loadUser(userId)로 읽으면 이름 · role · removedAt이 그대로 나온다
- [x] #84 [web] 자녀 등록(FAM-02) · 멤버 수정(FAM-03)은 선택 값 grade(1~20자, 예: 「중학교 2학년」)를 받아 저장하고 가족 조회(FAM-01) members[]에 grade를 담는다. 21자 이상이면 422 VALIDATION_FAILED(최종디자인 11/13 픽업일정등록1) ← FamilyControllerTest.GET /v1/families/me는 members[]마다 grade를 담고 grade가 없는 멤버는 "grade":null로 돌려준다, FamilyControllerTest.POST /v1/families/me/members에 grade를 보내면 RegisterMemberCommand의 grade로 유스케이스에 넘긴다, FamilyControllerTest.PATCH /v1/families/me/members/{userId}에 grade를 보내면 UpdateMemberCommand의 grade로 넘기고 응답에 grade를 담는다, UserPersistenceAdapterTest.User의 grade에 값을 넣어 저장하면 users.grade에 남고 loadUser · loadUsers로 다시 읽었을 때 같은 값이다
- [x] #85 [서비스] 다른 모듈용 멤버 조회 in 포트는 멤버마다 profileImageUrl(사진 키 서명 URL 또는 기본 아바타 URL, 저장소 설정 없으면 null)을 함께 돌려준다(차량 · 자녀 화면 아바타용) ← LoadMemberServiceTest.findMember는 사진 키가 있는 멤버에게 그 키의 서명 URL을 profileImageUrl로 준다, LoadMemberServiceTest.findMember는 사진 키가 없는 멤버에게 기본 아바타 URL을 profileImageUrl로 준다, LoadMemberServiceTest.findMember는 저장소 설정이 없으면 profileImageUrl을 null로 준다, LoadMemberServiceTest.findMembers는 멤버마다 사진 키 서명 URL 또는 기본 아바타 URL을 profileImageUrl로 준다
- [x] #86 [서비스] 다른 모듈용 멤버 조회(findMember · findMembers)의 MemberInfo는 removed(해제 여부)를 담는다. findMember는 해제된 멤버도 removed true로 돌려준다(사람 결정 2026-11-14) ← LoadMemberServiceTest.findMember는 해제된 멤버도 돌려주고 removed를 true로 담는다, LoadMemberServiceTest.findMember는 해제되지 않은 멤버의 removed를 false로 담는다
- [x] #87 [서비스] 멤버 수정(FAM-03)은 해제된(removedAt이 있는) 멤버면 404 NOT_FOUND로 거절하고 저장하지 않는다(사람 결정 2026-11-14) ← UpdateMemberServiceTest.이미 해제된(removedAt이 있는) 멤버를 수정하려 하면 NOT_FOUND로 거절하고 저장하지 않는다

## 미완료 항목
- 없음

## 완료 기준 (공통 규칙 문서)
- [x] C1 설계서의 테스트 케이스가 모두 GREEN
- [x] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [x] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [x] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 2026-11-14 01:05 KST · 도메인 리뷰어 · 결론 조건부 통과 · 상 0 · 중 2 · 하 12 · 리뷰 시점 체크 87/87
- 리뷰 시점 테스트: ● GREEN · 2773 passed · 전체 · 244s · .\gradlew.bat test --console=plain
  - 결론: 조건부 통과
  - user 도메인에는 동작을 바꿔야 하는 [상]이 없습니다. 다만 해제된 멤버를 막는 처리가 auth · schedule · pickup에 아직 없어서, 그 항목들이 처리될 때까지 운영에 배포하면 안 됩니다.
  - [중] 해제된 멤버가 아직 쓰지 않은 초대를 수락하면 새 기기와 토큰을 받을 수 있음 — main에서 MemberInfo.removed()를 쓰는 곳 0건, AcceptInviteService.java:45 · IssueInviteService.java:61 · GetInviteService.java:30이 findMember 결과를 그대로 씀. 05 #125 · #126 미체크 → note(auth에서 먼저 처리, 그 전 배포 금지)
  - [중] schedule · pickup이 findMember로 해제된 자녀를 통과시킴 — FamilyChildGuard.java:15, StartPickupService.java:104. 06 #84 · 08 #276 미체크. 설정 요약(SET-07, GetSetupSummaryService.java:42)에서 해제된 자녀를 어떻게 다룰지는 06 항목에 없음 → 사람 결정
  - [하] 스웨거 SET-06 설명 「다음 세션부터 적용된다」가 설계서와 반대 — FamilyApiDoc.java:74 → 리팩터링
  - [하] 스웨거에 이번 변경분이 빠짐: FAM-02 · FAM-03 grade(1~20자), FAM-01 해제 멤버 제외, FAM-03 · FAM-07 해제된 멤버 404 — FamilyApiDoc.java:27 · :33 · :56 · :64 · :69 → 리팩터링
  - [하] 주석 규칙(02:80) 위반 2건 — ProfileImageUrls.java:13 Javadoc, PickupActivityAdapter.java:8 → 리팩터링
  - [하] 테스트에서만 쓰는 보조 생성자 · with 메서드가 main에 늘어남 — MemberInfo 5인자, MemberResult 6인자 · withGrade, UpdateMemberCommand 7인자 · withGrade, User.restore 5 · 6 · 7인자 → note
  - [하] SaveUserPort.deleteUser를 main에서 부르는 곳이 없고 어댑터 테스트도 없음 → 사람 결정(지울지)
  - [하] prod DDL 문장이 없음. 제안: ALTER TABLE users ADD COLUMN removed_at TIMESTAMPTZ NULL, ADD COLUMN grade VARCHAR(20) NULL; → note
  - [하] 해제된 CO_GUARDIAN · DELEGATE도 access token이 살아 있는 30분 동안 FAM-01~03 · SET-06 · SET-11을 호출할 수 있음 — access-ttl PT30M, 요청마다 폐기 확인 없음 → note(범위를 넓혀 기록)
  - [하] findMember · findMembers가 멤버마다 서명 URL을 만듦. 이름만 쓰는 pickup · schedule 호출도 서명 비용을 내고, 서명 실패 시 503 STORAGE_UNAVAILABLE 경로가 새로 생김 → note
  - [하] users.family_id 조회 인덱스 없음(findByFamilyIdAndRemovedAtIsNullOrderByUserIdAsc) → note(기존 메모 유지)
  - [하] FOLLOWUP user.md 후속 과제 23건 중 해소된 것이 그대로 남아 있음 → note(정리)
  - [하] TODO.md에서 공유모듈이 「완료 · 리뷰 조건부 통과」에서 「대기 · 기록 없음 · 0/4」로 바뀜. 이 worktree에 shared 리뷰 기록이 없어서인지 미확인 → 사람 확인
  - [하] 체크된 #76 문장이 아직 옛 이름 「SET-07」을 씀(지금은 SET-11) → note(기존 메모 유지)
  - 완료 기준:
  - C1 충족 — 87/87 체크, GREEN 2773 passed.
  - C2 충족 — removedAt 삭제 표시, 조회 · 한도에서 해제 멤버 제외, 단건 조회는 해제 멤버도 읽음, 해제 멤버 수정 · 재해제 404, grade, MemberInfo profileImageUrl · removed가 설계서 04와 맞음. 스웨거 문구만 2건 어긋남([하]).
  - C3 사람 확인 필요 — 검토는 끝남. 공통 규칙 위반은 주석 2건(02:80, [하]).
  - C4 충족 — 확인 필요 사항 14행이 FOLLOWUP에 있음. 열린 것은 「settingsVersion을 올리는 변경의 범위」 하나이고 기록돼 있음.
  - 후속 과제 상태(요약): 남음 — 해제 멤버 초대(auth #125 · #126), 해제 멤버 token TTL 동안 접근, LoadMemberService.sorted 중복, child_location ALTER · FamilyEntity @Version 없음, FamilyApiDoc:74 문구, child-location Idempotency 테스트, 번호판 채우는 시점, 키 교체 방법, 가족당 연결 차량 1대, 중복 VIN 500, service 형식 직접 확인, 필터 순서 · familyId null · PATCH null 구분, 동시성 · 인덱스 · web 오류 행 테스트, 같은 차량 동시 해제 500 · versionToSave 폴백 · 02:216 픽스처 문구, UserErrors.java:1…
  - 테스트: scope all GREEN 2773 passed.

## 확인 필요 사항 (설계서)
| 항목 | 현재 가정 |
|---|---|
| 「자녀 외 멤버 최대 5명」에 GUARDIAN 자신이 들어가는지 | 소스에 없음. GUARDIAN 포함 CHILD가 아닌 멤버 전체를 센다고 가정 |
| GUARDIAN이 자기 role을 바꾸려 할 때 오류 코드 | 결정됨: 422 VALIDATION_FAILED(fields role), 항목 #32 · #67 |
| 전화번호 국제 형식 범위 | 예시가 +8210…뿐. +로 시작하는 국제 형식(E.164)을 받는다고 가정 |
| 멤버 해제 때 User 행 처리 | 결정됨(D7 사람 결정 2026-11-13). DB FK는 두지 않고(02 관례), 행을 지우지 않고 removedAt을 채운다. 조회 범위는 핵심 로직 「멤버」 |
| 멤버 해제 때 그 자녀의 일정 · 장소 처리 | 결정됨(사람 결정 2026-11-14). 일정 · 장소 행은 그대로 둔다. 해제된 자녀에게 일정 생성 · 수정은 404(06), 픽업 시작 때 해제된 자녀는 세션에서 뺀다(08) |
| LINK-05를 차량 앱이 부를 수 있는지 | 4.3 표의 사용 화면에 「차량 설정 요약 화면」이 있으나 4.14 호출은 GUARDIAN뿐. 4.14대로 USER 주체 GUARDIAN만 허용 |
| LINK-05 때 차량 기기 토큰 폐기 | 결정됨(사람 결정 2026-11-07). 해제하면 그 차량의 살아 있는 기기를 모두 폐기한다. 해제된 차량 앱은 AUTH-02로 다시 등록한다. 포트도 결정됨: RevokeVehicleDevicesPort → auth RevokeVehicleDevicesUseCase |
| settingsVersion을 올리는 변경의 범위 | SET-06 · SET-11(자녀 위치 사용)이 올림. 일정(SET-02 · 03) · 장소(SPOT-02 · 03) · 멤버 변경도 올리는지 정해야 함(SET-08 충돌 판단에 영향) |
| 번호판 마스킹 규칙 | 예시 「12가 ****」뿐. 마지막 숫자 4자리를 *로 바꾼다고 가정 |
| user ↔ auth 서로 부름 | auth는 User 생성을 user에 요청하고, user는 기기 상태 · 폐기를 auth에 요청. user 쪽은 out 포트로 둔다. auth 쪽 임시 어댑터(LoadMemberDeviceStatusPort 기기 상태 · RevokeMemberDevicesPort 기기 폐기)는 auth 연동으로 교체됨 |
| 진행 중 픽업 확인(FAM-07 · LINK-05)을 pickup in 포트로 부르는 것 | 해소: pickup 구현 뒤 CheckActivePickupPort 어댑터가 pickup ActivePickupQueryUseCase를 부른다(임시 false 어댑터는 없어짐)(사람 결정 2026-11-13) |
| 포트 · 유스케이스 이름 | 소스 2.2 예시(StartPickupUseCase, LoadSessionPort)의 이름 규칙을 따른 제안 |
| LINK-04 NEW로 만드는 GUARDIAN의 name · phone | name은 결정됨(사람 결정 2026-11-07). LINK-04 요청 name, mode NEW일 때 필수(05_인증-auth.md와 같음). phone은 결정됨: 받지 않는다. LINK-04 NEW로 만드는 GUARDIAN은 phone null을 허용하고, 「자녀 외 phone 필수」 규칙의 예외로 둔다(사람 결정 2026-11-13) |
| Role enum 위치 | 해소: 02에 「common 순수 타입(Role · PrincipalType · LatLng)은 domain에서 써도 된다」 예외를 둠. user/domain은 common.security.Role을 그대로 쓰고 Role enum을 따로 만들지 않음(사람 결정 2026-11-07, 03 확인 필요 사항) |

## 다음 단계
- 커밋 · 푸시: /tdd commit (기능별 분할 커밋) · /tdd push (커밋 후 기능 브랜치 푸시)
- 다음 도메인: 인증 (05_인증-auth.md). 시작: /tdd domain auth

## 후속 과제
- users.removed_at 컬럼: schema.sql에 users 정의 없음. prod는 ddl-auto validate라 운영 DB에 removed_at(TIMESTAMPTZ NULL) · grade 컬럼을 넣는 마이그레이션 방법을 정해야 함. SaveUserPort.deleteUser는 FAM-07 삭제 표시 뒤 쓰는 곳이 없음 — 정리 여부 결정 필요
- 중간 리뷰(#80~#83) [상] 해제된 멤버의 초대 발급 · 수락 통과는 auth 항목(05 추가분)이 처리될 때까지 열려 있음. auth · schedule · pickup 도메인 진행 때 우선 처리. [하] 해제된 멤버도 access token TTL 동안 자기 사진 변경 가능(ChangeProfileImageService). [하] LoadMemberService.sorted는 어댑터 OrderByUserIdAsc와 겹침(#72 단위 테스트가 기댐)
- [도메인 리뷰 2026-11-14 중] 해제 멤버 차단은 auth 05 #125 · #126, schedule 06 #84, pickup 08 #276이 처리될 때까지 운영 배포 금지. 설정 요약(SET-07 GetSetupSummaryService)에서 해제된 자녀를 어떻게 다룰지 schedule 진행 때 사람 결정 필요
- [prod DDL] 운영 DB에 실행: ALTER TABLE users ADD COLUMN removed_at TIMESTAMPTZ NULL, ADD COLUMN grade VARCHAR(20) NULL; (prod ddl-auto validate)
- [도메인 리뷰 하] 해제된 CO_GUARDIAN · DELEGATE도 access token TTL(PT30M) 동안 FAM-01~03 · SET-06 · SET-11 · 사진 변경을 부를 수 있음(요청마다 폐기 확인 없음). findMember · findMembers가 멤버마다 서명 URL을 만들어 이름만 쓰는 pickup · schedule 호출도 서명 비용 · 503 경로를 가짐(이름 전용 조회 분리 검토). 테스트 전용 보조 생성자 · with 메서드(MemberInfo 5인자, MemberResult 6인자 · withGrade, UpdateMemberCommand 7인자 · withGrade, User.restore 5~7인자)는 main 코드에서 쓰지 말 것. SaveUserPort.deleteUser는 쓰는 곳 없음 — 지울지 사람 결정. FOLLOWUP user.md 해소된 후속 과제 정리 필요. TODO.md 공유모듈 상태가 이 worktree에서 「기록 없음 0/4」로 보임 — 사람 확인
- [운영 · 2026-11-13] prod(ddl-auto validate) 배포 전에 실행: ALTER TABLE family ADD COLUMN child_location_enabled BOOLEAN NOT NULL DEFAULT false; (dev는 ddl-auto update로 자동 생성). [팀장 결정 2026-11-13] 자녀 위치 사용 API 번호는 schedule의 SET-07(setup-summary)과 겹쳐 SET-11로 바꿈(체크된 #76 문장의 「SET-07」은 옛 이름). [하 · 후속] Family @Version 없음: SET-06 · SET-11이 동시에 오면 다른 설정 값이 되돌아갈 수 있음. FamilyInfo · FamilyResult · Family.restore · FamilyEntity의 보조 생성자는 값을 false로 채우므로 main 코드에서 쓰지 말 것. #255 구현 때 「자녀 동의 AND 가족 설정」 조건과 포트 위치를 확인
- [2026-11-13 재리뷰 처리] 04의 요약 · 범위 · 구조 · FAM-01 행 · 예시에 SET-11 · childLocationEnabled 반영, SET-11 예시 추가, 확인 필요 「settingsVersion을 올리는 변경」 행 갱신(SET-06 · SET-11). [하 · 선택] PUT /v1/families/me/child-location이 Idempotency-Key를 요구하지 않음을 보는 테스트 없음(UserIdempotencyKeyTest는 call-mode만). [사람 결정 · 이전 지적 유지] SET-06 「다음 세션부터 적용」 문구(FamilyApiDoc:55, 04:102), ACTIVE_PICKUP_EXISTS 두 모듈 중복(UserErrors:27 · AuthErrors:22), 04:8 · 04:227 「pickup 임시 false」 문장이 연동 뒤 사실과 다름, #64 문장 · 증거가 지금 코드와 다름
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
