# 09. 알림 (notification)

푸시 발송, 수신 확인, 재발송. pickup이 ApplicationEventPublisher로 발행한 이벤트를 커밋 뒤에 받아 FCM으로 보낸다(별도 큐 없음). 푸시 토큰은 auth의 Device에서 읽는다. 차량은 FCM을 쓰지 않고 SSE(pickup)로 받는다.

| 항목 | 내용 |
|---|---|
| 범위 | 엔티티 Notification. API NOTI-01(수신 확인) · NOTI-02(알림 이력) · NOTI-03(다시 보내기). FCM 발송 리스너(AFTER_COMMIT · @Async), 재발송 스케줄러(1분), 다른 모듈용 in 포트(Notification 행 기록) |
| 의존 | 03_공유모듈-shared, 05_인증-auth(푸시 대상 기기), 04_사용자-user(받는 사람 이름), 08_픽업-pickup(세션 이벤트, 호출 진행 기록 · 확인 여부) |
| 진행 | 계층 병렬 |

## 구조

```text
notification
├─ adapter
│  ├─ in
│  │  ├─ web          NotificationController(NOTI-01~03), NotificationApiDoc
│  │  ├─ event        FCM 발송 리스너(@TransactionalEventListener AFTER_COMMIT + @Async)
│  │  └─ scheduler    재발송 작업(1분마다)
│  └─ out
│     ├─ persistence  NotificationEntity, 저장소
│     └─ fcm          Firebase Admin SDK 발송
├─ application/port/in · out · service
└─ domain             Notification, 재발송 규칙
```

## 테이블

알림 (Notification)

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| notificationId | BIGINT | PK | |
| sessionId | BIGINT | FK → PickupSession | |
| callId | BIGINT | FK → Call, NULL | 합류 안내일 때만 |
| type | VARCHAR(30) | | 예: SESSION_STARTED · DEPARTURE · CALL · PREP_EXTENDED · HELP |
| deviceId | BIGINT | FK → Device | 받는 기기 |
| sentAt | TIMESTAMPTZ | NULL | FCM 발송 성공 시각 |
| deliveredAt | TIMESTAMPTZ | NULL | NOTI-01 |
| openedAt | TIMESTAMPTZ | NULL | |

## API 명세

| ID | Method · 경로 | 호출 | Idempotency-Key | 요청 → 응답 | 오류 |
|---|---|---|---|---|---|
| NOTI-01 | POST /v1/notifications/{notificationId}/delivery-receipt | 알림을 받은 기기의 앱(FCM을 받으면 자동) | 불필요 | receivedAt → 204 | 공통 |
| NOTI-02 | GET /v1/pickup-sessions/{sessionId}/notifications | GUARDIAN · CO_GUARDIAN · DELEGATE | 불필요 | — → 200 items[](notificationId · type · receiverName · title · sentAt · deliveredAt · openedAt · attempt) | 공통 |
| NOTI-03 | POST /v1/notifications/{notificationId}/resend | GUARDIAN · CO_GUARDIAN · DELEGATE | 필요 | — → 202 notificationId(새로 보낸 알림) | 429 RESEND_TOO_SOON |

응답 예시
- NOTI-02: `{"items":[{"notificationId":7001,"type":"CALL","receiverName":"민준","title":"이제 나와요","sentAt":"2026-11-05T18:09:02+09:00","deliveredAt":"2026-11-05T18:09:04+09:00","openedAt":null,"attempt":1}]}`
- NOTI-03: `{"notificationId":7002}`

## 핵심 로직

### 행 기록 (pickup이 부름)
- pickup 서비스는 트랜잭션 안에서 RecordNotificationUseCase로 Notification 행을 만든다(Call 생성과 행 기록을 한 트랜잭션으로 묶어 중복 발송과 누락을 막음). 받는 사람 userId 목록을 받아 auth의 푸시 대상 기기(살아 있고 pushToken이 있는 기기)마다 한 행씩 만든다.
- 롤백되면 행도 이벤트도 남지 않는다.

### 발송 (리스너)
1. pickup이 발행한 세션 이벤트를 AFTER_COMMIT · @Async로 받는다. FCM이 느려도 API 응답은 기다리지 않는다.
2. 그 이벤트의 Notification 행이 있으면 행마다 FCM 알림을 보내고 성공하면 sentAt을 채운다. FCM 데이터에 notificationId를 담는다(NOTI-01에서 씀).
3. 행이 없는 상태 변화 이벤트는 받는 쪽(보호자 · 자녀)의 기기에 같은 data(eventId · seq · stateVersion · occurredAt · payload)를 FCM 데이터 메시지로 보낸다.
4. ETA_UPDATED · VEHICLE_LOCATION은 FCM으로 보내지 않는다(FCM은 잦은 발송을 묶거나 버림). 모바일 앱은 화면이 켜진 동안 SES-03을 15초마다 조회한다.
5. CALL 알림이 발송되면 pickup in 포트 markCallSent를 부른다(Call SENT · CALL_SENT).
6. 보호자는 GUARDIAN · CO_GUARDIAN · DELEGATE(공동 보호자 · 대리보호자 포함), 자녀는 그 세션의 세션 자녀다. 차량 기기는 대상이 아니다.

