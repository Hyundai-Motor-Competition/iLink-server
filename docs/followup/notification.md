# FOLLOWUP · 알림 (notification)

- 생성: 2026-11-11 12:11 KST · tdd-guard
- 상태: 완료 (체크 64/64 · 완료 기준 4/4)
- 테스트: ● GREEN · 2261 passed · 전체 · 206s · .\gradlew.bat test --console=plain
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
- [x] #11 [persistence] loadBySession은 그 세션의 알림을 발송 순서대로 돌려준다 ← NotificationPersistenceAdapterTest.loadBySession은
- [x] #12 [persistence] loadResendTargets는 sentAt이나 deliveredAt이 빈 행 중 마지막 발송 뒤 1분이 지난 행을 돌려준다 ← NotificationPersistenceAdapterTest.loadResendTargets는
- [x] #13 [persistence] countAttempts는 같은 세션 · callId · type · deviceId 행 수를 센다 ← NotificationPersistenceAdapterTest.countAttempts는
- [x] #14 [서비스] 행 기록은 받는 사람의 푸시 대상 기기마다 Notification 행을 하나씩 만든다 ← RecordNotificationServiceTest.행 기록은
- [x] #15 [서비스] 행 기록은 pushToken이 없거나 폐기된 기기에는 행을 만들지 않는다 ← RecordNotificationServiceTest.행 기록은 pushToken이 없거나
- [x] #16 [서비스] 행 기록을 부른 트랜잭션이 롤백되면 Notification 행이 남지 않는다 ← RecordNotificationServiceTest.행 기록은 부른 쪽 트랜잭션에 참여한다
- [x] #17 [서비스] 발송 리스너는 트랜잭션이 커밋된 뒤에만 FCM을 부른다 ← SessionEventPushListenerTest.발송 리스너는 트랜잭션이
- [x] #18 [서비스] 발송 리스너는 트랜잭션이 롤백되면 FCM을 부르지 않는다 ← SessionEventPushListenerTest.발송 리스너는 트랜잭션이 롤백되면
- [x] #19 [서비스] 발송 리스너는 요청 스레드와 다른 스레드에서 실행된다 ← SessionEventPushListenerTest.발송 리스너는 요청 스레드와
- [x] #20 [서비스] 발송 리스너는 FCM 성공 때 sentAt을 채우고 실패 때 sentAt을 비워 둔다 ← DispatchSessionEventServiceTest.발송 리스너는 FCM 성공 때
- [x] #21 [서비스] 발송 리스너가 보내는 FCM 데이터에는 notificationId가 들어 있다 ← DispatchSessionEventServiceTest.발송 리스너가
- [x] #22 [서비스] 발송 리스너는 행이 없는 상태 변화 이벤트를 보호자 · 자녀 기기에 eventId · seq · stateVersion · occurredAt · payload 데이터 메시지로 보낸다 ← DispatchSessionEventServiceTest.발송 리스너는 행이 없는
- [x] #23 [서비스] 발송 리스너는 ETA_UPDATED와 VEHICLE_LOCATION을 FCM으로 보내지 않는다 ← DispatchSessionEventServiceTest.발송 리스너는 ETA_UPDATED와
- [x] #24 [서비스] 발송 리스너는 받는 쪽이 「차량 · 보호자」인 이벤트를 자녀 기기에 보내지 않는다 ← DispatchSessionEventServiceTest.발송 리스너는
- [x] #25 [서비스] 발송 리스너는 차량 기기에 FCM을 보내지 않는다 ← DispatchSessionEventServiceTest.발송
- [x] #26 [서비스] CALL 알림 발송이 성공하면 CallProgressPort.markCallSent를 부른다 ← DispatchSessionEventServiceTest.발송 리스너는 CALL 알림 발송이 성공하면
- [x] #27 [서비스] 수신 확인은 두 번째 receivedAt이 와도 첫 deliveredAt을 유지한다 ← RecordDeliveryServiceTest.수신 확인은
- [x] #28 [서비스] CALL 알림의 첫 수신 확인은 CallProgressPort.markCallDelivered를 부른다 ← RecordDeliveryServiceTest.CALL 알림의 첫 수신 확인은
- [x] #29 [서비스] 다른 기기의 알림에 수신 확인을 보내면 NOT_FOUND로 거절한다 ← RecordDeliveryServiceTest.다른 기기의 알림에
- [x] #30 [서비스] 알림 이력은 받는 사람 이름과 attempt를 채워 돌려준다 ← ListNotificationsServiceTest.알림 이력은
- [x] #31 [서비스] 다시 보내기는 같은 세션 · callId · type · 기기로 새 행을 만들고 FCM을 보내고 새 notificationId를 돌려준다 ← ResendNotificationServiceTest.다시 보내기는
- [x] #32 [서비스] 재발송 작업은 deliveredAt이 빈 알림을 다시 보내고 새 행을 만든다 ← ResendUndeliveredServiceTest.재발송 작업은
- [x] #33 [서비스] 재발송 작업은 확인된 호출의 CALL 알림은 다시 보내지 않는다 ← ResendUndeliveredServiceTest.재발송 작업은 확인된 호출
- [x] #34 [서비스] 재발송 작업은 같은 알림을 처음 발송 뒤 2번까지만 다시 보낸다 ← ResendUndeliveredServiceTest.재발송
- [x] #35 [서비스] RELINK 이전 기기 이벤트를 받으면 이전 기기의 pushToken으로 「다른 기기에서 로그인했어요」를 보낸다 ← NotifyDeviceReplacedServiceTest.RELINK
- [x] #36 [서비스] dev 프로필의 FCM 발송은 테스트용 Firebase 프로젝트 설정을 쓴다 ← FcmPropertiesDevProfileTest.dev 프로필로 바인딩하면
- [x] #37 [web] POST /v1/notifications/{notificationId}/delivery-receipt는 204를 돌려준다 ← NotificationControllerTest.AuthPrincipal[type=USER, id=31, familyId=12, role=CHILD]
- [x] #38 [web] POST /v1/notifications/{notificationId}/delivery-receipt에 receivedAt이 없으면 422 VALIDATION_FAILED를 돌려준다 ← NotificationControllerTest."{}"
- [x] #39 [web] GET /v1/pickup-sessions/{sessionId}/notifications는 200과 items[](notificationId · type · receiverName · title · sentAt · deliveredAt · openedAt · attempt)를 돌려준다 ← NotificationControllerTest.AuthPrincipal[type=USER, id=111, familyId=12, role=CO_GUARDIAN]
- [x] #40 [web] GET /v1/pickup-sessions/{sessionId}/notifications를 CHILD 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← NotificationControllerTest.GET /v1/pickup-sessions/{sessionId}/notifications를
- [x] #41 [web] POST /v1/notifications/{notificationId}/resend는 202와 새 notificationId를 돌려준다 ← NotificationControllerTest.AuthPrincipal[type=USER, id=121, familyId=12, role=DELEGATE]
- [x] #42 [web] POST /v1/notifications/{notificationId}/resend가 1분 안이면 429 RESEND_TOO_SOON과 retryAfterSeconds를 돌려준다 ← NotificationControllerTest.POST /v1/notifications/{notificationId}/resend가 1분 안이면
- [x] #43 [web] POST /v1/notifications/{notificationId}/resend를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← NotificationControllerTest.POST /v1/notifications/{notificationId}/resend를
- [x] #44 [web] Idempotency-Key는 POST /v1/notifications/{notificationId}/resend에만 요구된다 ← NotificationIdempotencyKeyTest.Idempotency-Key는
- [x] #45 [기반] ErrorCode에 RESEND_TOO_SOON(429)이 있고 BusinessException이 그 코드와 retryAfterSeconds를 함께 담는다 ← BusinessExceptionTest.tooManyRequests(ErrorCode.RESEND_TOO_SOON, 30)로 만든 예외는
- [x] #46 [persistence] save로 저장한 알림을 load로 다시 읽으면 값이 같고, loadPending은 그 세션 · type · callId의 sentAt이 빈 행만 돌려준다 ← NotificationPersistenceAdapterTest.save는 id가
- [x] #47 [서비스] 알림의 LoadPushTargetPort · LoadMemberNamePort · CallProgressPort 어댑터는 auth · user · pickup의 in 포트를 불러 결과를 그대로 옮긴다 ← NotificationModuleLinkAdaptersTest.LoadMemberNamePort.loadNames는
- [x] #48 [서비스] pickup의 RecordNotificationPort 어댑터는 임시 빈 목록 대신 RecordNotificationUseCase.record를 불러 만든 notificationId 목록을 돌려준다 ← PickupNotificationAdapterTest.recordNotifications는
- [x] #49 [서비스] 알림 이력 · 다시 보내기는 요청자 가족의 세션 알림이 아니면 NOT_FOUND로 거절한다 ← ListNotificationsServiceTest.알림 이력은 요청자 가족의
- [x] #50 [persistence] loadResendTargets는 CALL 알림이면 deliveredAt이 있어도 마지막 발송 뒤 1분이 지난 행을 돌려준다 ← NotificationPersistenceAdapterTest.loadResendTargets는 CALL 알림이면
- [x] #51 [서비스] 재발송 작업은 묶음 전체에서 마지막 행을 기준으로 판단해, 마지막 행이 1분 안에 보냈거나 CALL이 아닌데 묶음에 수신된 행이 있으면 보내지 않는다 ← ResendUndeliveredServiceTest.재발송 작업은 묶음 전체의
- [x] #52 [서비스] 재발송 작업은 세션이 끝났거나 호출이 철회된 알림은 다시 보내지 않는다 ← ResendUndeliveredServiceTest.재발송 작업은 세션이 끝났으면
- [x] #53 [서비스] 발송 · 재발송 · 다시 보내기는 폐기된 기기에 푸시를 보내지 않는다(RELINK 이전 기기 푸시만 폐기 기기 토큰을 쓴다) ← ResendNotificationServiceTest.다시 보내기는 폐기된
- [x] #54 [서비스] 재발송 작업과 다시 보내기도 CALL 알림 발송이 성공하면 CallProgressPort.markCallSent를 부른다 ← ResendUndeliveredServiceTest.재발송 작업은 CALL 알림 발송이
- [x] #55 [서비스] 발송 리스너는 이벤트에 담긴 notificationId의 행만 보내고 같은 type의 옛 실패 행은 다시 보내지 않는다 ← DispatchSessionEventServiceTest.발송 리스너는 notificationIds
- [x] #56 [서비스] CALL 수신 확인은 그 호출의 첫 수신일 때만 markCallDelivered를 부른다(다른 기기 · 재발송 행의 수신은 다시 부르지 않는다) ← RecordDeliveryServiceTest.같은 호출의
- [x] #57 [서비스] prod 프로필은 환경 변수로 FCM project-id · credentials-path를 읽는다 ← FcmPropertiesProdProfileTest.prod 프로필에서 FCM project id는
- [x] #58 [서비스] HELP 알림 본문은 payload의 도움 요청 종류(NEED_HELP · CANT_FIND_CAR · CONTACT_PARENT)에 맞는 08 SIG-05 문구를 쓴다 ← DispatchSessionEventServiceTest."NEED_HELP" -> "도움이 필요해요"
- [x] #59 [서비스] 다시 보내기는 같은 묶음에 sentAt이 하나도 없으면 가장 최근 행의 createdAt을 마지막 시도로 보고 1분 제한을 건다 ← ResendNotificationServiceTest.다시 보내기는 sentAt이 하나도
- [x] #60 [서비스] pickup markCallSent는 이미 보낸(sentAt 있음) Call에 다시 불려도 자녀 상태 · stateVersion · CALL_SENT 이벤트를 바꾸지 않는다(재발송 · 다시 보내기 반복 호출 안전) ← CallTest.이미
- [x] #61 [persistence] loadResendTargets는 만든 지 24시간 안의 행만 돌려준다 ← NotificationPersistenceAdapterTest.loadResendTargets는 만든 지
- [x] #62 [서비스] 재발송 작업은 묶음 시도가 3번 이상인 행을 대상 판단 전에 거른다(세션별 조회 · 멈춤 확인을 부르지 않는다) ← ResendUndeliveredServiceTest.재발송 작업은 시도가 3번
- [x] #63 [서비스] 다시 보내기는 세션이 끝났거나 호출이 철회됐으면 409로 거절하고 새 행을 만들지 않는다 ← ResendNotificationServiceTest.다시 보내기는 세션이 끝났으면 409로
- [x] #64 [서비스] 다시 보내기의 1분 제한은 묶음 마지막 행의 sentAt(없으면 그 행의 createdAt) 기준이다(재발송 작업과 같은 규칙) ← ResendNotificationServiceTest.다시 보내기의 마지막 행은

