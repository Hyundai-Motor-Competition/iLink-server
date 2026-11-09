# TODO · 설계서 기준 개발 할 일

- 생성: 2026-11-09 19:49 KST · tdd-guard
- 설계서: C:/ilink/docs
- 프로젝트: gradle · C:/ilink
- 테스트: ● GREEN · 2046 passed · 전체 · 142s
- 진행: 공유모듈 57/57 · 사용자 73/73 · 인증 120/120 · 일정 77/77 · 장소 58/58 · 픽업 241/241 · 알림 0/44 · 남은 항목 44/670
- 현재 도메인: 픽업 (08_픽업-pickup.md) · 진행 시작: /tdd go

설계서 번호 순으로 진행한다. 한 도메인의 항목 · 도메인 리뷰 · 완료 기준이 모두 끝나면 다음 도메인으로 넘어간다. 도메인별 상세 기록은 이 폴더의 <key>.md, 리뷰 모음은 REVIEW.md.

## 1. 공유모듈 (shared) · 03_공유모듈-shared.md · 대기 · 항목 57/57 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review shared
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 2. 사용자 (user) · 04_사용자-user.md · 대기 · 항목 73/73 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review user
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 3. 인증 (auth) · 05_인증-auth.md · 대기 · 항목 120/120 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review auth
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 4. 일정 (schedule) · 06_일정-schedule.md · 대기 · 항목 77/77 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review schedule
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 5. 장소 (place) · 07_장소-place.md · 대기 · 항목 58/58 · 완료 기준 0/4
### 도메인 리뷰
- [ ] 기록 없음 → /tdd review place
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 6. 픽업 (pickup) · 08_픽업-pickup.md · 완료 · 항목 241/241 · 완료 기준 4/4 · 리뷰 11-09 19:49 통과 (상 0 · 중 0 · 하 5)
- 남은 할 일 없음