### 수신 확인 (NOTI-01)
- 푸시를 받으면 앱이 자동으로 부른다. deliveredAt은 첫 receivedAt만 남기고 다시 와도 바꾸지 않는다.
- CALL 알림의 첫 수신 확인이면 pickup in 포트 markCallDelivered를 부른다(Call DELIVERED · CALL_DELIVERED). 차량 정차 · 호출 화면 「수신」 칩의 근거.

### 이력 · 다시 보내기 (NOTI-02 · 03)
- NOTI-02: 그 세션의 알림을 종류 · 받는 사람 이름 · 제목 · 발송 · 수신 · 열람 시각 · attempt로 돌려준다. deliveredAt이 null이면 보호자 화면에 「수신 미확인」.
- NOTI-03: 같은 내용을 FCM으로 다시 보내고 새 Notification 행을 만든다(attempt + 1). 같은 알림을 1분 안에 다시 보내면 429 RESEND_TOO_SOON.

### 재발송 스케줄러
- 1분마다 sentAt 또는 deliveredAt이 빈 행을 찾아 다시 보낸다. CALL은 아이가 「확인했어요」(CALL-04)를 누르지 않았으면 다시 보낸다.
- 처음 발송 뒤 최대 2번 더(attempt 3까지). 재발송은 같은 Call에 Notification 행을 하나 더 쓰는 것이라 테이블이 늘지 않는다.
- 메모리 이벤트라 서버가 발송 도중 꺼져 사라진 알림도 이 작업이 보완한다.

### 시그니처

```java
// in 포트 — web · scheduler
void recordDelivery(DeliveryReceiptCommand c);                    // RecordDeliveryUseCase (NOTI-01)
List<NotificationView> list(NotificationListQuery q);             // ListNotificationsUseCase (NOTI-02)
long resend(ResendCommand c);                                     // ResendNotificationUseCase (NOTI-03)
int resendUndelivered(Instant now);                               // ResendUndeliveredUseCase (스케줄러)
void dispatch(SessionEventMessage m);                             // DispatchSessionEventUseCase (리스너)
void notifyDeviceReplaced(List<Long> previousDeviceIds);          // NotifyDeviceReplacedUseCase (auth RELINK 이벤트)

// in 포트 — 다른 모듈용
List<Long> record(NotificationRequest r);                         // RecordNotificationUseCase (pickup, 같은 트랜잭션) r: sessionId, callId?, type, receiverUserIds

// out 포트
Optional<Notification> load(long notificationId); List<Notification> loadBySession(long sessionId); List<Notification> loadPending(long sessionId, String type, Long callId); List<Notification> loadResendTargets(Instant now); Notification save(Notification n);   // LoadNotificationPort · SaveNotificationPort
int countAttempts(Notification n);                                // LoadNotificationPort (같은 세션 · callId · type · deviceId 행 수)
boolean send(PushMessage m);                                      // SendPushPort (FCM)
List<PushTarget> loadPushTargets(List<Long> userIds); Optional<PushTarget> loadPushTarget(long deviceId);   // LoadPushTargetPort → auth
Map<Long, String> loadNames(List<Long> userIds);                  // LoadMemberNamePort → user
void markCallSent(long callId, Instant at); void markCallDelivered(long callId, Instant at); boolean isCallAcknowledged(long callId);   // CallProgressPort → pickup
```

