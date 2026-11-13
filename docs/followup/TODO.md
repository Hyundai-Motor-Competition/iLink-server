# TODO · 설계서 기준 개발 할 일

- 생성: 2026-11-14 01:30 KST · tdd-guard
- 설계서: C:/pw/pickup/docs
- 프로젝트: gradle · C:/pw/pickup
- 테스트: ● GREEN · 2824 passed · 전체 · 144s
- 진행: 공유모듈 72/72 · 사용자 87/87 · 인증 120/126 · 일정 77/84 · 장소 107/107 · 픽업 272/277 · 알림 65/65 · 프로필사진 39/39 · 남은 항목 18/857
- 현재 도메인: 픽업 (08_픽업-pickup.md) · 진행 시작: /tdd go

설계서 번호 순으로 진행한다. 한 도메인의 항목 · 도메인 리뷰 · 완료 기준이 모두 끝나면 다음 도메인으로 넘어간다. 도메인별 상세 기록은 이 폴더의 <key>.md, 리뷰 모음은 REVIEW.md.

## 1. 공유모듈 (shared) · 03_공유모듈-shared.md · 대기 · 항목 72/72 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review shared
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 2. 사용자 (user) · 04_사용자-user.md · 완료 · 항목 87/87 · 완료 기준 4/4 · 리뷰 11-14 01:05 조건부 통과 (상 0 · 중 2 · 하 12)
- 남은 할 일 없음

## 3. 인증 (auth) · 05_인증-auth.md · 대기 · 항목 120/126 · 완료 기준 0/4
### 테스트 케이스
- [ ] #121 [web] 초대 수락(FAM-06)과 연결 승인(LINK-04)의 consents에 SERVICE_IMPROVEMENT(선택 동의)를 보낼 수 있고, 빠져도 수락 · 승인은 성공한다(C00 · 가족 프로필 등록4 「(선택) 서비스 개선」)
- [ ] #122 [서비스] refresh 토큰은 발급 14일(P14D) 뒤 만료되고, 만료된 refresh 토큰으로 POST /v1/auth/refresh를 부르면 유효하지 않은 refresh 토큰과 같은 401로 거절한다(사람 결정 2026-11-13)
- [ ] #123 [통합] access 토큰 유효 기간 PT30M · refresh 토큰 유효 기간 P14D는 application.yml 설정값으로 두고, 발급한 access 토큰의 exp가 발급 시각 + 30분이다
- [ ] #124 [web] GET /v1/vehicle-link-codes/by-code/{code}(LINK-03)가 같은 기기 5번 실패 뒤면 429 LINK_CODE_ATTEMPT_LIMIT과 error.retryAfterSeconds(차단이 풀릴 때까지 남은 초)를 돌려준다(D3)
- [ ] #125 [서비스] 초대 발급 · 초대 조회 · 초대 수락은 대상 멤버가 해제된 멤버(user MemberInfo.removed true)면 404 NOT_FOUND로 거절하고 기기 · 토큰을 만들지 않는다(user D7 삭제 표시 뒤 후속, 사람 결정 2026-11-14)
- [ ] #126 [서비스] RevokeMemberDevicesUseCase(멤버 해제 때 호출)는 그 멤버의 기기 · 토큰과 함께 아직 쓰지 않은 초대도 폐기한다(사람 결정 2026-11-14)
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 4. 일정 (schedule) · 06_일정-schedule.md · 대기 · 항목 77/84 · 완료 기준 0/4
### 테스트 케이스
- [ ] #78 [서비스] SET-09 readiness는 오늘 일정이 DRAFT면 startable false와 missing에 SCHEDULE을 넣어 SES-02 픽업 시작 판정과 같은 값을 준다(D10, 사람 결정 2026-11-13)
- [ ] #79 [web] GET /v1/children/me/next-pickup(CHILD 전용)은 지금 이후 가장 가까운 픽업의 academyName · spotName · targetAt(KST)을 돌려주고, 남은 일정이 없으면 200과 null 본문을 준다(최종디자인 11/13 C01 실내 대기)
- [ ] #80 [web] GET /v1/vehicles/me/weekly-pickups의 각 픽업에 자녀 profileImageUrl을 담는다(V01 자녀 카드)
- [ ] #81 [web] GET /v1/families/me/today-pickups(GUARDIAN · CO_GUARDIAN)는 오늘 가족 자녀들의 픽업을 시각 순으로 childName · academyName · targetAt · status(SCHEDULED · IN_PROGRESS · COMPLETED)로 돌려준다(부모 메인 오늘의 픽업 타임라인)
- [ ] #82 [persistence] Academy는 familyId를 갖고 (familyId, externalPlaceId)가 유니크다. 두 가족이 같은 externalPlaceId로 일정을 등록하면 학원 행이 가족마다 따로 생긴다(사람 결정 2026-11-13: 학원은 가족마다 따로 둠)
- [ ] #83 [서비스] 일정 등록 · 수정의 학원 다시 쓰기는 같은 가족의 학원만 갱신하고, 다른 가족 학원의 이름 · 주소 · 위치 · 출입구 · 대표번호를 바꾸지 않는다
- [ ] #84 [서비스] 해제된 자녀(user MemberInfo.removed true)에게는 일정 생성 · 수정을 404 NOT_FOUND로 거절한다. 해제된 자녀의 기존 일정 행은 지우지 않는다(사람 결정 2026-11-14)
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 5. 장소 (place) · 07_장소-place.md · 대기 · 항목 107/107 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review place
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 6. 픽업 (pickup) · 08_픽업-pickup.md · 진행 중 · 항목 272/277 · 완료 기준 0/4
### 테스트 케이스
- [ ] #270 [web] POST /v1/pickup-sessions/{sessionId}/spot-changes도 spotId 대신 candidate {name, location}을 받아 임시 장소로 세션 장소를 바꾼다(V03 「다른장소로 가기」)
- [ ] #271 [서비스] 장소 변경에 here=true를 보내면 마지막으로 받은 차량 위치로 이름 「차량 정차 위치」, stopPoint · meetingPoint가 같은 임시 장소를 만들어 세션 장소를 바꾸고 바로 합류 안내(CALL)를 보낸다(V03 「여기로 부르기」, 서버가 한 번에 처리)
- [ ] #272 [서비스] here=true인데 받은 차량 위치가 없으면 422 VALIDATION_FAILED, 정차 중이 아니라 호출 가드를 못 넘으면 장소를 바꾸지 않고 409 CALL_GUARD_UNMET로 거절한다
- [ ] #276 user의 LoadMemberService가 pickup에 주는 MemberInfo.profileImageUrl을 가족 조회(GetMyFamilyService · MemberResults)와 같은 방식의 URL로 채워, 세션 뷰 children[].profileImageUrl · driver.profileImageUrl에 실제 값이 나간다(#273 후속, 팀장 결정 2026-11-14)
- [ ] #277 [서비스] 픽업 시작(StartPickup)은 해제된 자녀(user MemberInfo.removed true)의 일정을 세션 자녀에서 뺀다. 지난 기록 조회는 해제된 자녀 이름을 그대로 보인다(사람 결정 2026-11-14)
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 7. 알림 (notification) · 09_알림-notification.md · 대기 · 항목 65/65 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review notification
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 8. 프로필사진 (profile) · 10_프로필사진-profile.md · 완료 · 항목 39/39 · 완료 기준 4/4 · 리뷰 11-10 03:07 통과 (상 0 · 중 0 · 하 2)
- 남은 할 일 없음
