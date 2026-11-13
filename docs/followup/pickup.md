# FOLLOWUP · 픽업 (pickup)

- 생성: 2026-11-13 16:14 KST · tdd-guard
- 상태: 완료 (체크 266/266 · 완료 기준 4/4)
- 테스트: ● GREEN · 2588 passed · 전체 · 275s · .\gradlew.bat test --console=plain
- 설계서: C:/ilink/docs/08_픽업-pickup.md

## 완료 항목
- [x] #1 [기반] PickupSession.start는 ACTIVE · ROUTING · SELECTED · stateVersion 0인 세션과 자녀마다 WAITING인 SessionChild를 만든다
- [x] #2 [기반] PickupSession.start는 장소의 spotId · name · stopPoint · meetingPoint를 세션 스냅샷으로 복사한다
- [x] #3 [기반] 세션을 바꾸는 도메인 동작을 하면 stateVersion이 1 오른다
- [x] #4 [기반] hold는 ACTIVE를 ON_HOLD로 바꾸고 releaseHold는 ON_HOLD를 ACTIVE로 되돌린다
- [x] #5 [기반] cancel은 ACTIVE와 ON_HOLD 세션을 CANCELLED로 바꾼다
- [x] #6 [기반] COMPLETED나 CANCELLED 세션에 cancel을 부르면 SESSION_ALREADY_FINISHED로 거절된다
- [x] #7 [기반] confirmRoute는 spotStatus를 CONFIRMED로 바꾸고 routeVersion을 1 올린다
- [x] #8 [기반] changeSpot은 spotStatus를 SELECTED로 되돌리고 spotVersion을 1 올리고 스냅샷을 새 장소 값으로 바꾼다
- [x] #9 [기반] changeSpot은 MEETING 자녀가 있으면 CHILD_ALREADY_MOVING으로 거절된다
- [x] #10 [기반] applyTelemetry는 etaAt을 observedAt + navEtaSeconds로, remainingMeters와 etaUpdatedAt을 새 값으로 덮어쓴다
- [x] #11 [기반] applyTelemetry는 observedAt이 마지막 값보다 오래되면 아무것도 바꾸지 않는다
- [x] #12 [기반] ETA는 etaUpdatedAt이 60초를 넘게 지나면 stale로 판정된다
- [x] #13 [기반] CallTrigger는 navEtaSeconds 180 · remainingMeters 2000이면 ETA_3MIN을 돌려준다
- [x] #14 [기반] CallTrigger는 navEtaSeconds 600 · remainingMeters 700이면 DIST_700M을 돌려준다
- [x] #15 [기반] CallTrigger는 navEtaSeconds 181 · remainingMeters 701이면 트리거 없음을 돌려준다
- [x] #16 [기반] Guard는 ON_HOLD 세션에서 G1을 unmet에 넣는다
- [x] #17 [기반] Guard는 spotStatus가 CONFIRMED가 아니면 G2를 unmet에 넣는다
- [x] #18 [기반] Guard는 확정 경로가 지금 장소 버전과 다르면 G3을 unmet에 넣는다
- [x] #19 [기반] Guard는 ETA가 stale이면 G4를 unmet에 넣는다
- [x] #20 [기반] Guard는 세션 자녀 중 한 명이라도 callNotBefore가 지나지 않았으면 G5를 unmet에 넣는다
- [x] #21 [기반] Guard는 다섯 조건을 모두 만족하면 met true와 빈 unmet을 돌려준다
- [x] #22 [기반] 새 Call은 status ACCEPTED와 triggerType · 그때의 spotVersion을 갖는다
- [x] #23 [기반] 철회된 호출 뒤에 만든 Call은 prevCallId로 이전 호출을 가리킨다
- [x] #24 [기반] Call.markSent는 status를 SENT로 바꾸고 sentAt을 채운다
- [x] #25 [기반] Call.revoke는 status REVOKED · revokedAt · revokeReason을 채운다
- [x] #26 [기반] 이미 REVOKED인 Call을 revoke하면 CALL_ALREADY_REVOKED로 거절된다
- [x] #27 [기반] 자녀 둘인 세션에서 한 명만 확인하면 Call은 ACKNOWLEDGED가 아니고 둘 다 확인하면 ACKNOWLEDGED다
- [x] #28 [기반] SessionChild.acknowledge를 두 번 부르면 첫 acknowledgedAt이 유지된다
- [x] #29 [기반] SessionChild.acknowledge는 CALLED 자녀를 MEETING으로 바꾼다
- [x] #30 [기반] extendPrep은 18:09에 5분을 더하면 extraPrepMinutes 5 · callNotBefore 18:14로 만든다
- [x] #31 [기반] extendPrep은 18:05에 5분, 18:09에 5분을 더하면 extraPrepMinutes 10 · callNotBefore 18:19로 만든다
- [x] #32 [기반] extendPrep은 누적이 15분을 넘으면 PREP_EXTENSION_LIMIT으로 거절된다
- [x] #33 [기반] extendPrep은 childPhase를 바꾸지 않는다
- [x] #34 [기반] detectBoarding은 STOPPED · 운전석 아닌 문 · 호출 발송 뒤면 boardingDetectedAt · boardingCancelableUntil(+10초) · boardingDoor와 모든 자녀의 boardedAt을 채운다
- [x] #35 [기반] detectBoarding은 STOPPED가 아니면 아무것도 바꾸지 않는다
- [x] #36 [기반] detectBoarding은 합류 안내 발송 전이면 아무것도 바꾸지 않는다
- [x] #37 [기반] rejectBoarding은 취소 창 안이면 그 자녀의 boardedAt만 비운다
- [x] #38 [기반] rejectBoarding은 boardingCancelableUntil이 지났으면 REJECTION_WINDOW_CLOSED로 거절된다
- [x] #39 [기반] complete는 MANUAL인데 vehiclePhase가 STOPPED가 아니면 VEHICLE_NOT_STOPPED로 거절된다
- [x] #40 [기반] complete는 notBoardedChildUserIds의 자녀를 NOT_BOARDED로 바꾸고 세션을 COMPLETED로 바꾼다
- [x] #41 [기반] complete는 탑승하지도 미탑승 지정되지도 않은 자녀가 있으면 CHILDREN_NOT_ALL_BOARDED로 거절된다
- [x] #42 [기반] complete는 모든 자녀의 lastLat · lastLng · lastLocatedAt을 비운다
- [x] #43 [기반] openIncident는 세션을 ON_HOLD로 바꾸고 incidentType · incidentOpenedAt을 채운다
- [x] #44 [기반] 열린 예외가 있는 세션에 openIncident를 부르면 INCIDENT_ALREADY_OPEN으로 거절된다
- [x] #45 [기반] updateChildLocation은 at이 저장된 값보다 오래되면 위치를 바꾸지 않는다
- [x] #46 [기반] 도움 요청 규칙은 같은 아이의 마지막 요청 뒤 60초 안이면 HELP_REQUEST_TOO_SOON과 마지막 시각 + 60초를 돌려준다
- [x] #47 [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] #48 [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] #49 [기반] JPA 엔티티 5개가 BaseTimeEntity를 상속하고 테이블 정의대로 매핑된다(PickupOutcome PK는 sessionId)
- [x] #50 [기반] 테스트 시드로 차량 1대 · 자녀 2명 · 기본 장소가 있는 ACTIVE 세션 1개를 만든다
- [x] #51 [persistence] PickupSession을 SessionChild와 함께 저장하고 읽으면 스냅샷 좌표와 자녀 상태가 같다
- [x] #52 [persistence] 같은 세션에 같은 childUserId의 SessionChild를 두 번 저장하면 유니크 위반이 난다 ← PickupSessionPersistenceAdapterTest.같은 세션에 같은 childUserId의 SessionChild를 두 번 저장하면
- [x] #53 [persistence] loadActiveByVehicle은 그 차량의 ACTIVE · ON_HOLD 세션만 찾는다
- [x] #54 [persistence] loadActiveByChild는 그 자녀가 포함된 진행 중 세션을 찾는다
- [x] #55 [persistence] loadForUpdate로 잠근 세션을 다른 트랜잭션이 loadForUpdate하면 앞 트랜잭션이 끝날 때까지 기다린다
- [x] #56 [persistence] append는 세션 안 seq를 1부터 1씩 늘려 저장한다
- [x] #57 [persistence] 같은 세션에 같은 seq의 SessionEvent를 저장하면 유니크 위반이 난다
- [x] #58 [persistence] loadAfter는 afterSeq보다 큰 이벤트를 seq 오름차순으로 limit개 돌려주고 남으면 hasMore true다 ← SessionEventPersistenceAdapterTest.loadAfter(afterSeq 1, limit 2)는
- [x] #59 [persistence] loadLastHelpRequest는 payload의 childUserId로 그 아이의 마지막 HELP_REQUESTED를 찾는다
- [x] #60 [persistence] loadBoardingExpired는 boardingCancelableUntil이 지난 ACTIVE 세션만 돌려준다
- [x] #61 [persistence] loadLiveCall은 그 세션의 REVOKED가 아닌 가장 최근 Call을 돌려준다
- [x] #62 [persistence] PickupOutcome은 sessionId를 PK로 저장되고 같은 세션에 두 번째 행은 저장되지 않는다
- [x] #63 [persistence] loadHistory는 그 자녀가 포함된 세션 중 기간 안의 COMPLETED · CANCELLED 세션만 돌려준다
- [x] #64 [persistence] markProcessed는 처음 eventId에 true, 같은 eventId에 false를 돌려준다
- [x] #65 [서비스] 픽업 시작은 date가 한국 날짜로 오늘이 아니면 NOT_TODAY로 거절한다 ← StartPickupServiceTest.픽업 시작은 date가 한국 날짜로
- [x] #66 [서비스] 픽업 시작은 학원이 다른 일정이 섞이면 SCHEDULES_NOT_GROUPABLE로 거절한다 ← StartPickupServiceTest.픽업 시작은 학원이 다른 일정이
- [x] #67 [서비스] 픽업 시작은 시간대가 다른 일정이 섞이면 SCHEDULES_NOT_GROUPABLE로 거절한다 ← StartPickupServiceTest.픽업 시작은 목표 픽업 시각이
- [x] #68 [서비스] 픽업 시작은 기본 장소가 없으면 SETUP_INCOMPLETE와 missing PICKUP_SPOT으로 거절한다 ← StartPickupServiceTest.픽업 시작은 기본 장소가 없으면
- [x] #69 [서비스] 픽업 시작은 자녀 기기가 없으면 SETUP_INCOMPLETE와 missing CHILD_DEVICE로 거절한다 ← StartPickupServiceTest.픽업 시작은 자녀 기기 상태가
- [x] #70 [서비스] 픽업 시작은 DRAFT 일정이 있으면 SETUP_INCOMPLETE로 거절한다 ← StartPickupServiceTest.픽업 시작은 DRAFT 일정이 하나라도
- [x] #71 [서비스] 픽업 시작은 그 차량에 다른 진행 중 세션이 있으면 ANOTHER_SESSION_ACTIVE로 거절한다 ← StartPickupServiceTest.픽업 시작은 그 차량에 다른 일정
- [x] #72 [서비스] 픽업 시작은 같은 일정의 진행 중 세션이 있으면 새로 만들지 않고 그 세션을 created false로 돌려준다 ← StartPickupServiceTest.픽업 시작은 같은 차량 · 같은 date
- [x] #73 [서비스] 픽업 시작은 SESSION_STARTED를 seq 1로 저장하고 SESSION_STARTED 알림 기록을 요청한다 ← StartPickupServiceTest.픽업 시작은 저장한 새 세션의
- [x] #74 [서비스] 진행 중 세션 찾기는 차량 주체에 그 차량의 세션, 자녀 주체에 자기가 포함된 세션을 돌려준다 ← FindActiveSessionServiceTest.진행 중 세션 찾기는 차량 주체에 그 차량의
- [x] #75 [서비스] 진행 중 세션 찾기는 세션이 없으면 빈 값을 돌려준다 ← FindActiveSessionServiceTest.진행 중 세션 찾기는 보호자 주체에
- [x] #76 [서비스] 세션 조회는 자녀 주체에게 자기 children 항목만 돌려준다 ← GetSessionServiceTest.세션 조회는 자녀 주체에게 자기 children
- [x] #77 [서비스] 세션 조회는 SES-04 전이면 자녀 주체에게 spot을 null로 주고 eta.stopEtaAt은 준다 ← GetSessionServiceTest.세션 조회는 SES-04 전이면
- [x] #78 [서비스] 세션 조회는 자녀 주체에게 children[].location을 주지 않고 차량 · 보호자 주체에게는 준다 ← GetSessionServiceTest.세션 조회는 자녀 주체에게 children[].location
- [x] #79 [서비스] 세션 조회는 호출이 없으면 status.call NONE을 돌려준다 ← GetSessionServiceTest.세션 조회는 호출이 없으면
- [x] #80 [서비스] 세션 조회는 MEETING 자녀가 있으면 spot.locked true를 돌려준다 ← GetSessionServiceTest.세션 조회는 MEETING 자녀가 있으면
- [x] #81 [서비스] 다른 가족의 세션을 조회하면 NOT_FOUND로 거절한다 ← GetSessionServiceTest.다른 가족의 차량
- [x] #82 [서비스] 세션을 바꾸는 요청의 stateVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절한다 ← SessionAccessTest.요청 5 · 서버 6
- [x] #83 [서비스] 경로 확정은 가족이 저장하지 않은 spotId면 SPOT_NOT_SAVED로 거절한다 ← ConfirmRouteServiceTest.경로 확정은 어느 가족도 저장하
- [x] #84 [서비스] 첫 경로 확정은 자녀에게 DEPARTURE 알림 기록을 요청하고 DEPARTURE_NOTICE_SENT를 저장한다 ← ConfirmRouteServiceTest.첫 경로 확정은 세션 자녀 모두에
- [x] #85 [서비스] 두 번째 경로 확정은 출발 알림을 다시 요청하지 않는다 ← ConfirmRouteServiceTest.두 번째 경로 확정은
- [x] #86 [서비스] 경로 확정은 SPOT_CONFIRMED를 저장한다 ← ConfirmRouteServiceTest.경로 확정은 저장한 SPOT_CONFIRMED를
- [x] #87 [서비스] 장소 변경은 철회되지 않은 발송 호출이 있으면 CALL_ALREADY_SENT로 거절한다 ← ChangeSpotServiceTest.호출 status SENT
- [x] #88 [서비스] 장소 변경이 성공하면 SPOT_CHANGED를 저장한다 ← ChangeSpotServiceTest.장소 변경은 저장한 SPOT_CHANGED를
- [x] #89 [서비스] 픽업 취소는 철회되지 않은 호출을 REVOKED로 바꾸고 CALL_REVOKED와 SESSION_CANCELLED를 저장한다 ← CancelSessionServiceTest.픽업 취소는 CALL_REVOKED 다음에
- [x] #90 [서비스] 픽업 취소는 자녀 위치 세 컬럼을 비운다 ← CancelSessionServiceTest.픽업 취소는 세션을 CANCELLED로
- [x] #91 [서비스] 연락처는 전화번호가 있는 가족 멤버(CHILD 제외)와 role ACADEMY인 학원 대표번호를 돌려준다 ← GetContactsServiceTest.가족 보호자
- [x] #92 [서비스] 차량 ETA 수신은 ETA_UPDATED를 저장하지 않고 발행만 한다 ← ReceiveTelemetryServiceTest.차량 ETA 수신은 ETA_UPDATED를
- [x] #93 [서비스] AUTO 가족에서 트리거와 가드를 통과하면 Call ACCEPTED · CALL 알림 기록 요청 · CALL_TRIGGERED를 한 트랜잭션으로 저장한다 ← ReceiveTelemetryServiceTest.AUTO 가족에서 ETA 180초 안
- [x] #94 [서비스] 호출 저장 중 예외가 나면 Call · SessionEvent가 남지 않고 이벤트도 발행되지 않는다 ← ReceiveTelemetryServiceIntegrationTest.호출 저장 중 CALL_TRIGGERED
- [x] #95 [서비스] ON_HOLD 세션에서는 트리거를 만족해도 호출을 만들지 않는다 ← ReceiveTelemetryServiceTest.ON_HOLD 세션에서는
- [x] #96 [서비스] callHeld가 true면 트리거를 만족해도 호출을 만들지 않는다 ← ReceiveTelemetryServiceTest.callHeld가 true면
- [x] #97 [서비스] 철회되지 않은 호출이 있으면 새 호출을 만들지 않는다 ← ReceiveTelemetryServiceTest.ACKNOWLEDGED
- [x] #98 [서비스] 가드를 통과하지 못하면 호출을 만들지 않고 세션 뷰 call.guard.unmet에 그 가드를 보인다 ← ReceiveTelemetryServiceTest.70초 전에 잰 ETA가
- [x] #99 [서비스] GUARDIAN_CONFIRM 가족에서는 트리거와 가드를 통과해도 호출을 자동으로 만들지 않는다 ← ReceiveTelemetryServiceTest.GUARDIAN_CONFIRM 가족에서는
- [x] #100 [서비스] callNotBefore 전에는 트리거를 만족해도 호출을 만들지 않고 callNotBefore 뒤 첫 SIG-01에서 만든다 ← ReceiveTelemetryServiceTest.callNotBefore 18:09:10
- [x] #101 [서비스] 철회된 호출 뒤 조건이 다시 맞으면 prevCallId를 단 새 호출을 만든다 ← ReceiveTelemetryServiceTest.가장 최근 호출이 철회됐고
- [x] #102 [서비스] markCallSent는 Call을 SENT로, 세션 자녀를 CALLED로 바꾸고 CALL_SENT를 저장한다 ← CallProgressServiceTest.markCallSent는 ACCEPTED Call을
- [x] #103 [서비스] markCallDelivered는 Call을 DELIVERED로 바꾸고 CALL_DELIVERED를 저장한다 ← CallProgressServiceTest.markCallDelivered는 SENT Call을
- [x] #104 [서비스] 차량 이벤트 수신은 같은 eventId를 두 번째로 받으면 무시한다 ← ReceiveVehicleEventServiceTest.차량 이벤트 수신은 같은 eventId를
- [x] #105 [서비스] 차량 이벤트 STOPPED는 vehiclePhase를 STOPPED로 바꾸고 VEHICLE_STOPPED를 저장한다 ← ReceiveVehicleEventServiceTest.차량 이벤트 STOPPED는
- [x] #106 [서비스] 차량 이벤트 DEPARTED는 vehiclePhase를 DEPARTED로 바꾸고 VEHICLE_DEPARTED를 저장한다 ← ReceiveVehicleEventServiceTest.차량 이벤트 DEPARTED는
- [x] #107 [서비스] 탑승 후보가 된 DOOR_CYCLE은 BOARDING_DETECTED를 저장한다 ← ReceiveVehicleEventServiceTest.탑승 후보가 된 DOOR_CYCLE은
- [x] #108 [서비스] 호출 보류는 이미 보낸 호출이 있으면 CALL_ALREADY_SENT로 거절한다 ← HoldCallServiceTest.호출 status SENT
- [x] #109 [서비스] 호출 보류 해제는 callHeld를 false로 바꾸고 저장된 ETA로 판정을 다시 돌려 조건이 맞으면 호출을 만든다 ← HoldCallServiceTest.호출 보류 해제는 callHeld false
- [x] #110 [서비스] 합류 안내 보내기는 AUTO 가족이면 NOT_GUARDIAN_CONFIRM_MODE로 거절한다 ← SendCallServiceTest.합류 안내 보내기는 AUTO 가족이면
- [x] #111 [서비스] 합류 안내 보내기는 가드를 통과하지 못하면 CALL_GUARD_UNMET과 unmet 목록으로 거절한다 ← SendCallServiceTest.가드 G4 미충족
- [x] #112 [서비스] 합류 안내 보내기는 이미 보낸 호출이 있으면 CALL_ALREADY_SENT로 거절한다 ← SendCallServiceTest.살아 있는 호출 status ACCEPTED
- [x] #113 [서비스] 합류 안내 보내기가 성공하면 triggerType MANUAL인 Call을 만든다 ← SendCallServiceTest.합류 안내 보내기가 성공하면 Call
- [x] #114 [서비스] 호출 철회는 세션 자녀를 WAITING으로 되돌리고 위치를 비우고 CALL_REVOKED를 저장한다 ← RevokeCallServiceTest.호출 철회는 CALLED · MEETING 세션
- [x] #115 [서비스] 호출 확인은 형제가 모두 확인했을 때만 CALL_ACKNOWLEDGED를 저장한다 ← AcknowledgeCallServiceTest.호출 확인은 형제 중 아직 확인하
- [x] #116 [서비스] 철회된 호출을 확인하면 CALL_ALREADY_REVOKED로 거절한다 ← AcknowledgeCallServiceTest.철회된 호출을 확인하면 409
- [x] #117 [서비스] 준비 시간 추가는 합류 안내를 이미 받은 자녀면 CALL_ALREADY_SENT로 거절한다 ← ExtendPrepTimeServiceTest.자녀 CALLED · 호출 SENT
- [x] #118 [서비스] 준비 시간 추가는 PREP_TIME_EXTENDED를 저장하고 보호자에게 PREP_EXTENDED 알림 기록을 요청한다 ← ExtendPrepTimeServiceTest.준비 시간 추가는 그 자녀의 extraPr
- [x] #119 [서비스] 자녀 위치는 위치 동의가 없으면 LOCATION_CONSENT_REQUIRED로 거절한다 ← ReportChildLocationServiceTest.자녀 위치는 위치 동의가 없으면
- [x] #120 [서비스] 자녀 위치는 끝난 세션이면 SESSION_ALREADY_FINISHED로 거절한다 ← ReportChildLocationServiceTest.세션 COMPLETED
- [x] #121 [서비스] 자녀 위치는 WAITING 자녀면 409로 거절한다 ← ReportChildLocationServiceTest.자녀 위치는 WAITING 자녀면
- [x] #122 [서비스] 자녀 위치는 CALLED 자녀의 lastLat · lastLng · lastLocatedAt을 덮어쓴다 ← ReportChildLocationServiceTest.자녀 위치는 CALLED 자녀의
- [x] #123 [서비스] 도움 요청은 HELP_REQUESTED(payload childUserId · type)를 저장하고 보호자에게 HELP 알림 기록을 요청하고 nextAllowedAt = 지금 + 60초를 돌려준다 ← RequestHelpServiceTest.type "NEED_HELP"
- [x] #124 [서비스] 도움 요청 1분 제한은 형제의 요청에 영향을 받지 않는다 ← RequestHelpServiceTest.도움 요청 1분 제한은 형제의
- [x] #125 [서비스] 탑승 완료 IMMEDIATE는 PickupOutcome을 boardingMethod IMMEDIATE로 저장하고 SESSION_COMPLETED를 저장한다 ← CompletePickupServiceTest.탑승 완료 IMMEDIATE는 PickupOutcome을
- [x] #126 [서비스] 탑승 완료 MANUAL은 미탑승 지정 외 자녀의 boardedAt을 채우고 boardingMethod MANUAL로 저장한다 ← CompletePickupServiceTest.탑승 완료 MANUAL은 PickupOutcome을
- [x] #127 [서비스] 탑승 완료는 PickupOutcome.actualStopAt에 VEHICLE_STOPPED의 occurredAt을 넣는다 ← CompletePickupServiceTest.탑승 완료는 PickupOutcome.actualStopAt
- [x] #128 [서비스] 자동 완료 작업은 취소 창이 지나고 모든 자녀가 탄 세션을 boardingMethod DOOR로 완료한다 ← AutoCompleteBoardingServiceTest.자동 완료 작업은 PickupOutcome을
- [x] #129 [서비스] 자동 완료 작업은 SES-12로 boardedAt이 비워진 자녀가 있는 세션을 완료하지 않는다 ← AutoCompleteBoardingServiceTest.자동 완료 작업은 SES-12로
- [x] #130 [서비스] 탑승 취소는 자녀가 여러 명인데 childUserId가 없으면 VALIDATION_FAILED로 거절한다 ← RejectBoardingServiceTest.탑승 취소는 자녀가 여러 명인데
- [x] #131 [서비스] 탑승 예외는 INCIDENT_OPENED를 저장한다 ← OpenIncidentServiceTest.탑승 예외는 INCIDENT_OPENED를
- [x] #132 [서비스] 픽업 이력은 month 2026-11에 한국 날짜 11월 1일~30일 세션을 돌려준다 ← GetPickupHistoryServiceTest.[1] guardianRole = GUARDIAN
- [x] #133 [서비스] 픽업 결과는 예상 18:11:00 · 실제 18:11:50이면 diffSeconds 50을 돌려준다 ← GetOutcomeServiceTest.픽업 결과는 예상 18:11:00
- [x] #134 [서비스] 결과 정정은 actualStopAt을 그대로 두고 correctedStopAt · correctionReason · correctionStatus REQUESTED · correctedBy를 채운다 ← CorrectOutcomeServiceTest.결과 정정은 actualStopAt 18:11:50을
- [x] #135 [서비스] 결과 정정은 correctedStopAt이 세션 날짜 밖이면 VALIDATION_FAILED로 거절한다 ← CorrectOutcomeServiceTest.[3] correctedStopAt = "2026-11-10T00:11:20Z"
- [x] #136 [서비스] 데이터 설정 변경은 learningConsent와 retentionDays 90을 돌려준다 ← UpdateDataSettingsServiceTest.데이터 설정 변경은 설정 행이 없
- [x] #137 [서비스] SSE 리스너는 커밋된 세션 이벤트를 그 세션 차량의 연결로 보낸다 ← SessionEventStreamListenerTest.세션 A의 이벤트는 세션 A의 연결로만
- [x] #138 [web] GET /v1/pickup-sessions/active는 진행 중 세션이 있으면 200과 sessionId · status · stateVersion을 돌려준다 ← PickupIdempotencyKeyTest.SES-01 GET active
- [x] #139 [web] GET /v1/pickup-sessions/active는 진행 중 세션이 없으면 204를 돌려준다 ← PickupSessionControllerTest.GET /v1/pickup-sessions/active는 진행 중 세션이 없으면
- [x] #140 [web] POST /v1/pickup-sessions는 새 세션이면 201과 SessionView를 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions는 새 세션이면 201
- [x] #141 [web] POST /v1/pickup-sessions는 진행 중인 같은 세션이면 200을 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions는 진행 중인 같은 세션이면
- [x] #142 [web] POST /v1/pickup-sessions가 오늘이 아니면 422 NOT_TODAY를 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions가 오늘이 아니면
- [x] #143 [web] POST /v1/pickup-sessions에 빈 scheduleIds를 보내면 422 VALIDATION_FAILED를 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions에 빈 scheduleIds를
- [x] #144 [web] POST /v1/pickup-sessions를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← PickupSessionControllerTest.AuthPrincipal[type=USER, id=101, familyId=12, role=GUAR
- [x] #145 [web] GET /v1/pickup-sessions/{sessionId}는 200과 SessionView 필드를 돌려준다 ← PickupIdempotencyKeyTest.SES-03 GET session
- [x] #146 [web] GET /v1/pickup-sessions/{sessionId}의 시각 필드는 +09:00으로 내려간다 ← PickupSessionControllerTest.GET /v1/pickup-sessions/{sessionId}의 시각 필드는
- [x] #147 [web] PUT /v1/pickup-sessions/{sessionId}/route는 200과 SessionView를 돌려준다 ← PickupSessionControllerTest.PUT /v1/pickup-sessions/{sessionId}/route는 200과
- [x] #148 [web] PUT /v1/pickup-sessions/{sessionId}/route가 저장하지 않은 장소면 422 SPOT_NOT_SAVED를 돌려준다 ← PickupSessionControllerTest.PUT /v1/pickup-sessions/{sessionId}/route가 저장하지 않은
- [x] #149 [web] PUT /v1/pickup-sessions/{sessionId}/route의 stateVersion이 다르면 409 STATE_VERSION_CONFLICT와 최신 세션 뷰를 돌려준다 ← PickupSessionControllerTest.PUT /v1/pickup-sessions/{sessionId}/route의 stateVersion이
- [x] #150 [web] POST /v1/pickup-sessions/{sessionId}/spot-changes는 200과 SessionView를 돌려준다 ← PickupIdempotencyKeyTest.SES-05 POST spot-changes
- [x] #151 [web] POST /v1/pickup-sessions/{sessionId}/spot-changes가 자녀 이동 중이면 409 CHILD_ALREADY_MOVING을 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions/{sessionId}/spot-changes가 자녀 이동 중이면
- [x] #152 [web] POST /v1/pickup-sessions/{sessionId}/spot-changes를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions/{sessionId}/spot-changes를 DELEGATE
- [x] #153 [web] PUT /v1/pickup-sessions/{sessionId}/hold는 200과 status.session ON_HOLD인 SessionView를 돌려준다 ← PickupIdempotencyKeyTest.SES-06 PUT hold
- [x] #154 [web] DELETE /v1/pickup-sessions/{sessionId}/hold는 쿼리 stateVersion을 받아 200과 status.session ACTIVE인 SessionView를 돌려준다 ← PickupIdempotencyKeyTest.SES-06 DELETE hold
- [x] #155 [web] POST /v1/pickup-sessions/{sessionId}/cancellation은 200과 status.session CANCELLED인 SessionView를 돌려준다 ← PickupIdempotencyKeyTest.SES-07 POST cancellation
- [x] #156 [web] POST /v1/pickup-sessions/{sessionId}/cancellation이 끝난 세션이면 409 SESSION_ALREADY_FINISHED를 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions/{sessionId}/cancellation이 끝난 세션이면
- [x] #157 [web] POST /v1/pickup-sessions/{sessionId}/cancellation을 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions/{sessionId}/cancellation을 DELEGATE
- [x] #158 [web] GET /v1/pickup-sessions/{sessionId}/stream은 그 세션 차량 토큰에 text/event-stream으로 응답한다 ← PickupStreamControllerTest.GET /v1/pickup-sessions/{sessionId}/stream은 그 세션 차량 토큰에
- [x] #159 [web] GET /v1/pickup-sessions/{sessionId}/stream을 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← PickupStreamControllerTest.GET /v1/pickup-sessions/{sessionId}/stream을 USER 토큰으로
- [x] #160 [web] GET /v1/pickup-sessions/{sessionId}/stream에 Last-Event-ID 41을 보내면 seq 42 이후 저장 이벤트부터 보낸다 ← PickupStreamControllerTest.GET /v1/pickup-sessions/{sessionId}/stream에 Last-Event-ID
- [x] #161 [web] GET /v1/pickup-sessions/{sessionId}/stream은 이벤트를 event · id(seq) · data(eventId · seq · stateVersion · occurredAt · payload) 형식으로 보낸다 ← PickupStreamControllerTest.GET /v1/pickup-sessions/{sessionId}/stream은 이어 받는 이벤트를
- [x] #162 [web] GET /v1/pickup-sessions/{sessionId}/stream은 ping 간격마다 빈 주석 `: ping`을 보낸다 ← PickupStreamControllerTest.GET /v1/pickup-sessions/{sessionId}/stream은 ping 간격마다
- [x] #163 [web] GET /v1/pickup-sessions/{sessionId}/events는 200과 items[] · hasMore를 돌려준다 ← PickupIdempotencyKeyTest.SES-09 GET events
- [x] #164 [web] GET /v1/pickup-sessions/{sessionId}/events에 limit 101을 보내면 422 VALIDATION_FAILED를 돌려준다 ← PickupSessionControllerTest.GET /v1/pickup-sessions/{sessionId}/events에 limit 101
- [x] #165 [web] GET /v1/pickup-sessions/{sessionId}/contacts는 200과 items[](name · role · phone)를 돌려준다 ← PickupIdempotencyKeyTest.SES-10 GET contacts
- [x] #166 [web] POST /v1/pickup-sessions/{sessionId}/completion은 200과 status.session COMPLETED인 SessionView를 돌려준다 ← PickupSessionControllerTest.mode "IMMEDIATE"
- [x] #167 [web] POST /v1/pickup-sessions/{sessionId}/completion이 MANUAL인데 정차 중이 아니면 422 VEHICLE_NOT_STOPPED를 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions/{sessionId}/completion이 MANUAL인데
- [x] #168 [web] POST /v1/pickup-sessions/{sessionId}/completion이 빠진 자녀가 있으면 422 CHILDREN_NOT_ALL_BOARDED를 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions/{sessionId}/completion이 빠진 자녀가
- [x] #169 [web] POST /v1/pickup-sessions/{sessionId}/boarding-rejection은 200과 SessionView를 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions/{sessionId}/boarding-rejection은 200과
- [x] #170 [web] POST /v1/pickup-sessions/{sessionId}/boarding-rejection이 10초가 지났으면 409 REJECTION_WINDOW_CLOSED를 돌려준다 ← PickupSessionControllerTest.POST /v1/pickup-sessions/{sessionId}/boarding-rejection이 10초가
- [x] #171 [web] PUT /v1/pickup-sessions/{sessionId}/incident는 200과 incident가 채워진 SessionView를 돌려준다 ← PickupSessionControllerTest.PUT /v1/pickup-sessions/{sessionId}/incident는 200과
- [x] #172 [web] PUT /v1/pickup-sessions/{sessionId}/incident가 이미 열린 예외면 409 INCIDENT_ALREADY_OPEN을 돌려준다 ← PickupSessionControllerTest.PUT /v1/pickup-sessions/{sessionId}/incident가 이미
- [x] #173 [web] POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry는 202를 돌려준다 ← PickupSignalControllerTest."선택 필드 모두"
- [x] #174 [web] POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry에 음수 navEtaSeconds를 보내면 422 VALIDATION_FAILED를 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry에 음수
- [x] #175 [web] POST /v1/pickup-sessions/{sessionId}/vehicle-events는 202를 돌려준다 ← PickupSignalControllerTest.type "STOPPED", door null
- [x] #176 [web] POST /v1/pickup-sessions/{sessionId}/vehicle-events에 door 없는 DOOR_CYCLE을 보내면 422 VALIDATION_FAILED를 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/vehicle-events에 door 없는
- [x] #177 [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions는 200과 extraPrepMinutes · callNotBefore를 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/prep-time-extensions는 200과
- [x] #178 [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions에 minutes 7을 보내면 422 VALIDATION_FAILED를 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/prep-time-extensions에 minutes 7을
- [x] #179 [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions가 누적 15분을 넘으면 422 PREP_EXTENSION_LIMIT을 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/prep-time-extensions가 누적
- [x] #180 [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions를 GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/prep-time-extensions를 GUARDIAN
- [x] #181 [web] POST /v1/pickup-sessions/{sessionId}/child-locations는 202를 돌려준다 ← PickupSignalControllerTest."accuracyM 있음"
- [x] #182 [web] POST /v1/pickup-sessions/{sessionId}/child-locations가 동의 없으면 403 LOCATION_CONSENT_REQUIRED를 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/child-locations가
- [x] #183 [web] POST /v1/pickup-sessions/{sessionId}/help-requests는 201과 nextAllowedAt을 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/help-requests는 201과
- [x] #184 [web] POST /v1/pickup-sessions/{sessionId}/help-requests가 1분 안 재요청이면 429 HELP_REQUEST_TOO_SOON과 nextAllowedAt을 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/help-requests가 1분 안
- [x] #185 [web] PUT /v1/pickup-sessions/{sessionId}/call-hold는 200과 call.held true인 SessionView를 돌려준다 ← PickupIdempotencyKeyTest.CALL-01 PUT call-hold
- [x] #186 [web] PUT /v1/pickup-sessions/{sessionId}/call-hold가 발송 뒤면 409 CALL_ALREADY_SENT를 돌려준다 ← PickupCallControllerTest.PUT /v1/pickup-sessions/{sessionId}/call-hold가 발송
- [x] #187 [web] DELETE /v1/pickup-sessions/{sessionId}/call-hold는 쿼리 stateVersion을 받아 200과 call.held false인 SessionView를 돌려준다 ← PickupIdempotencyKeyTest.CALL-01 DELETE call-hold
- [x] #188 [web] POST /v1/pickup-sessions/{sessionId}/calls는 201과 callId · status를 돌려준다 ← PickupCallControllerTest.POST /v1/pickup-sessions/{sessionId}/calls는 201과
- [x] #189 [web] POST /v1/pickup-sessions/{sessionId}/calls가 가드 미충족이면 422 CALL_GUARD_UNMET과 unmet을 돌려준다 ← PickupCallControllerTest.POST /v1/pickup-sessions/{sessionId}/calls가 가드 미충족이면
- [x] #190 [web] POST /v1/pickup-sessions/{sessionId}/calls가 AUTO 가족이면 409 NOT_GUARDIAN_CONFIRM_MODE를 돌려준다 ← PickupCallControllerTest.POST /v1/pickup-sessions/{sessionId}/calls가 AUTO
- [x] #191 [web] POST /v1/calls/{callId}/revocation은 200과 status.call REVOKED인 SessionView를 돌려준다 ← PickupIdempotencyKeyTest.CALL-03 POST revocation
- [x] #192 [web] POST /v1/calls/{callId}/revocation이 이미 철회된 호출이면 409 CALL_ALREADY_REVOKED를 돌려준다 ← PickupCallControllerTest.POST /v1/calls/{callId}/revocation이 이미 철회된
- [x] #193 [web] POST /v1/calls/{callId}/revocation을 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← PickupCallControllerTest.POST /v1/calls/{callId}/revocation을 TEACHER 토큰으
- [x] #194 [web] POST /v1/calls/{callId}/acknowledgement는 200과 callId · status · acknowledgedAt을 돌려준다 ← PickupCallControllerTest.POST /v1/calls/{callId}/acknowledgement는 200과
- [x] #195 [web] POST /v1/calls/{callId}/acknowledgement를 차량 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← PickupCallControllerTest.POST /v1/calls/{callId}/acknowledgement를 차량
- [x] #196 [web] GET /v1/children/{childId}/pickup-history는 200과 items[](date · sessionId · status · completedAt)를 돌려준다 ← PickupIdempotencyKeyTest.OUT-01 GET pickup-history
- [x] #197 [web] GET /v1/children/{childId}/pickup-history에 month 2026-13을 보내면 422 VALIDATION_FAILED를 돌려준다 ← PickupOutcomeControllerTest.GET /v1/children/{childId}/pickup-history에 month 2026
- [x] #198 [web] GET /v1/pickup-sessions/{sessionId}/outcome은 200과 completedAt · spotName · boardingMethod · predictedStopAt · actualStopAt · diffSeconds · correction을 돌려준다 ← PickupIdempotencyKeyTest.OUT-02 GET outcome
- [x] #199 [web] PUT /v1/pickup-sessions/{sessionId}/outcome/correction은 200과 correction.status REQUESTED를 돌려준다 ← PickupIdempotencyKeyTest.OUT-03 PUT outcome/correction
- [x] #200 [web] PUT /v1/pickup-sessions/{sessionId}/outcome/correction에 201자 note를 보내면 422 VALIDATION_FAILED를 돌려준다 ← PickupOutcomeControllerTest.PUT /v1/pickup-sessions/{sessionId}/outcome/correction에 201자
- [x] #201 [web] PUT /v1/guardians/me/data-settings는 200과 learningConsent · retentionDays를 돌려준다 ← PickupDataSettingsControllerTest.learningConsent true
- [x] #202 [web] PUT /v1/guardians/me/data-settings를 CO_GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다 ← PickupDataSettingsControllerTest.PUT /v1/guardians/me/data-settings를 CO_GUARDIAN
- [x] #203 [web] Idempotency-Key는 SES-02 · 04~07 · 11~13, SIG-03 · 05, CALL-01~04에만 요구되고 SIG-01 · 02 · 04와 조회 · OUT API에는 요구되지 않는다 ← PickupIdempotencyKeyTest.SIG-01 POST vehicle-telemetry
- [x] #204 [기반] 모듈 연결 out 포트의 어댑터가 있다: LoadSchedulePort는 schedule, LoadSpotPort는 place, LoadFamilyPort는 user, LoadChildReadinessPort는 auth의 in 포트로 읽고, RecordNotificationPort는 notification 구현 전 임시로 빈 목록을 돌려준다
- [x] #205 [기반] 공통 오류 본문은 도메인 오류가 error 안에 추가 키(unmet · missing · nextAllowedAt)를 더할 수 있고, STATE_VERSION_CONFLICT는 error와 나란히 session에 최신 세션 뷰를 싣는다
- [x] #206 [기반] PickupErrors는 API 명세의 픽업 오류 코드마다 상태 · code · 한국어 메시지를 담은 예외를 만든다
- [x] #207 user · auth · place의 진행 중 픽업 확인 어댑터가 임시 false · 빈 값 대신 pickup ActivePickupQueryUseCase를 부른다 ← AuthModuleLinkAdaptersTest.auth의 CheckActivePickupPort.hasActivePickup은 pickup ActivePickupQueryUseCase.hasActivePickup 결과를 그대로 돌려준다
- [x] #208 [기반] GuardianDataSetting 엔티티는 userId PK · learningConsent로 매핑되고 PickupOutcome 엔티티는 correctionNote VARCHAR(200)을 갖고, out 포트에 LoadDataSettingPort · SaveDataSettingPort가 있다
- [x] #209 [persistence] GuardianDataSetting을 저장하고 읽으면 learningConsent가 같고 행이 없는 보호자는 빈 값을 돌려준다 ← GuardianDataSettingPersistenceAdapterTest.learningConsent true
- [x] #210 [기반] SessionEvent 도메인은 eventId · sessionId · seq · type(SessionEventType 19종) · stateVersion · occurredAt · payload를 담고 restore로 다시 만들어진다
- [x] #211 [기반] PickupOutcome 도메인은 완료 때 completedAt · boardingMethod · predictedStopAt · actualStopAt으로 만들어지고, 정정은 actualStopAt을 두고 correctedStopAt · correctionReason · correctionStatus REQUESTED · correctedBy · correctionNote를 채우며, diffSeconds는 둘 중 하나가 없으면 비어 있다
- [x] #212 [기반] GuardianDataSetting 도메인은 행이 없으면 learningConsent false로 만들어지고 동의 값을 바꿀 수 있다
- [x] #213 [기반] Call은 markDelivered로 DELIVERED · deliveredAt을 채우고, 모든 필드를 restore로 되살리며, REVOKED인 Call에 늦게 온 markSent · markDelivered는 무시한다
- [x] #214 [기반] PickupSession은 childUserId로 자녀를 바꾸는 진입점(호출됨 CALLED · 철회로 WAITING과 위치 · acknowledgedAt 비움 · 확인 MEETING · 준비 시간 추가 · 위치 갱신)을 갖고, 위치 갱신 밖의 변경은 stateVersion을 1 올리며, 세션에 없는 자녀면 CHILD_NOT_IN_SESSION으로 거절된다
- [x] #215 [기반] PickupSession은 markStopped · markDeparted로 vehiclePhase를 STOPPED · DEPARTED로, holdCall · releaseCallHold로 callHeld를 바꾸고 stateVersion을 1 올린다
- [x] #216 [기반] 끝난 세션의 hold · releaseHold · complete · detectBoarding · openIncident는 SESSION_ALREADY_FINISHED로 거절되고, releaseHold는 열린 예외의 incidentResolvedAt을 채우며, 탑승 인식 전 rejectBoarding은 REJECTION_WINDOW_CLOSED로 거절된다
- [x] #217 [기반] PickupSession은 familyId를 start · restore로 담고 pickup_session은 family_id 컬럼을 가지며, LoadSessionPort에 loadActiveBySpot · LoadCallPort에 loadLatestCall(REVOKED 포함) · LoadFamilyPort에 loadMember(userId)가 있다
- [x] #218 [기반] PickupErrors는 CHILD_NOT_CALLED 409를 만들고 PickupViolation CHILD_NOT_IN_SESSION을 NOT_FOUND로 바꾼다
- [x] #219 [web] 세션을 바꾸는 요청 본문에 stateVersion이 없으면 422 VALIDATION_FAILED(fields stateVersion)를 돌려준다 ← PickupSessionControllerTest.POST "/v1/pickup-sessions/{sessionId}/boarding-rejectio
- [x] #220 [서비스] 호출 발송 · 전달 · 철회 · 확인은 세션 행을 잠근 뒤 Call을 다시 읽어, 그 사이 철회된 호출을 SENT · DELIVERED · ACKNOWLEDGED로 덮어쓰지 않는다 ← RevokeCallServiceTest.호출 철회는 세션 행을 잠근 뒤
- [x] #221 [서비스] isCallAcknowledged는 Call이 ACKNOWLEDGED면 true, 아니면 false를 돌려준다 ← CallProgressServiceTest.isCallAcknowledged는 Call이 ACKNOWLEDGED면
- [x] #222 [서비스] 세션 보류는 ACTIVE를 ON_HOLD로, 보류 해제는 ON_HOLD를 ACTIVE로 바꿔 저장하고 세션 뷰를 돌려준다 ← HoldSessionServiceTest.세션 보류는 잠가 읽은 ACTIVE 세션
- [x] #223 [서비스] 이벤트 목록은 세션을 볼 수 있는 주체에게 afterSeq 뒤 이벤트를 limit개와 hasMore로 돌려준다 ← ListSessionEventsServiceTest.이벤트 목록은 afterSeq 뒤 남은
- [x] #224 [서비스] 진행 중 픽업 확인(ActivePickupQueryUseCase)은 진행 중(ACTIVE · ON_HOLD) 세션이 있는지를 포트 시그니처대로 돌려준다 ← ActivePickupQueryServiceTest.sessionStatus ON_HOLD
- [x] #225 [web] 요청 필수 값(SES-02 date, SES-04 · 05 spotId, OUT-03 correctedStopAt, SIG-02 eventId · type · occurredAt)이 없거나 reason이 100자를 넘으면(SES-06 · 07, CALL-03) 422 VALIDATION_FAILED를 돌려준다 ← PickupSessionControllerTest."SES-06 reason 101자"
- [x] #226 [서비스] 자동 완료 스케줄러는 10초마다 completeExpiredBoardings를 불러, 취소 창이 지나고 모든 자녀가 탄 세션을 DOOR로 완료한다(실제 DB 통합 테스트) ← AutoCompleteBoardingSchedulerTest.completeExpiredBoardings에는 @Scheduled
- [x] #227 [서비스] 호출 철회가 커밋된 뒤 다른 트랜잭션의 markCallSent · acknowledge가 세션 잠금을 얻으면 DB의 최신 Call을 읽어 REVOKED를 덮어쓰지 않는다(두 트랜잭션 통합 테스트) ← CallProgressServiceIntegrationTest.markCallSent가 첫 loadCall 뒤 세션 잠금을
- [x] #228 [서비스] 픽업 시작은 다른 가족의 scheduleId가 섞이면 NOT_FOUND로 거절한다 ← StartPickupServiceTest.픽업 시작은 다른 가족
- [x] #229 [서비스] 픽업 시작은 오늘 요일이 아닌 일정 · 없는 scheduleId · 같은 자녀의 일정 2개가 섞이면 거절한다(500이 나지 않는다) ← StartPickupServiceTest.픽업 시작은 오늘 요일이 아닌 일정이 섞이면
- [x] #230 [서비스] 장소 변경은 요청 spotVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절한다 ← ChangeSpotServiceTest.장소 변경은 stateVersion이 같아도 요청 spotVersion
- [x] #231 [서비스] 차량 ETA 수신은 끝난(COMPLETED · CANCELLED) 세션이면 ETA를 저장하지 않고 발행하지 않는다 ← ReceiveTelemetryServiceTest.세션 CANCELLED
- [x] #232 [web] SIG-05 type이 NEED_HELP · CANT_FIND_CAR · CONTACT_PARENT 밖이거나 SIG-02 eventId가 UUID가 아니거나 type이 STOPPED · DEPARTED · STOP_FAILED · DOOR_CYCLE 밖이면 422 VALIDATION_FAILED를 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/vehicle-events에 UUID
- [x] #233 [서비스] 알림 기록 · 실시간 이벤트의 받는 사람 「보호자」는 가족의 GUARDIAN · CO_GUARDIAN · DELEGATE이고 TEACHER는 빠진다 ← SessionReceiversTest.보호자와 세션 자녀에게 보낼 때 받는
- [x] #234 [서비스] 탑승 예외를 열면 대상 자녀를 세션에 저장하고(pickup_session.incident_child_user_id), 세션 뷰 incident에 type · childUserId · openedAt을 채운다 ← GetSessionServiceTest.세션 조회는 자녀 주체에게도 열린 예외의
- [x] #235 [서비스] 세션 보류 · 해제는 SESSION_HELD · SESSION_RESUMED를 저장 · 발행하고, 해제 뒤 저장된 ETA로 호출 판정을 다시 돌린다 ← HoldSessionServiceTest.세션 보류는 SESSION_HELD를 저장한 세션의
- [x] #236 [서비스] 실시간 이벤트 payload의 시각 값(etaAt · predictedStopAt · callNotBefore · nextAllowedAt 등)은 +09:00 문자열로 담긴다 ← ExtendPrepTimeServiceTest.PREP_TIME_EXTENDED payload의 callNotBefore는
- [x] #237 [web] SES-05 spotVersion이 없으면 422 VALIDATION_FAILED(fields spotVersion)를 돌려준다 ← PickupSessionControllerTest."SES-05 spotVersion 없음"
- [x] #238 [서비스] 탑승 예외의 childUserId가 세션 자녀가 아니면 NOT_FOUND로 거절하고 저장하지 않는다 ← OpenIncidentServiceTest.탑승 예외는 childUserId가 세션 자녀가 아니면(같은 가족 자녀여도) NOT_FOUND로 거절하고 아무것도 저장 · 발행하지 않는다
- [x] #239 [서비스] 끝난(COMPLETED · CANCELLED) 세션에 경로 확정 · 장소 변경 · 준비 시간 추가 · 도움 요청이 오면 SESSION_ALREADY_FINISHED로 거절하고 저장 · 알림 · 발행하지 않는다 ← ConfirmRouteServiceTest.세션 COMPLETED
- [x] #240 [서비스] 완료된 세션에 온 차량 이벤트 DOOR_CYCLE은 오류 없이 무시하고 eventId는 처리한 것으로 남긴다 ← ReceiveVehicleEventServiceTest.완료(COMPLETED)된 세션에 온 DOOR_CYCLE은
- [x] #241 SessionEventType은 SESSION_HELD · SESSION_RESUMED를 더한 21종이다 ← SessionEventTest.SessionEventType은 설계서 「실시간 이벤트」 표의 이름 21개(19종 + SESSION_HELD · SESSION_RESUMED)를 enum 선언 순서대로 갖는다
- [x] #242 [web] POST /v1/pickup-sessions에 null이 든 scheduleIds([null] · [801, null])를 보내면 500이 아니라 422 VALIDATION_FAILED와 fields를 돌려주고 유스케이스를 부르지 않는다 ← PickupSessionControllerTest.본문 "{\"scheduleIds\":[null]
- [x] #243 [web] POST /v1/pickup-sessions/{sessionId}/completion에 null이 든 notBoardedChildUserIds([null] · [131, null])를 보내면 422 VALIDATION_FAILED와 fields를 돌려주고 유스케이스를 부르지 않는다 ← PickupSessionControllerTest.본문 "{\"mode\":\"MANUAL\",\"stateVersion\":15,\"notBoardedChildUserIds\":[null]
- [x] #244 [web] POST /v1/pickup-sessions/{sessionId}/child-locations에 범위 밖 lat(-90~90 밖) · lng(-180~180 밖)를 보내면 500이 아니라 422 VALIDATION_FAILED와 fields를 돌려주고 유스케이스를 부르지 않는다 ← PickupSignalControllerTest."lat 1000"
- [x] #245 [서비스] 차량 이벤트 DEPARTED를 받으면 철회되지 않은 호출을 철회(revokeReason VEHICLE_DEPARTED)하고 세션 자녀를 WAITING으로 돌리고 위치를 비우고 CALL_REVOKED를 저장 · 발행한다 ← ReceiveVehicleEventServiceTest.차량 이벤트 DEPARTED로 호출을 철회하면 세션 자녀를 모두 WAITING으로 돌리고
- [x] #246 [서비스] 차량 이벤트 STOP_FAILED를 받으면 철회되지 않은 호출을 철회(revokeReason STOP_FAILED)하고 CALL_REVOKED를 저장 · 발행하며, 호출이 없으면 아무것도 바꾸지 않는다 ← ReceiveVehicleEventServiceTest.차량 이벤트 STOP_FAILED로 호출을 철회하면 CALL_REVOKED만 저장하고
- [x] #247 [서비스] 차량 이벤트 STOPPED를 받으면 저장된 최신 ETA로 합류 호출 판정을 다시 돌려 조건이 맞으면 호출을 만든다 ← ReceiveVehicleEventServiceTest.차량 이벤트 STOPPED는 저장된 ETA가 180초 안이고 가드를 통과하는 AUTO 가족 세션이면
- [x] #248 [기반] Guard는 vehiclePhase가 STOPPED가 아니면 G6을 unmet에 넣는다 ← GuardTest.vehiclePhase ROUTING
- [x] #249 [서비스] 정차 전(vehiclePhase STOPPED 아님)에는 트리거와 G1~G5를 만족해도 자동 호출을 만들지 않고, 합류 안내 보내기(CALL-02)는 422 CALL_GUARD_UNMET(unmet에 G6)로 거절한다 ← SendCallServiceTest.가드 G6 미충족
- [x] #250 [기반] PickupSession.applyTelemetry는 vehiclePhase가 ROUTING일 때 remainingMeters ≤ 700 또는 navEtaSeconds ≤ 60이면 APPROACHING으로 바꾸고, 그 밖의 단계에서는 바꾸지 않는다 ← PickupSessionTest.applyTelemetry는 observedAt이 마지막 값보다 오래되면
- [x] #251 [서비스] 차량 ETA 수신으로 APPROACHING이 되면 VEHICLE_APPROACHING을 세션당 한 번 저장하고 차량 · 보호자 · 자녀에게 발행한다 ← ReceiveTelemetryServiceTest.차량 ETA 수신으로 APPROACHING이 되면 저장한 VEHICLE_APPROACHING을
- [x] #252 SessionEventType은 VEHICLE_APPROACHING · PREPARE_NOTICE_SENT · CHILD_READY를 더한 24종이다 ← SessionEventTest.SessionEventType은 VEHICLE_APPROACHING · PREPARE_NOTICE_SENT · CHILD_READY를 더한 24종
- [x] #253 [서비스] 신호 끊김 철회는 철회되지 않은 호출이 있는 ACTIVE 세션 중 etaUpdatedAt이 1분 넘게 지난 세션의 호출을 철회(revokeReason SIGNAL_LOST)하고 세션 자녀를 WAITING으로 돌리고 CALL_REVOKED를 저장 · 발행한다 ← RevokeSignalLostCallsServiceTest.신호 끊김 철회로 호출을 철회하면 세션 자녀를 모두 WAITING으로 돌리고
- [x] #254 [서비스] 신호 끊김 철회 스케줄러는 pickup.call.signal-lost-check-interval(기본 PT15S)마다 신호 끊김 철회를 부른다(실제 DB 통합 테스트) ← RevokeSignalLostCallsSchedulerTest.신호가 1분 넘게 끊기고 철회되지 않은 호출이 있는 ACTIVE 세션은 스케줄러 실행 뒤
- [x] #255 [서비스] 자녀 위치는 가족의 자녀 위치 사용(childLocationEnabled)이 꺼져 있으면 403 LOCATION_CONSENT_REQUIRED로 거절한다(LoadChildReadinessPort.hasLocationConsent가 user의 가족 설정을 읽음) ← ReportChildLocationServiceTest.자녀 위치는 가족의 자녀 위치 사용이 꺼져 있으면 403 LOCATION_CONSENT_REQUIRED로 거절한다
- [x] #256 [서비스] 차량 ETA 수신에서 remainingMeters ≤ 700 또는 navEtaSeconds ≤ 180이 처음 되면 세션 자녀에게 실내 준비 알림(Notification type PREPARE) 기록을 요청하고 PREPARE_NOTICE_SENT를 세션당 한 번 저장해 차량 · 보호자에게 발행한다 ← ReceiveTelemetryServiceTest.차량 ETA 수신에서 준비 알림 기준(700m · 180초)이 처음 되면
- [x] #257 [기반] SessionChild.markReady는 readyAt을 기록하고 여러 번 불러도 첫 시각을 유지한다 ← SessionChildTest.SessionChild.markReady를 두 번 부르면 첫 readyAt이 유지된다
- [x] #258 [서비스] 준비 완료(SIG-06)는 세션 자녀의 readyAt을 기록하고 CHILD_READY(payload childUserId)를 저장해 차량 · 보호자에게 발행하며, 끝난 세션이면 SESSION_ALREADY_FINISHED, 세션 자녀가 아니면 NOT_FOUND로 거절한다 ← MarkChildReadyServiceTest.준비 완료는 세션 행을 잠그고 그 자녀의 readyAt에
- [x] #259 [web] POST /v1/pickup-sessions/{sessionId}/ready는 자녀 토큰으로 200과 childUserId · readyAt을 돌려주고, 차량 · 보호자 토큰이면 403 FORBIDDEN을 돌려준다 ← PickupSignalControllerTest.POST /v1/pickup-sessions/{sessionId}/ready는 자녀 토큰으로
- [x] #260 [서비스] 세션 조회의 children[]는 readyAt을 담는다 ← GetSessionServiceTest.세션 조회는 자녀 주체에게도 자기 children[].readyAt을 준다
- [x] #261 [서비스] 차량 ETA 수신은 SIG-01 location을 세션의 마지막 차량 위치(vehicleLat · vehicleLng · vehicleLocatedAt)로 저장하고 observedAt이 오래되면 무시한다 ← ReceiveTelemetryServiceTest.차량 ETA 수신은 SIG-01 location을 세션의 마지막 차량 위치 vehicleLocation으로
- [x] #262 [persistence] pickup_session의 vehicle_lat · vehicle_lng · vehicle_located_at을 저장하고 읽는다 ← PickupSessionPersistenceAdapterTest.마지막 차량 위치가 있는 세션을 저장하면
- [x] #263 [서비스] 세션 조회는 vehicleLocation을 차량 · 보호자에게는 늘, 자녀에게는 remainingMeters ≤ 700 또는 남은 시간 ≤ 180초일 때만 주고, 끝난 세션이면 null로 준다 ← GetSessionServiceTest.남은 거리 "700"m · 도착까지 "720"초 → 자녀에게 보임 "true"
- [x] #264 [서비스] 끝난 세션(COMPLETED · CANCELLED)이거나 탑승을 인식한(boardingDetectedAt 있음) 세션에는 차량 이벤트 DEPARTED · STOP_FAILED와 신호 끊김 철회가 호출을 철회하지 않고 자녀 상태 · acknowledgedAt을 바꾸지 않는다(DEPARTED는 vehiclePhase · VEHICLE_DEPARTED만 남김) ← RevokeSignalLostCallsServiceTest.신호 끊김 철회는 잠가 다시 읽은 ACTIVE 세션에 boardingDetectedAt이 있으면(탑승 인식) 호출을
- [x] #265 [서비스] ON_HOLD 세션에서는 준비 알림 기준을 넘어도 실내 준비 알림을 보내지 않고, 보류가 풀린 뒤 첫 차량 ETA 수신에서 기준 안이면 보낸다 ← ReceiveTelemetryServiceTest.ON_HOLD 세션은 준비 알림 기준을 넘어도 PREPARE 알림 기록 요청 · PREPARE_NOTICE_SENT를 하지 않고
- [x] #266 [서비스] 탑승 인식 없이 MANUAL로 완료된 세션과 CANCELLED 세션에 DEPARTED · STOP_FAILED가 와도 살아 있는 호출을 철회하지 않는다(boardingDetectedAt이 비어 있어도 끝난 세션이면 철회 안 함) ← ReceiveVehicleEventServiceTest.MANUAL로 완료된 세션(boardingDetectedAt 없음) · 호출 SENT

## 미완료 항목
- 없음

## 완료 기준 (공통 규칙 문서)
- [x] C1 설계서의 테스트 케이스가 모두 GREEN
- [x] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [x] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [x] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 2026-11-13 16:14 KST · 도메인 리뷰어 · 결론 통과 · 상 0 · 중 2 · 하 4 · 리뷰 시점 체크 266/266
- 리뷰 시점 테스트: ● GREEN · 2588 passed · 전체 · 275s · .\gradlew.bat test --console=plain
  - 결론: 통과
  - 직전 리뷰의 [중] 2건은 모두 해소됐다. 동작을 바꿔야 하는 [상] · [중]은 없다. 직전 리뷰 뒤 바뀐 파일은 ReceiveVehicleEventServiceTest.java, docs/08, docs/followup/pickup.md, TODO.md뿐이고 main 코드는 바뀌지 않았다.
  - 이전 지적 처리
  - [중] MANUAL · 즉시 완료 · CANCELLED 뒤 출발 테스트 없음 → 해소. ReceiveVehicleEventServiceTest.java:594-657의 #266 8건이 boardingDetectedAt null을 먼저 단언해 keepsCalls의 isFinished() 조건을 고정한다.
  - [중] 08 핵심 로직 본문에 #264 규칙 없음 → 해소. 08 L290 · L291 · L294가 코드(ReceiveVehicleEventService.java:88 · 102, RevokeSignalLostCallsService.java:89-92)와 맞다.
  - [하] 5건 → FOLLOWUP 「1차 개발 뒤 정리」에 기록.
  - 발견 사항
  - [하] 08 「확인 필요」 표 L739(SES-06 · 07 행 「이벤트 21종」)도 옛 값(지금 24종). FOLLOWUP 메모 (3)에 L739를 함께 적기 → note
  - [하] ba90e2f 뒤 작업이 모두 미커밋(수정 66개, user 도메인 변경 섞임), 관계없는 contest-application-consent.docx · iLink_Project_Package/ 있음 → note(커밋 때 도메인별로 나누고 두 항목 제외)
  - [하] #266은 MANUAL 완료만 다루고 IMMEDIATE는 없음. 경로가 isFinished() 하나라 지금은 문제없음 → note
  - 완료 기준: C1 충족(266/266, GREEN 2588) / C2 충족 / C3 충족 / C4 충족
  - 테스트: scope all GREEN 2588 passed · 275s
- 이전 리뷰: 2026-11-13 16:06 KST (조건부 통과), 2026-11-13 15:51 KST (수정 필요), 2026-11-13 10:38 KST (조건부 통과), 2026-11-13 10:19 KST (조건부 통과)

## 확인 필요 사항 (설계서)
| 항목 | 현재 가정 |
|---|---|
| 「같은 시간대」 묶기 기준(SES-02) | 소스에 없음. 일정의 classEndTime + targetPickup(목표 픽업 시각)이 같으면 같은 시간대로 가정 |
| 「진행 중인 같은 세션」(SES-02 200) | 같은 차량 · 같은 date · 같은 scheduleIds 묶음으로 가정. 다르면 409 ANOTHER_SESSION_ACTIVE |
| DRAFT 일정으로 시작할 때 오류 코드 | 소스는 「픽업 시작 불가」만 있음. 422 SETUP_INCOMPLETE로 가정 |
| 일정 값 스냅샷 컬럼 | 소스는 schedule 값도 스냅샷한다고 했지만 SessionChild에는 scheduleId만 있음. 지연 판정(목표 시각 + 10분)에 쓸 목표 시각을 어디서 읽을지 정해야 함 |
| 합류 안내 방식 스냅샷 | SET-06은 「다음 세션부터 적용」인데 PickupSession에 mode 컬럼이 없음. 지금은 판정 때 Family.joinCallMode를 읽는다고 가정(진행 중에 바뀌면 바로 반영됨) |
| G3 「경로 일치」의 판정 | 소스에 정의 없음. SES-04로 확정한 장소 버전과 지금 spotVersion이 같으면 통과로 가정 |
| G5와 형제 | Call은 세션에 하나라 모든 세션 자녀의 callNotBefore가 지나야 통과로 가정. callNotBefore가 지금과 같은 시각이면 지난 것으로 본다(「이 시각 전에는 호출 안 함」) |
| ETA_3MIN · DIST_700M이 동시에 맞을 때 triggerType | 소스에 없음. ETA_3MIN으로 가정 |
| vehiclePhase APPROACHING 기준 | [사람 결정 2026-11-13] remainingMeters ≤ 700 또는 navEtaSeconds ≤ 60 |
| spotStatus ROUTING · INVALIDATED로 바꾸는 계기 | 이 상태로 바꾸는 API가 없음. 정해야 함 |
| navRouteId 저장 위치 | SES-04 요청에 있으나 컬럼이 없음. 저장하지 않는다고 가정 |
| SES-05 spotVersion 불일치 오류 | 소스에 없음. 409 STATE_VERSION_CONFLICT로 가정 |
| SES-05에서 CALL_ALREADY_SENT와 CHILD_ALREADY_MOVING이 함께 맞을 때 | MEETING이면 호출도 보낸 상태. CHILD_ALREADY_MOVING을 먼저 본다고 가정 |
| SES-06 · 07 reason 저장 위치 | [사람 결정 2026-11-09] 세션 보류 · 해제는 이벤트 SESSION_HELD · SESSION_RESUMED로 남기고(이벤트 21종, 받는 쪽 차량(SSE) · 보호자) 해제 뒤 저장된 ETA로 호출 판정을 다시 돌린다. 세션에 사유 컬럼이 없어 보류 reason은 저장하지 않고, 취소 reason은 자동 철회 Call.revokeReason에 넣는다 |
| CALL-02 응답 status | [사람 결정 2026-11-09] 응답 시점의 실제 상태 ACCEPTED를 준다. 발송(SENT)은 CALL_SENT 이벤트로 알린다 |
| Call SENT를 기록하는 주체 | notification이 FCM 성공 뒤 pickup in 포트(markCallSent)를 부른다고 가정 |
| CALL-02가 트리거(180초 · 700m)도 검사하는지 | 소스에는 가드 미충족 오류만 있음. 가드만 검사한다고 가정 |
| CALL_GUARD_UNMET의 unmet 위치 | 오류 형식에 unmet 칸이 없음. error 안에 unmet 배열을 더한다고 가정 |
| CALL-03으로 ACCEPTED(발송 전) 호출을 철회할 수 있는지 | 소스는 「발송 후」만 말함. ACCEPTED도 철회 가능으로 가정 |
| CALL-03 때 MEETING 자녀 | 상태 다이어그램은 CALLED → WAITING만 있음. 4.14대로 MEETING도 WAITING으로 돌린다고 가정 |
| CALL-04 = MEETING | 4.9는 「열람 ≠ 이동 시작」, 3장 · 9.6은 CALL-04로 MEETING. 3장 · 9.6을 따름 |
| boardedAt 기록 시점 | 플로우차트는 「창이 지나면 기록」, SES-12는 「boardedAt을 비움」. DOOR_CYCLE 때 기록하고 SES-12가 비운다고 가정 |
| SES-12 뒤 예외 열기 | 플로우차트는 SES-12 다음에 SES-13 예외로 이어짐. 서버가 자동으로 열지 않고 차량 앱이 SES-13을 부른다고 가정 |
| SES-11 IMMEDIATE를 탑승 인식 전에 누른 경우 | 소스에 없음. 탑승하지 않은 자녀가 있으면 422 CHILDREN_NOT_ALL_BOARDED로 가정 |
| 완료 뒤 · 주행 중 SES-13 | [사람 결정 2026-11-09 「끝난 세션의 동작」에 따름] 완료 · 취소 세션에서는 예외를 열 수 없다(409 SESSION_ALREADY_FINISHED) |
| 끝난 세션의 차량 정차 · 출발, 호출 보류 | [팀장 결정 2026-11-09] markStopped · markDeparted는 끝난 세션에서도 받는다(완료 뒤 출발이 정상 흐름). holdCall · releaseCallHold는 끝난 세션이면 409 SESSION_ALREADY_FINISHED |
| incident의 childUserId 저장 | [사람 결정 2026-11-09] pickup_session.incident_child_user_id 칸에 저장하고 세션 뷰 incident.childUserId는 거기서 읽는다(INCIDENT_OPENED payload에서 읽지 않음) |
| 예외 해소(incidentResolvedAt) 방법 | 해소 API가 없음. SES-06 DELETE(보류 해제) 때 채운다고 가정 |
| actualStopAt · predictedStopAt 값 | actualStopAt은 VEHICLE_STOPPED occurredAt. [사람 결정 2026-11-09] predictedStopAt은 SIG-02 STOPPED를 받을 때의 세션 etaAt(정차 직전 마지막 ETA)이고 VEHICLE_STOPPED payload에 담아 완료 때 읽는다. etaAt이 없으면 null |
| SIG-02 STOP_FAILED 처리 | [사람 결정 2026-11-13] 상태는 그대로, 살아 있는 호출이 있으면 자동 철회(STOP_FAILED). 끝난 세션 · 탑승 인식 뒤에는 철회 안 함 |
| SIG-02 eventId 저장 위치 | 「Idempotency-Key로 써서 중복 무시」. ~~idempotency_key 테이블에 저장한다고 가정~~ → 아래 「SIG-02 eventId와 idempotency_key 테이블」 결정(ProcessedVehicleEvent)으로 대체 |
| 자녀에게 장소를 숨기는 「SES-04 전」 | [팀장 결정 2026-11-09, 사람 확인 전] 경로 확정을 한 번도 하지 않은 상태(routeVersion == 0)로 본다. 첫 확정 때 자녀에게 출발 알림이 가므로 SES-05로 장소가 다시 SELECTED가 돼도 자녀에게 다시 숨기지 않는다 |
| SIG-04 WAITING · 철회 뒤 요청의 오류 코드 | [사람 결정 2026-11-09] 끝난 세션은 SESSION_ALREADY_FINISHED, 그 밖(WAITING · 철회 뒤)은 409 CHILD_NOT_CALLED |
| SES-04 spotId가 세션 장소와 다를 때 | [사람 결정 2026-11-09] 409 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절. 장소 변경은 SES-05로만 |
| 자녀 단위 변경과 stateVersion | [사람 결정 2026-11-09] 호출됨 · 확인 · 준비 시간 · 철회로 자녀 상태가 바뀌면 stateVersion +1. 자녀 위치(SIG-04) · ETA(SIG-01)는 올리지 않음 |
| 철회 때 acknowledgedAt | [사람 결정 2026-11-09] 철회하면 자녀 acknowledgedAt도 비워 새 호출에서 다시 확인 |
| 탑승 인식 전 SES-12 | [사람 결정 2026-11-09] 409 REJECTION_WINDOW_CLOSED |
| 끝난 세션의 동작 | [사람 결정 2026-11-09] COMPLETED · CANCELLED 세션의 보류 · 보류 해제 · 완료 · 탑승 인식 · 예외 열기는 409 SESSION_ALREADY_FINISHED |
| 철회된 호출에 늦게 온 발송 · 수신 확인 | [사람 결정 2026-11-09] 무시(상태 변화 없음) |
| CALL_ALREADY_SENT의 「이미 보냄」 | [사람 결정 2026-11-09] 철회되지 않은 호출이 있으면(ACCEPTED 포함) |
| 예외 해소 | [사람 결정 2026-11-09] 보류 해제(SES-06 DELETE) 때 열린 예외의 incidentResolvedAt을 채움 |
| 세션에 없는 childUserId | [팀장 결정 2026-11-09] 404 NOT_FOUND |
| SessionEvent.type | [팀장 결정 2026-11-09] 도메인에서는 enum(SessionEventType), 저장은 문자열. [사람 결정 2026-11-09] SESSION_HELD · SESSION_RESUMED가 더해져 19종 → 21종(DB 저장 안 하는 ETA_UPDATED · VEHICLE_LOCATION 포함) |
| 받는 사람 「보호자」 | [사람 결정 2026-11-09] 가족의 GUARDIAN · CO_GUARDIAN · DELEGATE. TEACHER는 뺀다(알림 기록 · 실시간 이벤트 모두) |
| 이벤트 payload의 시각 값 | [사람 결정 2026-11-09] etaAt · predictedStopAt · callNotBefore · nextAllowedAt 등은 +09:00 문자열 |
| 끝난 세션의 SES-04 · SES-05 · SIG-03 · SIG-05 | [사람 결정 2026-11-09] 409 SESSION_ALREADY_FINISHED로 거절하고 저장 · 알림 · 발행하지 않음 |
| 완료 뒤 SIG-02 DOOR_CYCLE | [사람 결정 2026-11-09] 202로 받고 무시한다. markProcessed로 eventId는 남김 |
| 자동 완료 스케줄러 | [사람 결정 2026-11-09] adapter/in/scheduler, 주기 pickup.session.auto-complete-interval(기본 PT10S), 세션마다 트랜잭션 |
| 세션 뷰 조립 위치 | [팀장 결정 2026-11-09] SessionView는 port.in record라 application/service에서 조립(「구조」 절의 domain 조립 규칙 대신) |
| 위치 동의 저장 위치 | 동의 테이블이 없음(05_인증-auth.md에도 기록) |
| SIG-01이 끝난 세션에 올 때 | 소스에 없음. 202로 받고 무시한다고 가정 |
| 세션 행 잠금 범위 | 소스는 SIG-05만 명시. 세션을 바꾸는 모든 요청을 loadForUpdate로 처리한다고 가정. [사람 결정 2026-11-09] 잠금 뒤 Call을 DB에서 다시 읽는다(영속성 컨텍스트 refresh). CallEntity에 @Version이 없어 잠금 없이 saveCall을 부르는 경로가 생기면 낙관적 잠금 추가 검토 |
| call.scheduledAt 계산 | 예시는 ETA − 3분(18:12 → 18:09). 자녀 callNotBefore 중 가장 늦은 값보다 앞서지 않게 한다고 가정 |
| 완료 세션 뷰의 nextPickup | 5.3은 「완료 세션에 nextPickup 포함」이나 세션 뷰 필드에 없음. 필드 모양을 정해야 함 |
| 허용 실외 대기 5분 · 지연 10분의 실행 시점 | 별도 타이머가 없고 세션 뷰에 지연 필드도 없음. 실행 방법과 응답 필드를 정해야 함 |
| Notification 행을 만드는 이벤트 | Notification type이 SESSION_STARTED · DEPARTURE · CALL · PREP_EXTENDED · HELP 등. 이 다섯은 행을 만들고 나머지 이벤트는 행 없이 FCM 데이터 메시지만 보낸다고 가정 |
| DEPARTURE_NOTICE_SENT 기록 시점 | SES-04 커밋 때 저장한다고 가정(실제 FCM 발송은 커밋 뒤) |
| SSE에서 저장 안 하는 ETA_UPDATED의 id | seq가 없음. id 줄을 빼고 보낸다고 가정 |
| SSE 연결이 여러 개일 때 | 같은 세션에 차량 연결이 둘 이상이면 모두에 보낸다고 가정 |
| OUT-02 · 03을 완료 전 세션에 부를 때 | PickupOutcome이 없음. 404 NOT_FOUND로 가정 |
| OUT-03 note 저장 위치 | [사람 결정 2026-11-09] PickupOutcome.correctionNote VARCHAR(200) |
| OUT-04 learningConsent · 파기 대기 저장 위치 | [사람 결정 2026-11-09] pickup 모듈의 GuardianDataSetting(userId PK, learningConsent). 「파기 대기」 처리는 후속 과제 |
| 오류 본문의 추가 값 위치 | unmet · missing · nextAllowedAt은 error 안의 키(schedule overlapScheduleIds와 같은 방식). STATE_VERSION_CONFLICT의 최신 세션 뷰는 error와 나란히 session(03 가정) |
| 문서 크기 | 엔드포인트 28개 · 항목이 많음. 소스의 모듈 경계(pickup 한 패키지)를 따라 한 문서로 둠. 나눌지 팀장이 정함 |
| SIG-02 eventId와 idempotency_key 테이블 | [팀장 결정 2026-11-09, 사람 확인 전] idempotency_key와 구조가 달라 pickup 전용 테이블 ProcessedVehicleEvent(eventId VARCHAR(36) PK, sessionId BIGINT, createdAt)에 저장한다. markProcessed는 이 PK 충돌로 중복을 판정 |
| SES-08 SSE 요청 로그 · 비동기 실행기 | 03 요청 로깅은 SSE를 연결(stream=open) · 종료(stream=closed) 두 줄로 남김. 타임아웃 · 연결 끊김 · completeWithError 때 종료 줄의 status · errorCode와 MVC 비동기 실행기 설정 필요 여부를 SES-08 때 정해야 함 |

## 다음 단계
- 커밋 · 푸시: /tdd commit (기능별 분할 커밋) · /tdd push (커밋 후 기능 브랜치 푸시)
- 다음 도메인: 알림 (09_알림-notification.md). 시작: /tdd domain notification

## 후속 과제
- [팀장 결정 · 설정 키] 픽업 기준값은 application.yml `pickup.session.*` 하위 키로 둠(`pickup:` 아래는 이미 async · idempotency · jwt 등 앱 전체 설정). 롤백되면 저장하지 않는 이벤트(ETA_UPDATED)도 커밋 뒤 발행하지 않음
- [service lane #94] AutoCompleteBoardingService에 테스트 전용 package-private 4인자 생성자가 남음(구현자는 테스트 생성 코드를 못 고침) — AutoCompleteBoardingServiceTest 생성 코드를 5인자(TransactionOperations)로 바꾸고 생성자를 하나로 정리할 것. 자동 완료 한 세션 실패 시 건너뛰기 테스트 없음, PickupProperties yml 바인딩 확인 테스트 없음
- [리뷰 거리] (1) #137 테스트 보조 SseEmitterProbe를 src/test의 org.springframework.web.servlet.mvc.method.annotation 패키지에 둠(패키지 전용 API 접근용) — 스프링 버전이 바뀌면 깨질 수 있음 (2) #207로 auth · place · user 테스트 이름이 바뀌어 auth #98 · place #44 · user #64의 체크 증거 이름과 어긋남 — 해당 도메인 문서 증거 갱신 필요 (3) 04 · 05 · 07 설계서의 「임시 어댑터」 설명(user L8 · L202, auth L19 · L266, place L8 · L19 · L153)을 실제 위임으로 고칠 것 (4) seq 없는 이벤트(ETA_UPDATED 등)는 SSE에서 id 줄 없이 보냄 — 팀장 결정
- [사람 결정 2026-11-09 · 도메인 리뷰 뒤] (1) 받는 사람 보호자 = GUARDIAN · CO_GUARDIAN · DELEGATE (2) 탑승 예외 대상 자녀는 pickup_session.incident_child_user_id 칸에 저장 (3) 세션 보류 · 해제는 SESSION_HELD · SESSION_RESUMED 이벤트를 남기고 해제 뒤 호출 판정을 다시 돌림 (4) 이벤트 payload 시각은 +09:00 문자열로 통일. 설계서 확인 필요 사항 · 이벤트 표(19종 → 21종) · 테이블 정의에 반영할 것
- [#227 후속] CallEntity에 @Version이 없어 세션 잠금 없이 saveCall을 부르는 경로가 생기면 같은 덮어쓰기가 다시 날 수 있음 — 엔티티 · 테이블에 낙관적 잠금 추가 검토. 설계서 「세션 행 잠금 안에서 처리」에 「잠금 뒤 Call을 DB에서 다시 읽는다」를 적을 것
- [사람 결정 2026-11-09 · 재리뷰 뒤] (1) 끝난 세션의 SES-04 · SES-05 · SIG-03 · SIG-05는 409 SESSION_ALREADY_FINISHED로 거절 (2) 완료 뒤 SIG-02 DOOR_CYCLE은 202로 받고 무시, markProcessed는 남김. [재리뷰 하 · 후속] 한 줄 주석 52줄(02 L78), SSE 송출 리스너 위치(adapter/in/sse vs 문서 adapter/out/event), 세션 끝나도 SSE 연결 안 닫힘, 테스트에만 쓰는 main 코드(restore 27인자 · 엔티티 생성자 오버로드 · PickupProperties.defaults), 통합 테스트 문맥의 @Scheduled, 자동 완료 실패 건너뛰기 · yml 바인딩 테스트, SIG-01 location · headingDeg · gear · SIG-04 lat · lng 범위 · SES-04 navRouteId 검증, 보류 해제 저장 ETA 계산, CALL-04 먼저 오면 MEETING→CALLED
- [세 번째 리뷰 하 · 후속] (1) 스웨거 doc 오류 응답 보강(SES-04 · 05 · SIG-03 · 05 409 SESSION_ALREADY_FINISHED, SIG-03 CALL_ALREADY_SENT, SIG-04 409 · CHILD_NOT_CALLED, SES-04 장소 다름 409, SES-13 404) (2) #158~#162 증거 이름을 SessionStreamControllerTest로 (3) SessionEventTest DisplayName · 순서 단언 정리 (4) #240 CANCELLED 테스트 추가 (5) 한 줄 주석 52줄 정리(02 L78), restore 27인자 · 2인자 openIncident 제거, 02 L55 · L60 SSE 송출 리스너 위치 문서 갱신
- [마지막 리뷰 하 · 후속] (1) 체크 증거 이름 갱신: #158~#162 → SessionStreamControllerTest(DisplayName 같음), #241 → SessionEventTest 「…enum 선언 순서대로 갖는다」 (2) user · auth · place PickupActivityAdapter 한 줄 주석 3줄 제거(02 L78) (3) 02 L72 네이티브 쿼리 예외에 ProcessedVehicleEvent markProcessed(INSERT … ON CONFLICT) 추가 (4) 04 L8 · L202 · L227, 05 L19 · L266, 07 L8 · L19 · L153 「임시 어댑터」 설명을 pickup 위임으로 갱신 (5) SseEmitterProbe 위치 유지(스프링 버전 업 때 확인). 주석 제거로 코드에서 사라진 임시 동작: auth 위치 동의 항상 true, notification 알림 빈 목록 — #204 후속 과제로 유지
- [설계 결정 필요 · 2026-11-13 재리뷰 중] SES-11 notBoardedChildUserIds에 세션에 없는 childUserId가 오면 지금은 조용히 무시하고 MANUAL이면 세션 자녀 전원을 탑승 처리해 COMPLETED로 끝낸다(되돌릴 수 없음). 08의 [팀장 결정 2026-11-09] 「세션에 없는 childUserId는 404 NOT_FOUND」(SES-12 · 13에는 적용)를 SES-11에도 적용할지 사람이 정해야 함. 차량 앱이 세션 밖 자녀 ID를 함께 보내는 경우가 있으면 404로 바꿀 때 앱이 깨질 수 있어 팀장이 정하지 않음. null 원소는 #243으로 422 처리
- [선택 · 2026-11-13 재리뷰 하] (1) SIG-04에 NaN · Infinity를 고정하는 테스트가 없다. 코드 · 라이브러리 바이트코드로는 `"lat":"NaN"` · `"lng":"Infinity"` 문자열은 422, 따옴표 없는 NaN 토큰은 400이지만 라이브러리 버전이 바뀌면 조용히 달라질 수 있다 (2) #242 · #243 테스트는 field 이름 앞부분(scheduleIds · notBoardedChildUserIds)만 본다. 앱이 field 이름으로 화면을 고르면 정확한 값(`notBoardedChildUserIds[1]` 등)과 건수 단언을 더한다 (3) 스웨거 SES-02 · SES-04 설명에 본문 검증 실패(입력값 오류) 422가 없다
- [운영 · 2026-11-13] prod(validate) 배포 전 실행: ALTER TABLE pickup_session ADD COLUMN vehicle_lat NUMERIC(9,6) NULL, ADD COLUMN vehicle_lng NUMERIC(9,6) NULL, ADD COLUMN vehicle_located_at TIMESTAMPTZ NULL; ALTER TABLE session_child ADD COLUMN ready_at TIMESTAMPTZ NULL; (user: ALTER TABLE family ADD COLUMN child_location_enabled BOOLEAN NOT NULL DEFAULT false;). [팀장 결정 2026-11-13] 자동 철회(출발 · 정차 실패 · 신호 끊김)는 끝난 세션 · 탑승 인식 뒤에는 하지 않음(#264, 기획서 「자녀가 나오기 전에 차가 다시 출발하면」). [사람 결정 필요] (1) 정차 뒤 SIG-01이 끊기면 아이가 걷는 중에도 약 75초 뒤 철회됨: 차량 앱이 정차 뒤에도 15초마다 SIG-01을 보내는지 차량 앱 담당 확인, 아니면 STOPPED 뒤 기준 완화 (2) ETA가 오래되면 remainingSeconds 0으로 자녀에게 차량 위치가 보임 (3) 자녀 LOCATION 동의는 여전히 늘 true. [후속] PREPARE 알림이 자녀 폰으로 안 나감: notification #65(09) 구현 필요. 700 · 180 기준 4곳 분산, 테스트 전용 main 오버로드 증가, SIG-01마다 loadMembers, loadSignalLost가 자녀까지 읽음, 통합 테스트 중 신호 끊김 스케줄러 15초마다 돎
- [1차 개발 뒤 정리 · 2026-11-13] (1) SES-12로 탑승을 취소한 뒤 차가 출발해도 호출이 철회되지 않음(rejectBoarding이 boardingDetectedAt을 남김, #264 문장대로) — 사람 결정 (2) findSignalLost 쿼리에 boardingDetectedAt is null 조건 추가(리팩터링) (3) 08 「확인 필요」 표의 SessionEvent.type 행 「19종 → 21종」은 지금 24종, 「허용 실외 대기 5분」 관련 행 정리 (4) 08 핵심 로직 본문에 #264 규칙 반영함(DEPARTED · STOP_FAILED · 신호 끊김)
- [1차 개발 뒤 정리 추가] 08 확인 필요 표 SES-06 · 07 행의 「이벤트 21종」도 지금 24종. IMMEDIATE 완료 뒤 출발 테스트는 MANUAL과 같은 경로(isFinished)라 생략. 커밋할 때 user · pickup · 문서로 나누고 contest-application-consent.docx · iLink_Project_Package/는 제외
- [pickup 기반 중간 정리] (1) SessionChild.extendPrep 인자 2개짜리(한도 확인 없음)는 #30 · #31 테스트를 인자 3개짜리로 옮긴 뒤 지울지 리뷰에서 결정. 한도 테스트가 15로만 불려 15를 코드에 박아도 통과함 → 다른 한도 값 테스트 추가 검토 (2) SIG-03 「호출 전 실내 대기에서만」 · 409 CALL_ALREADY_SENT는 서비스(Call 조회)에서 판정, PickupViolation에 없음 (3) PickupSession.applyTelemetry는 void라 무시됐는지 서비스가 알 수 없음 — 무시된 SIG-01 뒤 ETA_UPDATED 발행 · 호출 판정 여부를 서비스 항목에서 정할 것 (4) 기준값(ETA 180초 · 700m, ETA 유효 60초, 도움 60초, 준비 한도 15분, 탑승 취소 창 10초)은 도메인이 인자로 받음 — application.yml 키 이름은 서비스 lane에서 정함
- [pickup 기반 리뷰 때 정리] PickupSession.restore가 15 · 18 · 19인자 세 벌로 늘었음(필드가 늘 때마다 test-writer가 기존 코드를 못 고쳐 오버로드를 더함). 15 · 18인자는 routeSpotVersion 등을 null로 채워 G3이 틀리게 판정될 수 있음 → 기반 커밋 전 리뷰어가 모든 필드를 받는 restore 하나로 합치고 테스트 헬퍼를 옮길 것. SessionChild.restore도 같은 문제가 생길 수 있음
- [절차] #47 · #48 포트 구현자가 파일 92개를 Edit · Write 대신 scratchpad Python 스크립트(gen_ports.py)로 덮어써 tdd-guard 변경 추적을 거치지 않음. 변경은 src/main/.../pickup/application/port 골격만이고 대상 테스트는 GREEN 11 passed. 기반 체크 전 전체 테스트로 다시 확인할 것. 이후 에이전트 프롬프트에 「파일 수정은 Edit · Write로만」 명시
- [#205 후속] (1) BusinessException.withDetail은 새 객체가 아니라 같은 예외를 고쳐 돌려줌(던지기 전 이어 붙이는 용도). overlapScheduleIds 키는 금지 목록에 없어 conflictWithOverlap과 함께 쓰면 키가 두 번 나갈 수 있음 → 금지 키 추가 또는 overlapScheduleIds를 details로 옮기는 정리 검토 (2) ErrorCode.STATE_VERSION_CONFLICT 문구 「다른 곳에서 먼저 바뀌었습니다. 최신 상태를 확인해 주세요」는 구현자가 정함 → 사람 확인 (3) 03 공유모듈 설계서 89행(본문 키) · 230~235행(BusinessException 시그니처)에 stateVersionConflict · withDetail · details · session 반영 필요
- [#204 임시 동작] PickupChildReadinessAdapter.hasLocationConsent는 auth에 위치 동의 저장 · 조회 in 포트가 없어 임시로 항상 true(CHILD 초대 수락에 LOCATION 동의 필수라 연결된 자녀는 동의한 상태). 위치 동의 저장소가 생기면(05 「동의 저장 위치」 · 08 「위치 동의 저장 위치」) 실제 조회로 바꾸고 PickupModuleLinkAdaptersTest의 임시 단언도 바꿀 것. PickupNotificationAdapter.recordNotifications는 notification 구현 전 임시로 빈 목록 — notification 도메인 때 교체
- [#206 문구 확인] PickupErrors의 한국어 메시지 18개(NOT_TODAY 「오늘 날짜의 픽업만 시작할 수 있습니다」 등)는 설계서에 없어 test-writer가 정하고 PickupErrorsTest가 그대로 확인함 → 사람 확인 필요. missing 값은 schedule의 CHILD_DEVICE · PICKUP_SPOT, unmet 값은 GuardId 이름(G1~G5) 문자열
- [#38~#41 후속] (1) PickupSession.rejectBoarding은 boardingCancelableUntil이 null(탑승 인식 전)이면 NPE → SES-12를 인식 전에 부를 때 오류 코드를 서비스 항목에서 정할 것(예: REJECTION_WINDOW_CLOSED 또는 409) (2) IMMEDIATE에서 이미 탄 자녀(boardedAt 있음)를 notBoardedChildUserIds로 지정하면 phase만 NOT_BOARDED가 되고 boardedAt은 남음 → boardedAt도 비울지 설계 결정 필요 (3) 이미 COMPLETED · CANCELLED 세션에 complete를 부를 때 SESSION_ALREADY_FINISHED로 막을지 미정
- [#42~#44 후속] incidentCause를 언제 채우는지 설계서에 없음(SES-13 요청에 cause 없음). COMPLETED · CANCELLED 세션의 openIncident 허용 여부(확인 필요 「완료 뒤 · 주행 중 SES-13」)와 해소된 예외 뒤 다시 열기 여부 미정 — 해소는 SES-06 보류 해제 때 incidentResolvedAt을 채운다는 가정(서비스 항목에서 처리)
- [#50 시드] ScheduleSeed는 민준 일정 1개만 만들어 PickupSeed가 서윤 일정(민준과 같은 학원 · MON,WED · 18:00 · targetPickup 5 · READY)을 직접 만들고 clear에서 지움. ScheduleSeedTest(두 자녀 일정 정확히 1개) 단언을 지키려는 선택. PickupSeed.clear는 SessionEvent · Call · PickupOutcome을 지우지 않으니 이것들을 만드는 persistence · service 테스트는 직접 지우거나 clear를 늘릴 것
- [#214 후속] 이미 확인한 자녀에게 acknowledgeChild가 다시 오면 무시하는데 stateVersion을 올릴지 미정. markChildrenCalled가 MEETING · NOT_BOARDED 자녀도 CALLED로 바꾸는지 미정(테스트는 WAITING만). 철회 때 extraPrepMinutes · callNotBefore를 비울지 미정. markChildrenCalled는 at 인자 없이 만듦(자녀에 호출 시각 칸 없음)
- [문서] docs/followup/TODO.md에서 shared · user · auth · schedule · place가 「완료 · 4/4」에서 「대기 · 0/4 · 리뷰 기록 없음」으로 표시됨. 그 도메인들은 다른 경로(C:/pickup/docs)·다른 PC의 tdd-guard에서 리뷰 · 완료 기준을 기록해 이 PC 저장소에 기록이 없기 때문. 설계서 체크 표시와 docs/followup/<도메인>.md는 그대로. 필요하면 각 도메인 리뷰 기록을 이 PC tdd-guard에 다시 남기거나 TODO를 수동 정리
- [service lane #65~#68 후속] 픽업 시작에서 일정이 0건(scheduleIds를 못 찾음)이면 지금 SCHEDULES_NOT_GROUPABLE — 404 NOT_FOUND로 할지 결정 필요. 설계서 「같은 날」 묶기 조건(일정 weekdays에 오늘 요일 포함)은 테스트 · 구현 없음 — 항목 추가 검토
- [service lane #70 결정 확인] DRAFT 일정으로 픽업을 시작하면 422 SETUP_INCOMPLETE에 missing "SCHEDULE"(schedule SET-07 missing 값 재사용, test-writer 결정) — 사람 확인 필요. 같은 세션 반환(200)과 SETUP 검사의 순서는 미정(지금은 준비 완료 상태만 테스트)
- [service lane SessionViewAssembler] (1) ETA 유효 1분 · 호출 트리거 180초 · 700m 등 기준값을 클래스 상수로 둠 — 설계서는 application.yml 값. 설정 클래스(@ConfigurationProperties)를 service lane에서 만들어 주입할 것 (2) 주체별 필터(자녀 자기 항목만 · SES-04 전 spot null · location 숨김)는 #76~#78에서 principal 인자 추가 (3) incident.childUserId는 INCIDENT_OPENED payload에서 읽어야 하는데 SessionEventPort에 최근 건 조회가 없음(loadFirst만) — #131 때 정할 것 (4) children[].location에 위치 동의 확인 없이 lastLocation을 보임(SIG-04가 동의 없으면 저장 안 함에 기댐) (5) 조립이 미리 계산해 #79 · #80 등 뒤 테스트가 처음부터 GREEN일 수 있음
- [persistence lane PickupSessionPersistenceAdapter] (1) save에 @Transactional을 붙임 — 다른 어댑터엔 없고 트랜잭션 경계는 서비스. 세션 · 자녀 행 원자성 때문이라 유지할지 리뷰 때 정할 것 (2) loadActiveBy* 여러 건이면 sessionId 큰 것 1건 — 설계서엔 「가장 최근」 기준이 없음 (3) UPDATE가 setter 없는 엔티티를 새로 만들어 merge해 SELECT가 한 번 더 나감(SchedulePersistenceAdapter와 같은 방식)
- [설계 확인 필요 · 이벤트 받는 사람] 이벤트 표 「보호자 · 자녀」의 보호자에 CO_GUARDIAN · DELEGATE · TEACHER가 드는지, 자녀가 세션에 든 자녀인지 가족 전체 자녀인지 정해지지 않음. service lane은 GUARDIAN + 세션 자녀로 최소 구현. SESSION_STARTED payload 정의 없음, 새 세션 stateVersion 0인지 1인지 설계서에 없음(현재 PickupSession.start는 0)
- [persistence 설계 확인] (1) loadBoardingExpired에서 boardingCancelableUntil == now를 포함하는지 미정(지금은 < now) (2) loadLiveCall: 가장 최근 Call이 REVOKED이고 더 오래된 Call이 살아 있으면 그 오래된 Call을 돌려주는지 미정(정상 흐름에선 생기지 않음) (3) ProcessedVehicleEvent PK가 eventId뿐이라 다른 세션의 같은 eventId도 중복으로 봄 — 차량 eventId가 전역 고유인지 확인
- [설계 확인 · 자녀에게 장소 숨김] 「SES-04 전」을 routeVersion == 0(경로 확정을 한 번도 안 함)으로 정함. SES-05로 장소가 다시 SELECTED가 돼도 자녀에게 다시 숨기지 않음. TEACHER에게 location · spot을 주는지 미정(테스트는 GUARDIAN · CO_GUARDIAN · DELEGATE만)
- [persistence lane] (1) ProcessedVehicleEvent markProcessed가 네이티브 쿼리 INSERT ... ON CONFLICT DO NOTHING을 씀 — 02_공통-규칙-global.md L72 네이티브 쿼리 예외 목록에 한 줄 추가 필요 (2) loadForUpdate는 서비스 @Transactional 안에서만 불러야 함(밖이면 readOnly 트랜잭션에서 FOR UPDATE 거부) (3) loadBoardingExpired가 세션마다 자녀 쿼리를 따로 보냄(N+1) — 대상이 많으면 묶어 읽기 (4) CallPersistenceAdapter loadCall · loadLatestCall · saveCall은 아직 UOE — service가 쓰므로 persistence lane에서 채워야 함
- [설계 확인 · 출발 알림] 출발 알림의 「도착 예정 시각 포함」을 어디에 담을지 미정 — NotificationRequest에 eta 칸이 없음. DEPARTURE_NOTICE_SENT payload에 etaAt을 넣을지 정할 것. 경로 확정 거절 순서: SPOT_NOT_SAVED(422) 먼저, 세션 장소와 다른 경우 409는 그 뒤
- [service lane SessionAccess] 세션을 바꾸는 서비스(경로 확정 · 장소 변경 · 취소 · 신호 등)는 SessionAccess.requireVisible · requireStateVersion을 쓰도록 맞출 것 — ConfirmRouteService는 #82와 나란히 만들어져 아직 안 씀(서비스 lane 마지막 리뷰에서 정리). 세션에 없는 같은 가족 자녀는 조회 NOT_FOUND, 같은 가족 다른 차량도 NOT_FOUND로 정함. TEACHER는 가족 사용자로 통과(미정)
- [persistence 설계 확인 · SessionEventPort.loadFirst] 「처음」을 그 세션 · 그 type 중 seq가 가장 작은 이벤트로 정함(설계서에 기준 없음). append에 payload null이 오는 경우 미검증
- [service lane 순서] #94(호출 저장 중 예외면 Call · SessionEvent가 남지 않음)는 실제 트랜잭션이 필요한 통합 테스트라 persistence lane을 service lane에 합친 뒤 진행. #137(SSE 리스너 · 커밋 뒤 발행)도 같음
- [persistence lane SessionEventPersistenceAdapter] (1) loadLastHelpRequest가 jsonb 비교 대신 세션의 HELP_REQUESTED를 모두 읽어 Java로 거름 — 건수가 많아지면 네이티브 쿼리로 (2) append에 없는 sessionId가 오면 잠금 없이 저장됨 (3) 엔티티에 연관 매핑이 없어 session_event · session_child 등 sessionId FK가 DB에 없음 — 설계서 표는 FK. 기반 엔티티 확인 필요
- [설계 확인 · SES-05 · SES-06] (1) 장소 변경에서 다른 가족 장소면 SPOT_NOT_SAVED인지 (2) command spotVersion이 서버와 다르면 STATE_VERSION_CONFLICT인지 (3) CHILD_ALREADY_MOVING을 CALL_ALREADY_SENT보다 먼저 보는지 (4) 취소 때 자녀를 WAITING으로 되돌리는지 (5) 호출 철회가 stateVersion을 올리는지 — 모두 설계서에 없음. 취소 사유는 Call.revokeReason에 넣음
- [service lane ReceiveTelemetryService] (1) 기준값이 PickupProperties(record, 생성자 주입)와 SessionViewAssembler 상수 두 곳에 있음 — 하나로 합치고 application.yml에서 읽게 할 것 (2) CALL_TRIGGERED는 stateVersion을 올리지 않음(도메인에 호출 생성 때 올리는 메서드 없음) (3) Call.accept가 acceptedAt을 null로 둠 — 서비스 · 어댑터 중 누가 채울지 (4) ETA 저장(etaAt · remainingMeters · etaUpdatedAt) 보조 테스트를 항목 밖으로 더함 (5) ETA_UPDATED · CALL_TRIGGERED payload 키가 설계서에 없음(CALL_TRIGGERED는 callId · trigger로 정함)
- [service lane ChangeSpot · Cancel] (1) ChangeSpotService가 장소 없음 · 다른 가족 장소에 SPOT_NOT_SAVED를 던지는데 테스트가 없고 API 표 SES-05 오류 목록에도 없음 — 설계서에 넣고 테스트하거나 뺄 것 (2) command.spotVersion 불일치 409 미구현 (3) PickupSession.complete()는 자녀 위치를 비우는데 cancel()은 안 비움 — 서비스가 clearLocation을 직접 부름. 기반 도메인 cancel()에서 비우게 맞출 것 (4) 받는 사람 계산 private 도우미가 4개 서비스에 겹침 — 공용화 (5) CALL_REVOKED payload에 callId를 넣을지(CALL_SENT 예시는 callId 있음)
- [service lane ReceiveTelemetryService · 설계서와 다름] 설계서 L277 「observedAt이 마지막 값보다 오래되면 무시」인데 지금은 applyTelemetry가 값만 안 덮어쓰고 save · ETA_UPDATED 발행 · 호출 판정은 그대로 돌아, 늦게 온 ETA로 호출이 만들어질 수 있음. 체크 항목이 없어 테스트 없음 — #101 묶음에서 테스트를 더해 막을 것(applyTelemetry가 무시 여부를 알려주지 않으니 서비스에서 etaUpdatedAt 비교)
- [persistence lane 마지막 묶음] (1) PublishSessionEventPort 구현체 없음 — adapter/out/event(SSE · 커밋 뒤 발행) 몫으로 service 또는 web lane에서 만들어야 함, 없으면 병합 뒤 스프링 문맥이 깨짐 (2) Call · Outcome · DataSetting 엔티티에 값 바꾸는 메서드가 없어 UPDATE를 merge로 함 — createdAt 메모리 값이 null이 되는 문제를 어댑터에서 다시 읽어 막음 (3) loadHistory CANCELLED의 completedAt은 null(세션에 취소 시각 컬럼 없음)
- [service lane ReceiveVehicleEventService] (1) 정해지지 않은 차량 이벤트 type은 markProcessed만 남기고 끝남(테스트 없음) — STOP_FAILED 등 처리 규칙 확인 (2) door 값 검증은 web(컨트롤러)에서 — BoardingDoor.valueOf 실패 시 IllegalArgumentException (3) DEPARTED · BOARDING_DETECTED 이벤트 시각은 차량이 보낸 occurredAt 기준(설계서는 VEHICLE_STOPPED만 명시)
- [설계 확인 · CALL 결과] (1) 없는 callId · 이미 SENT인 Call에 다시 온 markCallSent · ACKNOWLEDGED Call에 온 markCallDelivered 동작이 설계서에 없음 (2) CALL_SENT · CALL_DELIVERED 이벤트 시각은 알림 모듈이 넘긴 sentAt · deliveredAt (3) [SSE 구조] web lane의 stream(#158~#162) 연결 보관소와 service #137 리스너 · PublishSessionEventPort 구현체가 서로 걸림 — 연결 보관소는 web lane, 리스너 · 포트 구현체는 병합 뒤 메인 폴더에서 #137과 함께 만들 것
- [설계 확인 · CALL-02] (1) 거절 순서: API 표 L183은 CALL_GUARD_UNMET 먼저, 핵심 로직 L305는 모드 먼저 — L305(모드 → 살아 있는 호출 → 가드)로 정함, API 표 순서를 맞출 것 (2) callHeld true일 때 수동 합류 안내를 받을지 미정 (3) 호출 생성 코드가 ReceiveTelemetryService · SendCallService에 겹침 — 리뷰 때 공용화
- [service lane 정리 거리] guardianIds · 받는 사람 계산 도우미가 ReceiveTelemetry · ConfirmRoute · CallProgress · ChangeSpot · CancelSession · StartPickup 등 6곳에 따로 있음. applyTelemetry 안 「더 오래된 값 무시」 검사가 서비스 검사와 겹침. service lane 마지막 리뷰에서 공용화
- [lane 병합 계획] service 서비스들은 @Service 없이 단위 테스트로만 GREEN, web 컨트롤러는 유스케이스를 생성자 주입해 web lane 단독 전체 테스트는 bean이 없어 못 뜸. 순서: (1) persistence를 feature/pickup에 병합 (2) service lane이 feature/pickup을 받아 #94 통합 테스트로 @Service · @Transactional · PickupProperties 설정 · PublishSessionEventPort 구현체 · Clock을 붙이고 전체 GREEN → service 병합 (3) web lane이 feature/pickup을 받아 전체 GREEN → web 병합. SES-03 응답 예시 children에 callNotBefore · location이 빠져 있음(필드 표에는 있음) — 예시를 필드 표에 맞출 것
- [service lane CallEngine] 호출 판정 · 생성 코드가 ReceiveTelemetryService(private) · SendCallService(private) · CallEngine 세 곳에 있음 — service lane 리뷰에서 ReceiveTelemetryService · SendCallService가 CallEngine을 쓰게 바꾸고 테스트 생성 코드도 맞출 것. 보류 해제의 「저장된 ETA」는 navEtaSeconds = etaAt − etaUpdatedAt로 계산. 보류 · 보류 해제는 이벤트를 저장하지 않음(19종에 없음). CALL_REVOKED · CALL_ACKNOWLEDGED payload 미정
- [service lane 준비 시간 · 도움 요청] (1) 준비 시간 15분 한도 · 도움 요청 60초 간격이 서비스 상수 — PickupProperties · application.yml로 합칠 것 (2) HELP_REQUEST_TOO_SOON 429에 retryAfterSeconds가 비어 있음(PickupErrors.helpRequestTooSoon이 nextAllowedAt만 넣음) — 공통 429 모양에 retryAfterSeconds가 필요하면 기반 수정 (3) 준비 시간 거절 기준을 살아 있는 호출(ACCEPTED 포함)로 정함 (4) PREP_TIME_EXTENDED payload 미정 (5) [설계 확인 · 보호자 범위] 모든 서비스가 받는 사람 「보호자」를 Role.GUARDIAN만으로 봄 — CO_GUARDIAN · DELEGATE 포함 여부를 사람에게 확인
- [설계 확인 · SES-07 탑승 완료] (1) 완료 때 살아 있는 호출을 철회하는지 설계서에 없음(취소만 자동 철회) — 지금은 그대로 둠 (2) VEHICLE_STOPPED가 없으면 actualStopAt · predictedStopAt 둘 다 null (3) SessionEvent payload의 시각 값은 DB JSON에서 문자열로 읽힘 — 서비스가 Instant.parse로 읽음. payload 시각 형식(UTC Z 문자열)을 설계서에 적을 것 (4) CompletePickupCommand.mode가 String — web에서 IMMEDIATE · MANUAL만 받게 검증
- [설계 확인 · CALL-04 응답] 응답 status가 호출 상태인지(형제 일부만 확인하면 SENT · DELIVERED가 나감) 자녀 기준인지, acknowledgedAt이 그 자녀 확인 시각인지 호출 전체 시각인지 미정. CALL-03 요청 stateVersion 필수 · reason 0~100자 검증은 체크 항목 없음 — web 검증 항목 추가 여부 확인
- [web 검증 거리 · SES-11 완료] CompletePickupCommand.mode는 IMMEDIATE · MANUAL만 받게 web에서 VALIDATION_FAILED — 지금 서비스는 잘못된 값이면 IllegalArgumentException, DOOR도 통과함(DOOR는 자동 완료 전용). notBoardedChildUserIds가 null이면 빈 목록으로 처리함
- [service lane 빈 곳 · 세션 뷰 incident] SessionViewAssembler에 incident 조립 코드가 없음(늘 null). 설계서 L673은 incident.childUserId를 INCIDENT_OPENED payload에서 읽는다고 가정하는데 assemble은 이벤트를 받지 않음 — web #171(incident가 채워진 SessionView)과 맞물리므로 service lane 리뷰에서 assemble에 incident 재료(loadFirst/최근 INCIDENT_OPENED)를 넣을 것. OUT-02 correction이 없을 때 null인지 CorrectionView(null, null)인지 미정
- [web 검증 거리] SES-04 요청 필드(spotId · navRouteId · stateVersion) 필수 검증, CALL-03 stateVersion · reason 검증 등 요청 검증 항목이 체크리스트에 일부만 있음 — 도메인 리뷰 때 web 검증 항목을 add로 보탤지 정할 것. #203 Idempotency-Key 누락 테스트는 PickupIdempotencyKeyTest로 따로
- [web 검증 거리 · service 확인] (1) incident type은 IncidentType.valueOf라 정의 안 된 값이면 500 — web에서 검증 (2) SES-13 탑승 예외 childUserId가 세션 자녀인지 확인 안 함 (3) OUT-01 childId가 CHILD 역할인지 확인 안 함(같은 가족만 봄) (4) OUT-02 정정이 없으면 correction은 CorrectionView(null, null) — web 응답이 correction.status null 모양인지 맞출 것
- [설계 확인 · 자동 완료 · 연락처 · 데이터 설정] (1) 자동 완료 작업의 트랜잭션 경계(세션마다 따로인지)와 한 세션 실패가 다른 세션 완료를 막지 않는지 설계서에 없음 — 스케줄러 어댑터를 만들 때 정할 것(스케줄러 어댑터 adapter/in/scheduler 항목이 체크리스트에 있는지 확인) (2) 연락처 정렬 순서 · 학원 phone null 처리 미정 (3) retentionDays 90이 서비스 상수 — PickupProperties로 합칠지 (4) learningConsent false 때 파기 대기는 후속 과제
- [web 테스트 손볼 곳] PickupCallControllerTest #188이 유스케이스 mock 응답 status를 SENT로 둠 — 설계서 결정은 CALL-02 응답 status ACCEPTED. 다음 PickupCallControllerTest 묶음(call-hold #185~#187)에서 mock 값을 ACCEPTED로 맞출 것. CALL-02 Idempotency-Key 누락 · 차량 외 403 테스트는 없음(#203 묶음에서)
- [설계서와 다름 · OUT-04] learningConsent를 false로 바꾸면 분석용 기록을 파기 대기로 돌린다는 설계서 문장이 있으나 체크 항목 · out 포트가 없어 구현 안 함. 자동 완료 작업이 한 실행에서 여러 세션을 같은 트랜잭션으로 처리하면 한 세션 실패가 전체를 롤백할 수 있음 — 스케줄러 어댑터에서 세션마다 트랜잭션을 나눌 것(스케줄러 어댑터 체크 항목 없음 — 도메인 리뷰 때 add 여부)
- [기반 확인 · common] TimeUtils의 SEOUL이 private이고 KST 날짜 변환 도우미가 todayKst(Clock)뿐이라 CorrectOutcomeService가 Asia/Seoul 상수를 따로 둠 — common에 TimeUtils.dateKst(Instant) 같은 공용 도우미를 둘지. reasonCode가 enum 밖이면 IllegalArgumentException — web에서 VALIDATION_FAILED로 막을 것
- [web lane 남은 일] (1) #203 Idempotency-Key는 PickupIdempotencyKeyTest(PlaceIdempotencyKeyTest 본보기)로 모든 픽업 API를 한 번에 — web lane 마지막 묶음 (2) #219 stateVersion 필수 검증을 PickupSessionController 요청(ConfirmRoute · ChangeSpot · HoldSession · CancelSession, int → Integer + @NotNull)에도 맞출 것 (3) reason 0~100자 검증(CALL-03 · SES-06 · SES-07) 체크 항목 없음
- [service lane 리뷰 남은 것] (1) SessionViewAssembler가 static이라 PickupProperties.defaults()를 씀 — application.yml로 값을 바꾸면 CallEngine G4 판정과 세션 뷰 guard · scheduledAt이 어긋남. #94 빈 등록 때 assemble에 값을 넘기거나 빈으로 바꿀 것 (2) 자동 완료는 세션마다 트랜잭션을 나눌 것(#94 단계) (3) [설계 확인 · 사람] 세션 뷰 incident.childUserId — PickupSession · 테이블에 예외 자녀 칸이 없어 채울 수 없음. 테이블에 칸을 둘지 뷰에서 뺄지 (4) HoldCallService · StartPickupService에 SessionAccess.latestView와 같은 뷰 조립 코드가 남음 (5) SessionEventMessage 생성 함수를 port record(기반)로 옮길지
- [#94 단계에서 확인 · #220 실제 효과] CallPersistenceAdapter.loadCall이 같은 트랜잭션에서 findById로 다시 읽으면 1차 캐시의 예전 엔티티가 나와 잠금 뒤 다시 읽기가 철회를 못 볼 수 있음 — 어댑터에서 refresh 또는 캐시를 피하는 조회로 바꾸고 통합 테스트로 확인할 것
- [설계 확인 · #222~#224] (1) 세션 보류 · 해제는 이벤트를 저장 · 발행하지 않음(19종에 없음) — 다른 앱이 보류를 실시간으로 알아야 하면 이벤트 종류 추가 필요 (2) 보류 reason을 저장할 컬럼 없음 (3) 보류 해제 뒤 호출 판정을 다시 돌릴지 미정 (4) isInActivePickup은 자녀로 든 세션만 봄 — 보호자 · 대리인 탈퇴 등을 막을지 (5) activeChildLocation 여러 자녀면 lastLocatedAt 가장 늦은 자녀