## 테스트 케이스
- [ ] [기반] Notification.markDelivered는 deliveredAt이 비었을 때만 receivedAt을 채운다
- [ ] [기반] Notification.markSent는 sentAt을 채운다
- [ ] [기반] 재발송 규칙은 attempt가 3이면 더 보내지 않는다
- [ ] [기반] 재발송 규칙은 CALL 알림을 deliveredAt이 있어도 호출이 확인되지 않았으면 재발송 대상으로 본다
- [ ] [기반] 재발송 규칙은 CALL이 아닌 알림을 deliveredAt이 있으면 재발송 대상에서 뺀다
- [ ] [기반] 다시 보내기 규칙은 같은 알림의 마지막 발송 뒤 1분 안이면 RESEND_TOO_SOON으로 거절한다
- [ ] [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [ ] [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [ ] [기반] NotificationEntity가 BaseTimeEntity를 상속하고 테이블 정의대로 매핑된다
- [ ] [기반] 테스트 시드로 CALL 알림 1건(자녀 기기, sentAt 있음 · deliveredAt 없음)을 만든다
- [ ] [persistence] loadBySession은 그 세션의 알림을 발송 순서대로 돌려준다
- [ ] [persistence] loadResendTargets는 sentAt이나 deliveredAt이 빈 행 중 마지막 발송 뒤 1분이 지난 행을 돌려준다
- [ ] [persistence] countAttempts는 같은 세션 · callId · type · deviceId 행 수를 센다
- [ ] [서비스] 행 기록은 받는 사람의 푸시 대상 기기마다 Notification 행을 하나씩 만든다
- [ ] [서비스] 행 기록은 pushToken이 없거나 폐기된 기기에는 행을 만들지 않는다
- [ ] [서비스] 행 기록을 부른 트랜잭션이 롤백되면 Notification 행이 남지 않는다
- [ ] [서비스] 발송 리스너는 트랜잭션이 커밋된 뒤에만 FCM을 부른다
- [ ] [서비스] 발송 리스너는 트랜잭션이 롤백되면 FCM을 부르지 않는다
- [ ] [서비스] 발송 리스너는 요청 스레드와 다른 스레드에서 실행된다
- [ ] [서비스] 발송 리스너는 FCM 성공 때 sentAt을 채우고 실패 때 sentAt을 비워 둔다
- [ ] [서비스] 발송 리스너가 보내는 FCM 데이터에는 notificationId가 들어 있다
- [ ] [서비스] 발송 리스너는 행이 없는 상태 변화 이벤트를 보호자 · 자녀 기기에 eventId · seq · stateVersion · occurredAt · payload 데이터 메시지로 보낸다
- [ ] [서비스] 발송 리스너는 ETA_UPDATED와 VEHICLE_LOCATION을 FCM으로 보내지 않는다
- [ ] [서비스] 발송 리스너는 받는 쪽이 「차량 · 보호자」인 이벤트를 자녀 기기에 보내지 않는다
- [ ] [서비스] 발송 리스너는 차량 기기에 FCM을 보내지 않는다
- [ ] [서비스] CALL 알림 발송이 성공하면 CallProgressPort.markCallSent를 부른다
- [ ] [서비스] 수신 확인은 두 번째 receivedAt이 와도 첫 deliveredAt을 유지한다
- [ ] [서비스] CALL 알림의 첫 수신 확인은 CallProgressPort.markCallDelivered를 부른다
- [ ] [서비스] 다른 기기의 알림에 수신 확인을 보내면 NOT_FOUND로 거절한다
- [ ] [서비스] 알림 이력은 받는 사람 이름과 attempt를 채워 돌려준다
- [ ] [서비스] 다시 보내기는 같은 세션 · callId · type · 기기로 새 행을 만들고 FCM을 보내고 새 notificationId를 돌려준다
- [ ] [서비스] 재발송 작업은 deliveredAt이 빈 알림을 다시 보내고 새 행을 만든다
- [ ] [서비스] 재발송 작업은 확인된 호출의 CALL 알림은 다시 보내지 않는다
- [ ] [서비스] 재발송 작업은 같은 알림을 처음 발송 뒤 2번까지만 다시 보낸다
- [ ] [서비스] RELINK 이전 기기 이벤트를 받으면 이전 기기의 pushToken으로 「다른 기기에서 로그인했어요」를 보낸다
- [ ] [서비스] dev 프로필의 FCM 발송은 테스트용 Firebase 프로젝트 설정을 쓴다
- [ ] [web] POST /v1/notifications/{notificationId}/delivery-receipt는 204를 돌려준다
- [ ] [web] POST /v1/notifications/{notificationId}/delivery-receipt에 receivedAt이 없으면 422 VALIDATION_FAILED를 돌려준다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/notifications는 200과 items[](notificationId · type · receiverName · title · sentAt · deliveredAt · openedAt · attempt)를 돌려준다
- [ ] [web] GET /v1/pickup-sessions/{sessionId}/notifications를 CHILD 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] POST /v1/notifications/{notificationId}/resend는 202와 새 notificationId를 돌려준다
- [ ] [web] POST /v1/notifications/{notificationId}/resend가 1분 안이면 429 RESEND_TOO_SOON과 retryAfterSeconds를 돌려준다
- [ ] [web] POST /v1/notifications/{notificationId}/resend를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] [web] Idempotency-Key는 POST /v1/notifications/{notificationId}/resend에만 요구된다

## 확인 필요 사항

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