## 미완료 항목
- 없음

## 완료 기준 (공통 규칙 문서)
- [x] C1 설계서의 테스트 케이스가 모두 GREEN
- [x] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [x] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [x] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 2026-11-11 12:11 KST · 도메인 리뷰어 · 결론 조건부 통과 · 상 0 · 중 1 · 하 17 · 리뷰 시점 체크 64/64
- 리뷰 시점 테스트: ● GREEN · 2261 passed · 전체 · 206s · .\gradlew.bat test --console=plain
  - 결론: 조건부 통과
  - 이전 리뷰(11-09 23:02, 하 7건): 7건 모두 그대로 남아 있다. 마지막 알림 커밋 7c1c94c 뒤로 바뀐 파일은 build.gradle과 ops/k8s/dev/api.yaml 두 개뿐이다(`git diff 7c1c94c HEAD --stat`). 알림 코드 · 테스트와 03 · 05 · 08 문서는 바뀌지 않았다.
  - [중] 재발송 작업이 @Scheduled 스레드에서 FCM을 동기로 보낸다. 스케줄러 스레드는 Spring Boot 기본값인 1개다(spring.task.scheduling 설정도, TaskScheduler 빈도 없음). 그래서 pickup 10초 자동 완료(AutoCompleteBoardingScheduler.java:17), SSE ping(SessionStreamRegistry.java:86), 키 정리 작업과 같은 스레드를 쓴다. e58000a로 dev가 실제 FCM을 부르게 됐으므로 FCM이 느려지면 자동 완료가 밀린다. FcmConfig.java:24-27에는 timeout 설정이 없다(SDK 기본 timeout · 재시도 값은 미확인) — 근거 C:/ilink/src/main/java/com/…
  - [하] (shared 단서 확인) C:/ilink/src/main/java/com/example/pickupservice/notification/adapter/in/scheduler/ResendUndeliveredScheduler.java:17이 fixedDelay=60_000L로 고정돼 있다. pickup(AutoCompleteBoardingScheduler.java:17)과 common(IdempotencyKeyCleaner.java:20)은 설정 키를 쓴다. SchedulingConfig.java:7은 조건 없이 켜지고 IntegrationTestSupport.java:10은 dev 프로필과 실제 Clock을 쓴다. 그래서 캐시된 테스트 컨텍스트마다 재발송 작업이 1분마다 같은 DB에서 돈다. 실패 …
  - [하] (이전 지적 남음) 1분 · 최대 3회 값이 여러 곳에 있다: C:/ilink/src/main/java/com/example/pickupservice/notification/application/service/ResendUndeliveredService.java:30 · :65, ResendPolicy.java:12-13, 스케줄러 :17. 마지막 행도 두 번 구한다(:61-64). 경계 계산 방식도 다르다(:65 isAfter, ResendPolicy.java:33은 초 단위로 버림) → 리팩터링
  - [하] (shared 단서 확인 · 이전 지적 남음) RESEND_TOO_SOON이 C:/ilink/src/main/java/com/example/pickupservice/common/error/ErrorCode.java:25에 있는데 쓰는 곳은 ResendNotificationService.java:61 하나다. 한 모듈만 쓰는 다른 429 코드는 각 모듈에서 BusinessException.of(HttpStatus, code, message)로 만든다(AuthErrors.java:27 · :42, PickupErrors.java:88). 03 L95 · L100 · L230, 02 L169와 맞지 않는다 → 사람 결정(03 규칙을 고치거나, retryAfterSeconds를 받는 팩토리를 만들어 옮김)
  - [하] (auth 단서 확인 · 이전 지적 남음) 05에 LoadPushTargetByDeviceUseCase(findPushTargetByDeviceId · findLivePushTargetByDeviceId)가 없고, 08에 ResendGuardUseCase · FindSessionFamilyUseCase · payload notificationIds가 없다(grep 0건) → note
  - [하] (auth 단서 확인) C:/ilink/src/main/java/com/example/pickupservice/notification/application/service/DispatchSessionEventService.java:14가 auth.domain.Platform을 import한다. auth in 포트 record인 PushTarget(auth/application/port/in/PushTarget.java:3)이 도메인 enum을 그대로 담고 있어서 생긴 일이다. pickup도 auth.domain.MemberDeviceStatus · schedule.domain.ScheduleStatus를 같은 방식으로 쓴다 → note(프로젝트 공통 형태)
  - [하] (user 단서 확인, 줄 번호는 틀림) C:/ilink/src/main/java/com/example/pickupservice/notification/adapter/out/user/NotificationMemberNameAdapter.java:24-27이 사용자마다 findMember를 따로 부른다(파일은 31줄이라 59-62줄은 없음). ListNotificationsService.java:43-48도 기기마다 loadPushTargetIncludingRevoked를 부른다. LoadMemberUseCase에는 여러 명을 한 번에 읽는 메서드가 없고 findMembers(familyId)만 있다 → 리팩터링(가족 단위 findMembers 사용)
  - [하] 발송과 저장 사이에 경합이 있다. C:/ilink/src/main/java/com/example/pickupservice/notification/adapter/out/persistence/NotificationPersistenceAdapter.java:64-69는 모든 칸을 덮어쓰는 merge이고 NotificationEntity에는 @Version이 없다. DispatchSessionEventService.java:67 · :76-78은 읽기 → FCM → 저장 순서라, 그 사이에 NOTI-01이 오면 deliveredAt이 null로 돌아갈 수 있다. NOTI-03은 @Transactional 안에서 FCM을 보내므로(ResendNotificationService.java:42 · :66) 커밋…
  - [하] 폐기된 기기에도 새 행을 먼저 만든다. ResendUndeliveredService.java:81-83 · ResendNotificationService.java:64-66에서 저장한 뒤 ResendDelivery.java:22-25가 대상이 없으면 false를 돌려준다. 그 결과 스케줄러는 보내지 못한 행을 시도 3회까지 쌓고, NOTI-03은 실제로 보내지 않았는데 202와 새 notificationId를 돌려준다. ResendNotificationServiceTest:304가 이 동작을 굳히고 있다 → 사람 결정
  - [하] C:/ilink/src/main/java/com/example/pickupservice/notification/adapter/in/web/DeliveryReceiptRequest.java:14가 receivedAt.toInstant()를 쓴다. 02 L156은 요청 시각도 TimeUtils로 바꾸라고 하고, pickup 요청 DTO 4곳은 TimeUtils.toUtc를 쓴다 → 리팩터링
  - [하] notification 테이블에 조회 인덱스가 없다. NotificationEntity.java:17 @Table에도, schema.sql에도 없다. findResendTargets(NotificationJpaRepository.java:22-26)는 1분마다 created_at 조건으로 테이블 전체를 읽고, loadBySession · countAttempts는 session_id로 찾는다. device · pickup_session 테이블은 @Index가 있다 → 항목 추가(설계서 테이블에 인덱스 추가) 또는 note
  - [하] (CI 영향) dev 기본 project-id가 실제 값과 다르다: C:/ilink/src/main/resources/application-dev.yml:16은 pickup-service-dev, ops/k8s/dev/api.yaml:15는 ilink-dev-d6624. FcmPropertiesDevProfileTest.java:21-24는 FCM_PROJECT_ID 환경 변수가 있으면 실패한다(:31처럼 값을 비워 두지 않음). 로컬에 FCM_CREDENTIALS_PATH가 있으면 FcmConfig.java:28 initializeApp(기본 이름)이 두 번째 테스트 컨텍스트부터 예외를 낼 수 있다(재현 안 함). Jenkins Test 단계에는 FCM 변수가 없다 → note
  - [하] (CI 영향) api.yaml:51-55의 Secret fcm이 optional이 아니어서 Secret이 없으면 파드가 뜨지 않고 롤백된다. ilink-secrets.sh는 Secret api만 다룬다 → note(운영 점검표)
  - [하] (이전 지적 남음) 재발송 작업에 「마지막 행 sentAt이 없으면 createdAt」 테스트가 없다(ResendUndeliveredServiceTest의 모든 행에 sentAt 있음). ResendPolicyTest에 lastAttemptAt 테스트도 없다 → 항목 추가
  - [하] (이전 지적 남음) web 테스트 빈칸: NOTI-03 409(SESSION_ALREADY_FINISHED · CALL_ALREADY_REVOKED), NOTI-02 TEACHER 403(NotificationApiDoc.java:29는 TEACHER · CHILD 403이라고 적음). C:/ilink/src/test/java/com/example/pickupservice/notification/adapter/in/web/NotificationControllerTest.java에 없음 → 항목 추가 또는 note
  - [하] (이전 지적 남음) 테스트 주석이 남아 있다: ResendNotificationServiceTest:90 · :137, ResendUndeliveredServiceTest:75 · :172 · :229, NotificationPersistenceAdapterTest:46-49 · :185-199 · :365, NotificationControllerTest:69-78. NotificationPersistenceAdapterTest:348-363은 들여쓰기가 한 칸 더 들어가 있다 → 리팩터링
  - [하] (이전 지적 남음) ResendNotificationServiceTest:175(#59)와 :388(#64)이 거의 같은 것을 검증한다 → note
  - [하] NotificationApiDoc.java:12 @Tag 설명 「알림 수신 확인 · 이력 조회」에 다시 보내기가 빠져 있다 → 리팩터링
  - 완료 기준: C1 충족 — 체크 64/64, GREEN 2261 passed / C2 충족 — NOTI-01~03의 경로 · 메서드 · 상태 · 권한 · 응답 필드(NotificationController.java:31-60, NotificationItemResponse.java:8-17), 시그니처(포트 in · out 대조), 09 결정(묶음 마지막 행 · 24시간 · 3회 · 멈춤 조건 · 1분 기준)이 일치 / C3 사람 확인 필요 — [중] 1건(스케줄러 스레드) 결정 필요, 공통 규칙 어긋남은 [하] 3건(TimeUtils · 주석 · ErrorCode 위치) / C4 충족(기록됨) — openedAt · 나머지 문구 · UNREGISTERED 처리는 09 확인 필요 사항과 FOLLOWUP L1…
  - 후속 과제:
  - 재발송 대상 중복 — 해소됨(ResendUndeliveredService.java:44-63, 묶음 마지막 행 기준)
  - #24 자녀 제외를 pickup에 맡김 — 남음(CallEngine.java:82 SessionReceivers.guardians로 지켜짐)
  - 계층 병렬 한계 — 해소됨(병합 뒤 GREEN)
  - 05 LoadPushTargetByDeviceUseCase 반영 — 남음
  - 1차 [하] 메모 — 남음(#16 DB 롤백 통합 테스트 없음 · Platform import · NOTI-02 N+1 · 05/08 문서). 09 구조 · 시그니처는 해소
  - prod FCM 환경 변수 누락 시에도 기동됨 — 남음(application-prod.yml:16-17 빈 기본값). dev는 e58000a로 값이 들어감
  - HELP 재발송 기본 문구 — 해소됨(09 「HELP 알림 문구」 결정으로 받아들임)
  - loadPending을 쓰는 곳 없음 — 설계 결정 필요(main에서 포트 · 어댑터 선언 말고는 0건)
  - 재발송 작업에 트랜잭션 없음 · 예외 나면 멈춤 — 설계 결정 필요(ResendUndeliveredService.java:41-89에 try/catch 없음)
  - 재리뷰 [하] 메모(확인한 자녀에게도 CALL 재발송 · SSE payload notificationIds · FQCN · 문서) — 남음
  - 최종 리뷰 [하] 7건 — 남음(위 「이전 지적 남음」 항목들)
  - 테스트: mcp__tdd-guard__tests scope "all" → 소스 변경이 없어 14분 전 결과를 재사용. `.\gradlew.bat test --console=plain` GREEN 2261 passed · 206s. 47a1229(build.gradle)는 알림 테스트에 영향 없음: 알림 테스트는 EmbeddedPostgis · zonky를 직접 쓰지 않는다(grep 0건).
- 이전 리뷰: 2026-11-09 23:02 KST (통과), 2026-11-09 22:43 KST (조건부 통과), 2026-11-09 22:12 KST (수정 필요), 2026-11-09 21:32 KST (수정 필요)

## 확인 필요 사항 (설계서)
| 항목 | 현재 가정 |
|---|---|
| Notification 행을 만드는 알림 종류 | type 예시가 SESSION_STARTED · DEPARTURE · CALL · PREP_EXTENDED · HELP 「등」. 이 다섯만 행을 만들고 나머지 상태 이벤트는 행 없이 데이터 메시지만 보낸다고 가정 |
| 데이터 메시지 발송 대상 | 4.12 「받는 쪽」 열대로 보호자 · 자녀를 고름. 「보호자」에 TEACHER가 들어가는지 소스에 없음. 빼는 것으로 가정 |
| attempt 저장 | Notification에 attempt 컬럼이 없음. 같은 세션 · callId · type · deviceId 행 수로 계산한다고 가정 |
| openedAt을 채우는 방법 | 열람을 알리는 API가 없음. 정해야 함 |
| 알림 제목 · 본문 문구 | CALL 「이제 나와요」, HELP 3종 문구만 소스에 있음. 나머지 문구를 정해야 함 |
| sentAt이 빈 행(발송 실패)의 재발송 | 새 행을 쓸지 같은 행을 다시 보낼지 소스에 없음(「같은 Call에 행을 하나 더」는 미확인 재발송 기준). 새 행으로 가정 |
| 재발송을 멈추는 조건 | 결정(2026-11-09): 스케줄러는 세션 COMPLETED · CANCELLED, 호출 REVOKED면 멈춘다. NOTI-03은 같은 경우 409(SESSION_ALREADY_FINISHED · CALL_ALREADY_REVOKED)로 거절한다 |
| 재발송 대상 범위 | 결정(2026-11-09): 만든 지 24시간 안의 행만 보고, 묶음 시도 3번 이상인 행은 판단 전에 거른다 |
| 1분 제한 기준 | 결정(2026-11-09): 스케줄러 · NOTI-03 모두 묶음 마지막 행(notificationId 최대)의 sentAt, 없으면 그 행의 createdAt |
| HELP 알림 문구 | 결정(2026-11-09): 첫 발송은 payload 도움 종류별 08 SIG-05 문구. 재발송은 행에 종류가 없어 기본 문구 |
| FCM 토큰이 무효(UNREGISTERED)일 때 | 소스에 없음. 기기의 pushToken을 비우지 않고 실패로만 둔다고 가정 |
| RELINK 「다른 기기에서 로그인했어요」 푸시의 Notification 행 | sessionId가 세션 FK라 세션 밖 알림을 담기 어려움. 행 없이 FCM만 보낸다고 가정 |
| NOTI-03 「같은 알림」의 기준과 429 응답 | 같은 세션 · callId · type · deviceId로 가정. 429에는 공통 오류 형식대로 retryAfterSeconds를 채운다고 가정 |
| NOTI-01 호출 주체 확인 | 소스는 「알림을 받은 기기의 앱」. 토큰의 주체가 그 행의 기기 주인일 때만 받는다고 가정 |
| 허용 실외 대기 5분 알림의 발송 주체 | 08_픽업-pickup.md와 같은 질문. 판정은 pickup, 발송은 이 모듈로 가정 |
| pickup ↔ notification 서로 부름 | pickup은 행 기록을 이 모듈에 요청하고, 이 모듈은 호출 진행을 pickup에 알림. pickup 쪽은 out 포트 + 테스트 대역으로 먼저 검증 |

## 다음 단계
- 커밋 · 푸시: /tdd commit (기능별 분할 커밋) · /tdd push (커밋 후 기능 브랜치 푸시)
- 다음 도메인 없음. 전체 완료 여부를 사람과 확인

## 후속 과제
- 재발송 대상 중복: 재발송이 같은 세션 · callId · type · deviceId로 새 행을 만들어 앞 시도 행도 loadResendTargets에 계속 나온다. resendUndelivered(#32~#34)에서 묶음의 마지막 행만 보내도록 처리 필요
- #24 자녀 제외는 pickup이 receiverUserIds에서 자녀를 빼는 것에 기댄다(알림 서비스는 PushTarget에 역할이 없어 스스로 거르지 못함). 알림 쪽 테스트는 receiverUserIds 그대로 전달만 확인
- 계층 병렬 한계: web · service lane은 다른 lane의 빈(서비스 · 영속 어댑터)이 없어 각 worktree 전체 테스트에서 컨텍스트 로드가 실패한다. 세 lane을 함께 병합한 뒤 전체 GREEN으로 확인함
- auth 설계서(05_인증-auth.md 「in 포트 — 다른 모듈용」)에 LoadPushTargetByDeviceUseCase.findPushTargetByDeviceId(long) 추가 반영 필요(폐기 기기도 pushToken 있으면 돌려줌, VEHICLE · 토큰 없음 제외). 알림 RELINK 푸시(#35)용. AuthInPortsTest 시그니처 검증 추가 여부도 결정 필요
- 도메인 리뷰 1차 [하] 메모: #16 롤백은 @Transactional 값만 확인(DB 롤백 통합 테스트 없음) · DispatchSessionEventService가 auth.domain.Platform 직접 import · NOTI-02 조회 N+1(기기마다 loadPushTarget, 사용자마다 findMember) · 문서 갱신 필요(09 시그니처에 LoadSessionFamilyPort · loadPushTarget 폐기 포함 여부 · 구조에 adapter/out/auth · pickup · user, 05 LoadPushTargetByDeviceUseCase, 08 FindSessionFamilyUseCase) · #24 자녀 제외는 pickup SessionReceivers.guardians로 지켜짐
- prod FCM 환경 변수(FCM_PROJECT_ID · FCM_CREDENTIALS_PATH)를 빠뜨려도 기동은 되고 푸시만 경고 로그와 함께 실패한다(테스트 환경 때문에 빈 기본값). 운영 배포 점검표에 넣을 것
- HELP 재발송(스케줄러 · NOTI-03)은 Notification 행에 도움 종류가 없어 기본 문구 「도움이 필요해요」로 나간다. 종류별 문구를 유지하려면 행에 종류를 저장하는 설계 결정 필요
- #55 뒤 LoadNotificationPort.loadPending은 쓰는 곳이 없다(포트 · 어댑터 · 테스트는 남김). 지울지 결정 필요
- 재발송 작업은 트랜잭션 없이 포트 호출마다 커밋한다. 한 건 예외면 그 자리에서 멈추고 나머지는 다음 주기(60초)로 밀림 — 묶음마다 try/catch · 로그를 넣을지 결정 필요. 발송 실패 · 예외로 sentAt 없는 행이 남으면 다음 주기 재발송 대상이 됨(설계서 미규정)
- 도메인 재리뷰 [하] 메모: 스케줄러가 이미 확인한 자녀 기기에도 CALL을 다시 보냄(판단은 Call 단위) · notificationIds가 발행 메시지 payload에 섞여 차량 SSE에도 나가고 저장 이벤트와 payload가 다름(08 문서 미반영) · SessionEventPushListener의 notification SessionEventMessage FQCN은 pickup record와 이름이 같아 남김 · 문서 갱신 필요(09 구조, 05 LoadPushTargetByDeviceUseCase의 findLive · findPushTargetByDeviceId, 08 ResendGuardUseCase · FindSessionFamilyUseCase · payload notificationIds)
- 최종 리뷰 [하]: 03 공유모듈 문서의 ErrorCode 설명 갱신 필요(SESSION_ALREADY_FINISHED · CALL_ALREADY_REVOKED · RESEND_TOO_SOON) · 1분 · 최대 3회 상수가 ResendUndeliveredService와 ResendPolicy에 중복, 마지막 행 두 번 계산(리팩터링) · 재발송 작업의 「마지막 행 sentAt 없으면 createdAt」 테스트와 ResendPolicy.lastAttemptAt 단위 테스트 없음 · NOTI-03 409 web 테스트 없음 · 알림 테스트 구획 주석 남음(ResendNotificationServiceTest 90 · 137, ResendUndeliveredServiceTest 75 · 172 · 229, NotificationPersistenceAdapterTest 181-199) · ResendNotificationServiceTest #59 · #64 테스트 거의 중복
