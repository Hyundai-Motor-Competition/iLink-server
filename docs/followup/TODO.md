# TODO · 설계서 기준 개발 할 일

- 생성: 2026-11-14 02:25 KST · tdd-guard
- 설계서: C:/pw/schedule/docs
- 프로젝트: gradle · C:/pw/schedule
- 테스트: ● GREEN · 2914 passed · 전체 · 127s
- 진행: 공유모듈 72/72 · 사용자 87/87 · 인증 128/130 · 일정 88/88 · 장소 107/107 · 픽업 266/278 · 알림 65/65 · 프로필사진 39/39 · 남은 항목 14/866
- 현재 도메인: 일정 (06_일정-schedule.md) · 진행 시작: /tdd go

설계서 번호 순으로 진행한다. 한 도메인의 항목 · 도메인 리뷰 · 완료 기준이 모두 끝나면 다음 도메인으로 넘어간다. 도메인별 상세 기록은 이 폴더의 <key>.md, 리뷰 모음은 REVIEW.md.

## 1. 공유모듈 (shared) · 03_공유모듈-shared.md · 대기 · 항목 72/72 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review shared
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 2. 사용자 (user) · 04_사용자-user.md · 대기 · 항목 87/87 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review user
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 3. 인증 (auth) · 05_인증-auth.md · 대기 · 항목 128/130 · 완료 기준 0/4
### 테스트 케이스
- [ ] #129 [서비스] 초대 발급 · 초대 조회 · 초대 수락은 대상 멤버가 해제된 멤버(user MemberInfo.removed true)면 404 NOT_FOUND로 거절하고 기기 · 토큰을 만들지 않는다(user D7 삭제 표시 뒤 후속, 사람 결정 2026-11-14)
- [ ] #130 [서비스] RevokeMemberDevicesUseCase(멤버 해제 때 호출)는 그 멤버의 기기 · 토큰과 함께 아직 쓰지 않은 초대도 폐기한다(사람 결정 2026-11-14)
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 4. 일정 (schedule) · 06_일정-schedule.md · 완료 · 항목 88/88 · 완료 기준 4/4 · 리뷰 11-14 02:24 통과 (상 0 · 중 0 · 하 7)
- 남은 할 일 없음

## 5. 장소 (place) · 07_장소-place.md · 대기 · 항목 107/107 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review place
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 6. 픽업 (pickup) · 08_픽업-pickup.md · 대기 · 항목 266/278 · 완료 기준 0/4
### 테스트 케이스
- [ ] #267 [서비스] 픽업 완료(SES-11)는 notBoardedChildUserIds에 세션에 없는 childUserId가 있으면 404 NOT_FOUND로 거절하고 세션 상태 · 자녀 탑승 상태를 바꾸지 않는다(사람 결정 2026-11-13)
- [ ] #268 [web] POST 픽업 완료(SES-11)에 세션에 없는 childUserId를 notBoardedChildUserIds로 보내면 404 NOT_FOUND를 돌려준다
- [ ] #269 [web] PUT /v1/pickup-sessions/{sessionId}/route는 spotId 대신 candidate {name, location}을 받으면 임시 장소를 만들어 그 spotId로 경로를 확정한다. spotId와 candidate를 둘 다 보내거나 둘 다 빼면 422 VALIDATION_FAILED(최종디자인 11/13 V02 추천 장소, 팀장 결정 2026-11-13)
- [ ] #270 [web] POST /v1/pickup-sessions/{sessionId}/spot-changes도 spotId 대신 candidate {name, location}을 받아 임시 장소로 세션 장소를 바꾼다(V03 「다른장소로 가기」)
- [ ] #271 [서비스] 장소 변경에 here=true를 보내면 마지막으로 받은 차량 위치로 이름 「차량 정차 위치」, stopPoint · meetingPoint가 같은 임시 장소를 만들어 세션 장소를 바꾸고 바로 합류 안내(CALL)를 보낸다(V03 「여기로 부르기」, 서버가 한 번에 처리)
- [ ] #272 [서비스] here=true인데 받은 차량 위치가 없으면 422 VALIDATION_FAILED, 정차 중이 아니라 호출 가드를 못 넘으면 장소를 바꾸지 않고 409 CALL_GUARD_UNMET로 거절한다
- [ ] #273 [web] 세션 뷰 children[]에 profileImageUrl을, 세션 뷰에 운전 중인 보호자 driver {name, profileImageUrl}을 담는다(V03 · C03 아바타, C02 「엄마가 출발했어요」)
- [ ] #274 [web] 세션 뷰 route에 경로 확정 때 전체 거리 totalMeters를 담는다(부모 메인 출발→도착 진행 막대)
- [ ] #275 [서비스] 보호자에게 보내는 알림(출발 알림 · 준비 시간 추가 · 도움 요청 · 탑승 · 완료 등)의 받는 사람은 가족의 GUARDIAN · CO_GUARDIAN · DELEGATE 모두다(GUARDIAN만이 아님, 사람 결정 2026-11-13)
- [ ] #276 [서비스] 픽업 시작(StartPickup)은 해제된 자녀(user MemberInfo.removed true)의 일정을 세션 자녀에서 뺀다. 지난 기록 조회는 해제된 자녀 이름을 그대로 보인다(사람 결정 2026-11-14)
- [ ] #277 [서비스] PickupProgressQueryUseCase.findProgressBySchedule(familyId, date)는 그 날짜 그 가족 세션의 scheduleId별 진행을 ACTIVE · ON_HOLD → IN_PROGRESS, COMPLETED → COMPLETED로 돌려주고 CANCELLED는 빼며 여러 세션이면 COMPLETED를 앞세운다(schedule SET-13, 사람 결정 2026-11-14)
- [ ] #278 [persistence] LoadSessionPort.loadByFamilyAndDate(familyId, date)는 상태와 상관없이 그 가족 · 그 날짜 세션을 자녀(childUserId · scheduleId)와 함께 돌려주고 다른 날짜 · 다른 가족은 뺀다
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
