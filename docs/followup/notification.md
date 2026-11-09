# FOLLOWUP · 알림 (notification)

- 생성: 2026-11-09 20:21 KST · tdd-guard
- 상태: 진행 중 (체크 11/45 · 완료 기준 0/4)
- 테스트: ● GREEN · 2077 passed · 전체 · 141s · .\gradlew.bat test --console=plain
- 설계서: C:/ilink/docs/09_알림-notification.md

## 완료 항목
- [x] #1 [기반] Notification.markDelivered는 deliveredAt이 비었을 때만 receivedAt을 채운다 ← NotificationTest.markDelivered는 deliveredAt이 이미 있으면 다시 와도 바꾸지 않는다
- [x] #2 [기반] Notification.markSent는 sentAt을 채운다 ← NotificationTest.markSent는 sentAt을 채운다
- [x] #3 [기반] 재발송 규칙은 attempt가 3이면 더 보내지 않는다 ← ResendPolicyTest.attempt가 3이면 deliveredAt이 비어 있어도 더 보내지 않는다
- [x] #4 [기반] 재발송 규칙은 CALL 알림을 deliveredAt이 있어도 호출이 확인되지 않았으면 재발송 대상으로 본다 ← ResendPolicyTest.CALL 알림은 deliveredAt이 있어도 호출이 확인되지 않았으면 재발송 대상이다
- [x] #5 [기반] 재발송 규칙은 CALL이 아닌 알림을 deliveredAt이 있으면 재발송 대상에서 뺀다 ← ResendPolicyTest.CALL이 아닌 알림은 deliveredAt이 있으면 재발송 대상에서 뺀다
- [x] #6 [기반] 다시 보내기 규칙은 같은 알림의 마지막 발송 뒤 1분 안이면 RESEND_TOO_SOON으로 거절한다 ← ResendPolicyTest.마지막 발송
- [x] #7 [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다 ← NotificationInPortsTest.다른 모듈용 RecordNotificationUseCase는
- [x] #8 [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다 ← NotificationOutPortsTest.PushMessage는 record이고
- [x] #9 [기반] NotificationEntity가 BaseTimeEntity를 상속하고 테이블 정의대로 매핑된다 ← NotificationEntityTest.NotificationEntity의 PK는
- [x] #10 [기반] 테스트 시드로 CALL 알림 1건(자녀 기기, sentAt 있음 · deliveredAt 없음)을 만든다 ← NotificationSeedTest.테스트 시드로 CALL 알림 1건
- [x] #45 [기반] ErrorCode에 RESEND_TOO_SOON(429)이 있고 BusinessException이 그 코드와 retryAfterSeconds를 함께 담는다 ← BusinessExceptionTest.tooManyRequests(ErrorCode.RESEND_TOO_SOON, 30)로 만든 예외는

## 미완료 항목
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
| Notification 행을 만드는 알림 종류 | type 예시가 SESSION_STARTED · DEPARTURE · CALL · PREP_EXTENDED · HELP 「등」. 이 다섯만 행을 만들고 나머지 상태 이벤트는 행 없이 데이터 메시지만 보낸다고 가정 |
| 데이터 메시지 발송 대상 | 4.12 「받는 쪽」 열대로 보호자 · 자녀를 고름. 「보호자」에 TEACHER가 들어가는지 소스에 없음. 빼는 것으로 가정 |
| attempt 저장 | Notification에 attempt 컬럼이 없음. 같은 세션 · callId · type · deviceId 행 수로 계산한다고 가정 |
| openedAt을 채우는 방법 | 열람을 알리는 API가 없음. 정해야 함 |
| 알림 제목 · 본문 문구 | CALL 「이제 나와요」, HELP 3종 문구만 소스에 있음. 나머지 문구를 정해야 함 |
| sentAt이 빈 행(발송 실패)의 재발송 | 새 행을 쓸지 같은 행을 다시 보낼지 소스에 없음(「같은 Call에 행을 하나 더」는 미확인 재발송 기준). 새 행으로 가정 |
| 재발송을 멈추는 조건 | 세션이 끝났거나 호출이 철회된 경우 소스에 없음. 멈춘다고 가정 |
| FCM 토큰이 무효(UNREGISTERED)일 때 | 소스에 없음. 기기의 pushToken을 비우지 않고 실패로만 둔다고 가정 |
| RELINK 「다른 기기에서 로그인했어요」 푸시의 Notification 행 | sessionId가 세션 FK라 세션 밖 알림을 담기 어려움. 행 없이 FCM만 보낸다고 가정 |
| NOTI-03 「같은 알림」의 기준과 429 응답 | 같은 세션 · callId · type · deviceId로 가정. 429에는 공통 오류 형식대로 retryAfterSeconds를 채운다고 가정 |
| NOTI-01 호출 주체 확인 | 소스는 「알림을 받은 기기의 앱」. 토큰의 주체가 그 행의 기기 주인일 때만 받는다고 가정 |
| 허용 실외 대기 5분 알림의 발송 주체 | 08_픽업-pickup.md와 같은 질문. 판정은 pickup, 발송은 이 모듈로 가정 |
| pickup ↔ notification 서로 부름 | pickup은 행 기록을 이 모듈에 요청하고, 이 모듈은 호출 진행을 pickup에 알림. pickup 쪽은 out 포트 + 테스트 대역으로 먼저 검증 |

## 다음 단계
- 남은 항목 34건부터 계속: /tdd domain notification → /tdd go
- 다음 도메인 없음. 전체 완료 여부를 사람과 확인

## 후속 과제
- 없음