## 7. 알림 (notification) · 09_알림-notification.md · 대기 · 항목 0/44 · 완료 기준 0/4
### 테스트 케이스
- [ ] #1 [기반] Notification.markDelivered는 deliveredAt이 비었을 때만 receivedAt을 채운다
- [ ] #2 [기반] Notification.markSent는 sentAt을 채운다
- [ ] #3 [기반] 재발송 규칙은 attempt가 3이면 더 보내지 않는다
- [ ] #4 [기반] 재발송 규칙은 CALL 알림을 deliveredAt이 있어도 호출이 확인되지 않았으면 재발송 대상으로 본다
- [ ] #5 [기반] 재발송 규칙은 CALL이 아닌 알림을 deliveredAt이 있으면 재발송 대상에서 뺀다
- [ ] #6 [기반] 다시 보내기 규칙은 같은 알림의 마지막 발송 뒤 1분 안이면 RESEND_TOO_SOON으로 거절한다
- [ ] #7 [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [ ] #8 [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [ ] #9 [기반] NotificationEntity가 BaseTimeEntity를 상속하고 테이블 정의대로 매핑된다
- [ ] #10 [기반] 테스트 시드로 CALL 알림 1건(자녀 기기, sentAt 있음 · deliveredAt 없음)을 만든다
- [ ] #11 [persistence] loadBySession은 그 세션의 알림을 발송 순서대로 돌려준다
- [ ] #12 [persistence] loadResendTargets는 sentAt이나 deliveredAt이 빈 행 중 마지막 발송 뒤 1분이 지난 행을 돌려준다
- [ ] #13 [persistence] countAttempts는 같은 세션 · callId · type · deviceId 행 수를 센다
- [ ] #14 [서비스] 행 기록은 받는 사람의 푸시 대상 기기마다 Notification 행을 하나씩 만든다
- [ ] #15 [서비스] 행 기록은 pushToken이 없거나 폐기된 기기에는 행을 만들지 않는다
- [ ] #16 [서비스] 행 기록을 부른 트랜잭션이 롤백되면 Notification 행이 남지 않는다
- [ ] #17 [서비스] 발송 리스너는 트랜잭션이 커밋된 뒤에만 FCM을 부른다
- [ ] #18 [서비스] 발송 리스너는 트랜잭션이 롤백되면 FCM을 부르지 않는다
- [ ] #19 [서비스] 발송 리스너는 요청 스레드와 다른 스레드에서 실행된다
- [ ] #20 [서비스] 발송 리스너는 FCM 성공 때 sentAt을 채우고 실패 때 sentAt을 비워 둔다
- [ ] #21 [서비스] 발송 리스너가 보내는 FCM 데이터에는 notificationId가 들어 있다
- [ ] #22 [서비스] 발송 리스너는 행이 없는 상태 변화 이벤트를 보호자 · 자녀 기기에 eventId · seq · stateVersion · occurredAt · payload 데이터 메시지로 보낸다
- [ ] #23 [서비스] 발송 리스너는 ETA_UPDATED와 VEHICLE_LOCATION을 FCM으로 보내지 않는다
- [ ] #24 [서비스] 발송 리스너는 받는 쪽이 「차량 · 보호자」인 이벤트를 자녀 기기에 보내지 않는다
- [ ] #25 [서비스] 발송 리스너는 차량 기기에 FCM을 보내지 않는다
- [ ] #26 [서비스] CALL 알림 발송이 성공하면 CallProgressPort.markCallSent를 부른다
- [ ] #27 [서비스] 수신 확인은 두 번째 receivedAt이 와도 첫 deliveredAt을 유지한다
- [ ] #28 [서비스] CALL 알림의 첫 수신 확인은 CallProgressPort.markCallDelivered를 부른다
- [ ] #29 [서비스] 다른 기기의 알림에 수신 확인을 보내면 NOT_FOUND로 거절한다
- [ ] #30 [서비스] 알림 이력은 받는 사람 이름과 attempt를 채워 돌려준다
- [ ] #31 [서비스] 다시 보내기는 같은 세션 · callId · type · 기기로 새 행을 만들고 FCM을 보내고 새 notificationId를 돌려준다
- [ ] #32 [서비스] 재발송 작업은 deliveredAt이 빈 알림을 다시 보내고 새 행을 만든다
- [ ] #33 [서비스] 재발송 작업은 확인된 호출의 CALL 알림은 다시 보내지 않는다
- [ ] #34 [서비스] 재발송 작업은 같은 알림을 처음 발송 뒤 2번까지만 다시 보낸다
- [ ] #35 [서비스] RELINK 이전 기기 이벤트를 받으면 이전 기기의 pushToken으로 「다른 기기에서 로그인했어요」를 보낸다
- [ ] #36 [서비스] dev 프로필의 FCM 발송은 테스트용 Firebase 프로젝트 설정을 쓴다
- [ ] #37 [web] POST /v1/notifications/{notificationId}/delivery-receipt는 204를 돌려준다
- [ ] #38 [web] POST /v1/notifications/{notificationId}/delivery-receipt에 receivedAt이 없으면 422 VALIDATION_FAILED를 돌려준다
- [ ] #39 [web] GET /v1/pickup-sessions/{sessionId}/notifications는 200과 items[](notificationId · type · receiverName · title · sentAt · deliveredAt · openedAt · attempt)를 돌려준다
- [ ] #40 [web] GET /v1/pickup-sessions/{sessionId}/notifications를 CHILD 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #41 [web] POST /v1/notifications/{notificationId}/resend는 202와 새 notificationId를 돌려준다
- [ ] #42 [web] POST /v1/notifications/{notificationId}/resend가 1분 안이면 429 RESEND_TOO_SOON과 retryAfterSeconds를 돌려준다
- [ ] #43 [web] POST /v1/notifications/{notificationId}/resend를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #44 [web] Idempotency-Key는 POST /v1/notifications/{notificationId}/resend에만 요구된다
### 완료 기준
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨
