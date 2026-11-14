# TODO · 설계서 기준 개발 할 일

- 생성: 2026-11-14 09:47 KST · tdd-guard
- 설계서: C:/pickup/docs
- 프로젝트: gradle · C:/pickup
- 테스트: ● GREEN · 3039 passed · 전체 · 134s
- 진행: 공유모듈 72/72 · 사용자 87/87 · 인증 128/130 · 일정 88/88 · 장소 107/107 · 픽업 283/285 · 알림 65/65 · 프로필사진 39/39 · 남은 항목 4/873
- 현재 도메인: 공유모듈 (03_공유모듈-shared.md) · 진행 시작: /tdd go

설계서 번호 순으로 진행한다. 한 도메인의 항목 · 도메인 리뷰 · 완료 기준이 모두 끝나면 다음 도메인으로 넘어간다. 도메인별 상세 기록은 이 폴더의 <key>.md, 리뷰 모음은 REVIEW.md.

## 1. 공유모듈 (shared) · 03_공유모듈-shared.md · 완료 · 항목 72/72 · 완료 기준 4/4 · 리뷰 11-14 00:02 조건부 통과 (상 0 · 중 3 · 하 7)
- 남은 할 일 없음

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

## 4. 일정 (schedule) · 06_일정-schedule.md · 대기 · 항목 88/88 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review schedule
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

## 6. 픽업 (pickup) · 08_픽업-pickup.md · 대기 · 항목 283/285 · 완료 기준 0/4
### 테스트 케이스
- [ ] #284 [서비스] PickupProgressQueryUseCase.findProgressBySchedule(familyId, date)는 그 날짜 그 가족 세션의 scheduleId별 진행을 ACTIVE · ON_HOLD → IN_PROGRESS, COMPLETED → COMPLETED로 돌려주고 CANCELLED는 빼며 여러 세션이면 COMPLETED를 앞세운다(schedule SET-13, 사람 결정 2026-11-14)
- [ ] #285 [persistence] LoadSessionPort.loadByFamilyAndDate(familyId, date)는 상태와 상관없이 그 가족 · 그 날짜 세션을 자녀(childUserId · scheduleId)와 함께 돌려주고 다른 날짜 · 다른 가족은 뺀다
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
