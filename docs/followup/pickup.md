# FOLLOWUP · 픽업 (pickup)

- 생성: 2026-11-09 15:22 KST · tdd-guard
- 상태: 진행 중 (체크 63/218 · 완료 기준 0/4)
- 테스트: ● GREEN · 1421 passed · 전체 · 112s · .\gradlew.bat test --console=plain
- 설계서: C:/ilink/docs/08_픽업-pickup.md

## 완료 항목
- [x] #1 [기반] PickupSession.start는 ACTIVE · ROUTING · SELECTED · stateVersion 0인 세션과 자녀마다 WAITING인 SessionChild를 만든다 ← PickupSessionTest.PickupSession.start는 ACTIVE · ROUTING · SELECTED · stateVersion 0인 세션을 만든다
- [x] #2 [기반] PickupSession.start는 장소의 spotId · name · stopPoint · meetingPoint를 세션 스냅샷으로 복사한다 ← PickupSessionTest.PickupSession.start는 장소의 spotId · name · stopPoint · meetingPoint를 세션 스냅샷으로 복사한다
- [x] #3 [기반] 세션을 바꾸는 도메인 동작을 하면 stateVersion이 1 오른다 ← PickupSessionTest.세션을 바꾸는 도메인 동작 releaseHold를 하면 stateVersion이 1 오른다
- [x] #4 [기반] hold는 ACTIVE를 ON_HOLD로 바꾸고 releaseHold는 ON_HOLD를 ACTIVE로 되돌린다 ← PickupSessionTest.releaseHold는 ON_HOLD 세션을 ACTIVE로 되돌린다
- [x] #5 [기반] cancel은 ACTIVE와 ON_HOLD 세션을 CANCELLED로 바꾼다 ← PickupSessionTest.cancel은 ON_HOLD 세션을 CANCELLED로 바꾸고 stateVersion을 1 올린다
- [x] #6 [기반] COMPLETED나 CANCELLED 세션에 cancel을 부르면 SESSION_ALREADY_FINISHED로 거절된다 ← PickupSessionTest.COMPLETED 세션에 cancel을 부르면 SESSION_ALREADY_FINISHED로 거절되고 세션은 그대로다
- [x] #7 [기반] confirmRoute는 spotStatus를 CONFIRMED로 바꾸고 routeVersion을 1 올린다 ← PickupSessionTest.confirmRoute는 spotStatus를 CONFIRMED로 바꾸고 routeVersion과 stateVersion을 1씩 올린다
- [x] #8 [기반] changeSpot은 spotStatus를 SELECTED로 되돌리고 spotVersion을 1 올리고 스냅샷을 새 장소 값으로 바꾼다 ← PickupSessionTest.changeSpot은 spotStatus를 SELECTED로 되돌리고 spotVersion과 stateVersion을 1씩 올린다
- [x] #9 [기반] changeSpot은 MEETING 자녀가 있으면 CHILD_ALREADY_MOVING으로 거절된다 ← PickupSessionTest.changeSpot은 MEETING 자녀가 있으면 CHILD_ALREADY_MOVING으로 거절되고 spotVersion · stateVersion · 스냅샷은 그대로다
- [x] #10 [기반] applyTelemetry는 etaAt을 observedAt + navEtaSeconds로, remainingMeters와 etaUpdatedAt을 새 값으로 덮어쓴다 ← PickupSessionTest.applyTelemetry는 etaAt을 observedAt + navEtaSeconds로, remainingMeters를 새 값으로, etaUpdatedAt을 observedAt으로 덮어쓴다
- [x] #11 [기반] applyTelemetry는 observedAt이 마지막 값보다 오래되면 아무것도 바꾸지 않는다 ← PickupSessionTest.applyTelemetry는 observedAt이 마지막 값보다 오래되면 etaAt · remainingMeters · etaUpdatedAt · stateVersion을 바꾸지 않는다
- [x] #12 [기반] ETA는 etaUpdatedAt이 60초를 넘게 지나면 stale로 판정된다 ← PickupSessionTest.ETA는 etaUpdatedAt이 60초를 넘게 지나면(61초) stale로 판정된다
- [x] #13 [기반] CallTrigger는 navEtaSeconds 180 · remainingMeters 2000이면 ETA_3MIN을 돌려준다 ← CallTriggerTest.CallTrigger는 navEtaSeconds 180 · remainingMeters 2000이면 ETA_3MIN을 돌려준다
- [x] #14 [기반] CallTrigger는 navEtaSeconds 600 · remainingMeters 700이면 DIST_700M을 돌려준다 ← CallTriggerTest.CallTrigger는 navEtaSeconds 600 · remainingMeters 700이면 DIST_700M을 돌려준다
- [x] #15 [기반] CallTrigger는 navEtaSeconds 181 · remainingMeters 701이면 트리거 없음을 돌려준다 ← CallTriggerTest.CallTrigger는 navEtaSeconds 181 · remainingMeters 701이면 트리거 없음을 돌려준다
- [x] #16 [기반] Guard는 ON_HOLD 세션에서 G1을 unmet에 넣는다 ← GuardTest.Guard는 ON_HOLD 세션에서 G1을 unmet에 넣는다
- [x] #17 [기반] Guard는 spotStatus가 CONFIRMED가 아니면 G2를 unmet에 넣는다 ← GuardTest.spotStatus SELECTED
- [x] #18 [기반] Guard는 확정 경로가 지금 장소 버전과 다르면 G3을 unmet에 넣는다 ← GuardTest.Guard는 확정 경로의 장소 버전(routeSpotVersion)이 지금 spotVersion과 다르면 G3을 unmet에 넣는다
- [x] #19 [기반] Guard는 ETA가 stale이면 G4를 unmet에 넣는다 ← GuardTest.Guard는 ETA가 stale이면(etaUpdatedAt이 61초 전) G4를 unmet에 넣는다
- [x] #20 [기반] Guard는 세션 자녀 중 한 명이라도 callNotBefore가 지나지 않았으면 G5를 unmet에 넣는다 ← GuardTest.Guard는 세션 자녀 둘 중 한 명이라도 callNotBefore가 지나지 않았으면(3분 뒤) G5를 unmet에 넣는다
- [x] #21 [기반] Guard는 다섯 조건을 모두 만족하면 met true와 빈 unmet을 돌려준다 ← GuardTest.Guard는 G1~G5 다섯 조건을 모두 만족하면 met true와 빈 unmet을 돌려준다
- [x] #22 [기반] 새 Call은 status ACCEPTED와 triggerType · 그때의 spotVersion을 갖는다 ← CallTest.새 Call은 status ACCEPTED와 triggerType · 그때의 spotVersion을 갖는다
- [x] #23 [기반] 철회된 호출 뒤에 만든 Call은 prevCallId로 이전 호출을 가리킨다 ← CallTest.철회된 호출 뒤에 만든 Call은 prevCallId로 이전 호출을 가리킨다
- [x] #24 [기반] Call.markSent는 status를 SENT로 바꾸고 sentAt을 채운다 ← CallTest.Call.markSent는 status를 SENT로 바꾸고 sentAt을 채운다
- [x] #25 [기반] Call.revoke는 status REVOKED · revokedAt · revokeReason을 채운다 ← CallTest.Call.revoke는 status REVOKED · revokedAt · revokeReason을 채운다
- [x] #26 [기반] 이미 REVOKED인 Call을 revoke하면 CALL_ALREADY_REVOKED로 거절된다 ← CallTest.이미 REVOKED인 Call을 revoke하면 CALL_ALREADY_REVOKED로 거절되고 첫 철회 값은 그대로다
- [x] #27 [기반] 자녀 둘인 세션에서 한 명만 확인하면 Call은 ACKNOWLEDGED가 아니고 둘 다 확인하면 ACKNOWLEDGED다 ← CallTest.자녀 둘인 세션에서 둘 다 확인하면 Call은 ACKNOWLEDGED이고 acknowledgedAt은 두 번째 확인 시각이다
- [x] #28 [기반] SessionChild.acknowledge를 두 번 부르면 첫 acknowledgedAt이 유지된다 ← SessionChildTest.SessionChild.acknowledge를 두 번 부르면 첫 acknowledgedAt이 유지된다
- [x] #29 [기반] SessionChild.acknowledge는 CALLED 자녀를 MEETING으로 바꾼다 ← SessionChildTest.SessionChild.acknowledge는 CALLED 자녀를 MEETING으로 바꾼다
- [x] #30 [기반] extendPrep은 18:09에 5분을 더하면 extraPrepMinutes 5 · callNotBefore 18:14로 만든다 ← SessionChildTest.extendPrep은 18:09에 5분을 더하면 extraPrepMinutes 5 · callNotBefore 18:14로 만든다
- [x] #31 [기반] extendPrep은 18:05에 5분, 18:09에 5분을 더하면 extraPrepMinutes 10 · callNotBefore 18:19로 만든다 ← SessionChildTest.extendPrep은 18:05에 5분, 18:09에 5분을 더하면 extraPrepMinutes 10 · callNotBefore 18:19로 만든다
- [x] #32 [기반] extendPrep은 누적이 15분을 넘으면 PREP_EXTENSION_LIMIT으로 거절된다 ← SessionChildTest.extendPrep은 누적이 15분을 넘으면(10분 + 10분) PREP_EXTENSION_LIMIT으로 거절되고 extraPrepMinutes · callNotBefore는 그대로다
- [x] #33 [기반] extendPrep은 childPhase를 바꾸지 않는다 ← SessionChildTest.extendPrep은 childPhase를 바꾸지 않는다(WAITING 그대로)
- [x] #34 [기반] detectBoarding은 STOPPED · 운전석 아닌 문 · 호출 발송 뒤면 boardingDetectedAt · boardingCancelableUntil(+10초) · boardingDoor와 모든 자녀의 boardedAt을 채운다 ← PickupSessionTest.door FRONT_RIGHT
- [x] #35 [기반] detectBoarding은 STOPPED가 아니면 아무것도 바꾸지 않는다 ← PickupSessionTest.vehiclePhase ROUTING
- [x] #36 [기반] detectBoarding은 합류 안내 발송 전이면 아무것도 바꾸지 않는다 ← PickupSessionTest.detectBoarding은 합류 안내 발송 전이면 탑승 칸 · 자녀 boardedAt · stateVersion을 바꾸지 않고 false를 돌려준다
- [x] #37 [기반] rejectBoarding은 취소 창 안이면 그 자녀의 boardedAt만 비운다 ← PickupSessionTest.rejectBoarding은 취소 창 안(인식 5초 뒤)이면 그 자녀의 boardedAt만 비우고 다른 자녀 · 탑승 칸 · 자녀 phase는 그대로 둔다
- [x] #38 [기반] rejectBoarding은 boardingCancelableUntil이 지났으면 REJECTION_WINDOW_CLOSED로 거절된다 ← PickupSessionTest.rejectBoarding은 boardingCancelableUntil이 지났으면(1초 뒤) REJECTION_WINDOW_CLOSED로 거절되고 boardedAt · stateVersion은 그대로다
- [x] #39 [기반] complete는 MANUAL인데 vehiclePhase가 STOPPED가 아니면 VEHICLE_NOT_STOPPED로 거절된다 ← PickupSessionTest.vehiclePhase APPROACHING
- [x] #40 [기반] complete는 notBoardedChildUserIds의 자녀를 NOT_BOARDED로 바꾸고 세션을 COMPLETED로 바꾼다 ← PickupSessionTest.complete는 MANUAL · 정차 중이면 notBoardedChildUserIds의 자녀를 NOT_BOARDED로 바꾸고 세션을 COMPLETED로 바꾼다
- [x] #41 [기반] complete는 탑승하지도 미탑승 지정되지도 않은 자녀가 있으면 CHILDREN_NOT_ALL_BOARDED로 거절된다 ← PickupSessionTest.complete는 IMMEDIATE를 탑승 인식 전에 부르면 탄 자녀가 없어 CHILDREN_NOT_ALL_BOARDED로 거절되고 세션 상태 · stateVersion은 그대로다
- [x] #42 [기반] complete는 모든 자녀의 lastLat · lastLng · lastLocatedAt을 비운다 ← PickupSessionTest.boardingMethod MANUAL
- [x] #43 [기반] openIncident는 세션을 ON_HOLD로 바꾸고 incidentType · incidentOpenedAt을 채운다 ← PickupSessionTest.openIncident는 ACTIVE 세션을 ON_HOLD로 바꾸고 incidentType · incidentOpenedAt을 채우며 incidentResolvedAt은 비워 둔다
- [x] #44 [기반] 열린 예외가 있는 세션에 openIncident를 부르면 INCIDENT_ALREADY_OPEN으로 거절된다 ← PickupSessionTest.열린 예외가 있는 세션에 openIncident를 부르면 INCIDENT_ALREADY_OPEN으로 거절되고 sessionStatus · incidentType · incidentOpenedAt · stateVersion은 그대로다
- [x] #45 [기반] updateChildLocation은 at이 저장된 값보다 오래되면 위치를 바꾸지 않는다 ← SessionChildTest.updateLocation은 at이 저장된 값보다 오래되면 위치와 시각을 바꾸지 않는다
- [x] #46 [기반] 도움 요청 규칙은 같은 아이의 마지막 요청 뒤 60초 안이면 HELP_REQUEST_TOO_SOON과 마지막 시각 + 60초를 돌려준다 ← HelpRequestRuleTest.도움 요청 규칙은 같은 아이의 마지막 요청 뒤 60초 안이면 HELP_REQUEST_TOO_SOON과 마지막 시각 + 60초를 돌려준다
- [x] #47 [기반] 핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다 ← PickupInPortsTest.핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다
- [x] #48 [기반] 핵심 로직 「시그니처」의 out 포트 인터페이스가 있다 ← PickupOutPortsTest.핵심 로직 「시그니처」의 out 포트 인터페이스가 있다
- [x] #49 [기반] JPA 엔티티 5개가 BaseTimeEntity를 상속하고 테이블 정의대로 매핑된다(PickupOutcome PK는 sessionId) ← PickupPersistenceEntitiesTest.픽업 엔티티 7개(PickupSession · SessionChild · SessionEvent · Call · PickupOutcome · GuardianDataSetting · ProcessedVehicleEvent)는 @Entity이고 BaseTimeEntity를 바로 상속한다
- [x] #50 [기반] 테스트 시드로 차량 1대 · 자녀 2명 · 기본 장소가 있는 ACTIVE 세션 1개를 만든다 ← PickupSeedTest.테스트 시드로 차량 1대 · 자녀 2명(민준 · 서윤) · 기본 장소 정문 앞이 있는 오늘(한국) ACTIVE 세션 1개를 만든다
- [x] #204 [기반] 모듈 연결 out 포트의 어댑터가 있다: LoadSchedulePort는 schedule, LoadSpotPort는 place, LoadFamilyPort는 user, LoadChildReadinessPort는 auth의 in 포트로 읽고, RecordNotificationPort는 notification 구현 전 임시로 빈 목록을 돌려준다 ← PickupModuleLinkAdaptersTest.pickup의 연결 out 포트 5개는 각각 빈이 정확히 1개이고 pickup.adapter.out.<모듈> 아래 클래스가 구현한다: schedule · place · user · auth · notification
- [x] #205 [기반] 공통 오류 본문은 도메인 오류가 error 안에 추가 키(unmet · missing · nextAllowedAt)를 더할 수 있고, STATE_VERSION_CONFLICT는 error와 나란히 session에 최신 세션 뷰를 싣는다 ← GlobalExceptionHandlerTest.컨트롤러가 BusinessException.stateVersionConflict(최신 세션 뷰)를 던지면 409와 본문 최상위에 error와 session 두 키를 돌려준다
- [x] #206 [기반] PickupErrors는 API 명세의 픽업 오류 코드마다 상태 · code · 한국어 메시지를 담은 예외를 만든다 ← PickupErrorsTest.callGuardUnmet([G2, G4])는 422 CALL_GUARD_UNMET · 한국어 message이고 error 추가 키 unmet 하나에 받은 가드 목록을 담는다
- [x] #208 [기반] GuardianDataSetting 엔티티는 userId PK · learningConsent로 매핑되고 PickupOutcome 엔티티는 correctionNote VARCHAR(200)을 갖고, out 포트에 LoadDataSettingPort · SaveDataSettingPort가 있다 ← PickupOutPortsTest.GuardianDataSetting용 out 포트 LoadDataSettingPort · SaveDataSettingPort가 있다
- [x] #210 [기반] SessionEvent 도메인은 eventId · sessionId · seq · type(SessionEventType 19종) · stateVersion · occurredAt · payload를 담고 restore로 다시 만들어진다 ← SessionEventTest.SessionEvent.restore는
- [x] #211 [기반] PickupOutcome 도메인은 완료 때 completedAt · boardingMethod · predictedStopAt · actualStopAt으로 만들어지고, 정정은 actualStopAt을 두고 correctedStopAt · correctionReason · correctionStatus REQUESTED · correctedBy · correctionNote를 채우며, diffSeconds는 둘 중 하나가 없으면 비어 있다 ← PickupOutcomeTest.PickupOutcome.correct는
- [x] #212 [기반] GuardianDataSetting 도메인은 행이 없으면 learningConsent false로 만들어지고 동의 값을 바꿀 수 있다 ← GuardianDataSettingTest.GuardianDataSetting.changeLearningConsent는
- [x] #213 [기반] Call은 markDelivered로 DELIVERED · deliveredAt을 채우고, 모든 필드를 restore로 되살리며, REVOKED인 Call에 늦게 온 markSent · markDelivered는 무시한다 ← CallTest.Call.markDelivered는
- [x] #214 [기반] PickupSession은 childUserId로 자녀를 바꾸는 진입점(호출됨 CALLED · 철회로 WAITING과 위치 · acknowledgedAt 비움 · 확인 MEETING · 준비 시간 추가 · 위치 갱신)을 갖고, 위치 갱신 밖의 변경은 stateVersion을 1 올리며, 세션에 없는 자녀면 CHILD_NOT_IN_SESSION으로 거절된다 ← PickupSessionTest.markChildrenCalled는
- [x] #215 [기반] PickupSession은 markStopped · markDeparted로 vehiclePhase를 STOPPED · DEPARTED로, holdCall · releaseCallHold로 callHeld를 바꾸고 stateVersion을 1 올린다 ← PickupSessionTest.markStopped는
- [x] #216 [기반] 끝난 세션의 hold · releaseHold · complete · detectBoarding · openIncident는 SESSION_ALREADY_FINISHED로 거절되고, releaseHold는 열린 예외의 incidentResolvedAt을 채우며, 탑승 인식 전 rejectBoarding은 REJECTION_WINDOW_CLOSED로 거절된다 ← PickupSessionTest.releaseHold는 열린 예외가
- [x] #217 [기반] PickupSession은 familyId를 start · restore로 담고 pickup_session은 family_id 컬럼을 가지며, LoadSessionPort에 loadActiveBySpot · LoadCallPort에 loadLatestCall(REVOKED 포함) · LoadFamilyPort에 loadMember(userId)가 있다 ← PickupSessionTest.PickupSession.start는 받은
- [x] #218 [기반] PickupErrors는 CHILD_NOT_CALLED 409를 만들고 PickupViolation CHILD_NOT_IN_SESSION을 NOT_FOUND로 바꾼다 ← PickupErrorsTest.CHILD_NOT_CALLED

## 미완료 항목
- [ ] #51 [persistence] PickupSession을 SessionChild와 함께 저장하고 읽으면 스냅샷 좌표와 자녀 상태가 같다
- [ ] #52 [persistence] 같은 세션에 같은 childUserId의 SessionChild를 두 번 저장하면 유니크 위반이 난다
- [ ] #53 [persistence] loadActiveByVehicle은 그 차량의 ACTIVE · ON_HOLD 세션만 찾는다
- [ ] #54 [persistence] loadActiveByChild는 그 자녀가 포함된 진행 중 세션을 찾는다
- [ ] #55 [persistence] loadForUpdate로 잠근 세션을 다른 트랜잭션이 loadForUpdate하면 앞 트랜잭션이 끝날 때까지 기다린다
- [ ] #56 [persistence] append는 세션 안 seq를 1부터 1씩 늘려 저장한다
- [ ] #57 [persistence] 같은 세션에 같은 seq의 SessionEvent를 저장하면 유니크 위반이 난다
- [ ] #58 [persistence] loadAfter는 afterSeq보다 큰 이벤트를 seq 오름차순으로 limit개 돌려주고 남으면 hasMore true다
- [ ] #59 [persistence] loadLastHelpRequest는 payload의 childUserId로 그 아이의 마지막 HELP_REQUESTED를 찾는다
- [ ] #60 [persistence] loadBoardingExpired는 boardingCancelableUntil이 지난 ACTIVE 세션만 돌려준다
- [ ] #61 [persistence] loadLiveCall은 그 세션의 REVOKED가 아닌 가장 최근 Call을 돌려준다
- [ ] #62 [persistence] PickupOutcome은 sessionId를 PK로 저장되고 같은 세션에 두 번째 행은 저장되지 않는다
- [ ] #63 [persistence] loadHistory는 그 자녀가 포함된 세션 중 기간 안의 COMPLETED · CANCELLED 세션만 돌려준다
- [ ] #64 [persistence] markProcessed는 처음 eventId에 true, 같은 eventId에 false를 돌려준다
- [ ] #65 [서비스] 픽업 시작은 date가 한국 날짜로 오늘이 아니면 NOT_TODAY로 거절한다
- [ ] #66 [서비스] 픽업 시작은 학원이 다른 일정이 섞이면 SCHEDULES_NOT_GROUPABLE로 거절한다
- [ ] #67 [서비스] 픽업 시작은 시간대가 다른 일정이 섞이면 SCHEDULES_NOT_GROUPABLE로 거절한다
- [ ] #68 [서비스] 픽업 시작은 기본 장소가 없으면 SETUP_INCOMPLETE와 missing PICKUP_SPOT으로 거절한다
- [ ] #69 [서비스] 픽업 시작은 자녀 기기가 없으면 SETUP_INCOMPLETE와 missing CHILD_DEVICE로 거절한다
- [ ] #70 [서비스] 픽업 시작은 DRAFT 일정이 있으면 SETUP_INCOMPLETE로 거절한다
- [ ] #71 [서비스] 픽업 시작은 그 차량에 다른 진행 중 세션이 있으면 ANOTHER_SESSION_ACTIVE로 거절한다
- [ ] #72 [서비스] 픽업 시작은 같은 일정의 진행 중 세션이 있으면 새로 만들지 않고 그 세션을 created false로 돌려준다
- [ ] #73 [서비스] 픽업 시작은 SESSION_STARTED를 seq 1로 저장하고 SESSION_STARTED 알림 기록을 요청한다
- [ ] #74 [서비스] 진행 중 세션 찾기는 차량 주체에 그 차량의 세션, 자녀 주체에 자기가 포함된 세션을 돌려준다
- [ ] #75 [서비스] 진행 중 세션 찾기는 세션이 없으면 빈 값을 돌려준다
- [ ] #76 [서비스] 세션 조회는 자녀 주체에게 자기 children 항목만 돌려준다
- [ ] #77 [서비스] 세션 조회는 SES-04 전이면 자녀 주체에게 spot을 null로 주고 eta.stopEtaAt은 준다
- [ ] #78 [서비스] 세션 조회는 자녀 주체에게 children[].location을 주지 않고 차량 · 보호자 주체에게는 준다
- [ ] #79 [서비스] 세션 조회는 호출이 없으면 status.call NONE을 돌려준다
- [ ] #80 [서비스] 세션 조회는 MEETING 자녀가 있으면 spot.locked true를 돌려준다
- [ ] #81 [서비스] 다른 가족의 세션을 조회하면 NOT_FOUND로 거절한다
- [ ] #82 [서비스] 세션을 바꾸는 요청의 stateVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절한다
- [ ] #83 [서비스] 경로 확정은 가족이 저장하지 않은 spotId면 SPOT_NOT_SAVED로 거절한다
- [ ] #84 [서비스] 첫 경로 확정은 자녀에게 DEPARTURE 알림 기록을 요청하고 DEPARTURE_NOTICE_SENT를 저장한다
- [ ] #85 [서비스] 두 번째 경로 확정은 출발 알림을 다시 요청하지 않는다
- [ ] #86 [서비스] 경로 확정은 SPOT_CONFIRMED를 저장한다
- [ ] #87 [서비스] 장소 변경은 철회되지 않은 발송 호출이 있으면 CALL_ALREADY_SENT로 거절한다
- [ ] #88 [서비스] 장소 변경이 성공하면 SPOT_CHANGED를 저장한다
- [ ] #89 [서비스] 픽업 취소는 철회되지 않은 호출을 REVOKED로 바꾸고 CALL_REVOKED와 SESSION_CANCELLED를 저장한다
- [ ] #90 [서비스] 픽업 취소는 자녀 위치 세 컬럼을 비운다
- [ ] #91 [서비스] 연락처는 전화번호가 있는 가족 멤버(CHILD 제외)와 role ACADEMY인 학원 대표번호를 돌려준다
- [ ] #92 [서비스] 차량 ETA 수신은 ETA_UPDATED를 저장하지 않고 발행만 한다
- [ ] #93 [서비스] AUTO 가족에서 트리거와 가드를 통과하면 Call ACCEPTED · CALL 알림 기록 요청 · CALL_TRIGGERED를 한 트랜잭션으로 저장한다
- [ ] #94 [서비스] 호출 저장 중 예외가 나면 Call · SessionEvent가 남지 않고 이벤트도 발행되지 않는다
- [ ] #95 [서비스] ON_HOLD 세션에서는 트리거를 만족해도 호출을 만들지 않는다
- [ ] #96 [서비스] callHeld가 true면 트리거를 만족해도 호출을 만들지 않는다
- [ ] #97 [서비스] 철회되지 않은 호출이 있으면 새 호출을 만들지 않는다
- [ ] #98 [서비스] 가드를 통과하지 못하면 호출을 만들지 않고 세션 뷰 call.guard.unmet에 그 가드를 보인다
- [ ] #99 [서비스] GUARDIAN_CONFIRM 가족에서는 트리거와 가드를 통과해도 호출을 자동으로 만들지 않는다
- [ ] #100 [서비스] callNotBefore 전에는 트리거를 만족해도 호출을 만들지 않고 callNotBefore 뒤 첫 SIG-01에서 만든다
- [ ] #101 [서비스] 철회된 호출 뒤 조건이 다시 맞으면 prevCallId를 단 새 호출을 만든다
- [ ] #102 [서비스] markCallSent는 Call을 SENT로, 세션 자녀를 CALLED로 바꾸고 CALL_SENT를 저장한다
- [ ] #103 [서비스] markCallDelivered는 Call을 DELIVERED로 바꾸고 CALL_DELIVERED를 저장한다
- [ ] #104 [서비스] 차량 이벤트 수신은 같은 eventId를 두 번째로 받으면 무시한다
- [ ] #105 [서비스] 차량 이벤트 STOPPED는 vehiclePhase를 STOPPED로 바꾸고 VEHICLE_STOPPED를 저장한다
- [ ] #106 [서비스] 차량 이벤트 DEPARTED는 vehiclePhase를 DEPARTED로 바꾸고 VEHICLE_DEPARTED를 저장한다
- [ ] #107 [서비스] 탑승 후보가 된 DOOR_CYCLE은 BOARDING_DETECTED를 저장한다
- [ ] #108 [서비스] 호출 보류는 이미 보낸 호출이 있으면 CALL_ALREADY_SENT로 거절한다
- [ ] #109 [서비스] 호출 보류 해제는 callHeld를 false로 바꾸고 저장된 ETA로 판정을 다시 돌려 조건이 맞으면 호출을 만든다
- [ ] #110 [서비스] 합류 안내 보내기는 AUTO 가족이면 NOT_GUARDIAN_CONFIRM_MODE로 거절한다
- [ ] #111 [서비스] 합류 안내 보내기는 가드를 통과하지 못하면 CALL_GUARD_UNMET과 unmet 목록으로 거절한다
- [ ] #112 [서비스] 합류 안내 보내기는 이미 보낸 호출이 있으면 CALL_ALREADY_SENT로 거절한다
- [ ] #113 [서비스] 합류 안내 보내기가 성공하면 triggerType MANUAL인 Call을 만든다
- [ ] #114 [서비스] 호출 철회는 세션 자녀를 WAITING으로 되돌리고 위치를 비우고 CALL_REVOKED를 저장한다
- [ ] #115 [서비스] 호출 확인은 형제가 모두 확인했을 때만 CALL_ACKNOWLEDGED를 저장한다
- [ ] #116 [서비스] 철회된 호출을 확인하면 CALL_ALREADY_REVOKED로 거절한다
- [ ] #117 [서비스] 준비 시간 추가는 합류 안내를 이미 받은 자녀면 CALL_ALREADY_SENT로 거절한다
- [ ] #118 [서비스] 준비 시간 추가는 PREP_TIME_EXTENDED를 저장하고 보호자에게 PREP_EXTENDED 알림 기록을 요청한다
- [ ] #119 [서비스] 자녀 위치는 위치 동의가 없으면 LOCATION_CONSENT_REQUIRED로 거절한다
- [ ] #120 [서비스] 자녀 위치는 끝난 세션이면 SESSION_ALREADY_FINISHED로 거절한다
- [ ] #121 [서비스] 자녀 위치는 WAITING 자녀면 409로 거절한다
- [ ] #122 [서비스] 자녀 위치는 CALLED 자녀의 lastLat · lastLng · lastLocatedAt을 덮어쓴다
- [ ] #123 [서비스] 도움 요청은 HELP_REQUESTED(payload childUserId · type)를 저장하고 보호자에게 HELP 알림 기록을 요청하고 nextAllowedAt = 지금 + 60초를 돌려준다
- [ ] #124 [서비스] 도움 요청 1분 제한은 형제의 요청에 영향을 받지 않는다
- [ ] #125 [서비스] 탑승 완료 IMMEDIATE는 PickupOutcome을 boardingMethod IMMEDIATE로 저장하고 SESSION_COMPLETED를 저장한다
- [ ] #126 [서비스] 탑승 완료 MANUAL은 미탑승 지정 외 자녀의 boardedAt을 채우고 boardingMethod MANUAL로 저장한다
- [ ] #127 [서비스] 탑승 완료는 PickupOutcome.actualStopAt에 VEHICLE_STOPPED의 occurredAt을 넣는다
- [ ] #128 [서비스] 자동 완료 작업은 취소 창이 지나고 모든 자녀가 탄 세션을 boardingMethod DOOR로 완료한다
- [ ] #129 [서비스] 자동 완료 작업은 SES-12로 boardedAt이 비워진 자녀가 있는 세션을 완료하지 않는다
- [ ] #130 [서비스] 탑승 취소는 자녀가 여러 명인데 childUserId가 없으면 VALIDATION_FAILED로 거절한다
- [ ] #131 [서비스] 탑승 예외는 INCIDENT_OPENED를 저장한다
- [ ] #132 [서비스] 픽업 이력은 month 2026-11에 한국 날짜 11월 1일~30일 세션을 돌려준다
- [ ] #133 [서비스] 픽업 결과는 예상 18:11:00 · 실제 18:11:50이면 diffSeconds 50을 돌려준다
- [ ] #134 [서비스] 결과 정정은 actualStopAt을 그대로 두고 correctedStopAt · correctionReason · correctionStatus REQUESTED · correctedBy를 채운다
- [ ] #135 [서비스] 결과 정정은 correctedStopAt이 세션 날짜 밖이면 VALIDATION_FAILED로 거절한다
- [ ] #136 [서비스] 데이터 설정 변경은 learningConsent와 retentionDays 90을 돌려준다
- [ ] #137 [서비스] SSE 리스너는 커밋된 세션 이벤트를 그 세션 차량의 연결로 보낸다
- [ ] #138 [web] GET /v1/pickup-sessions/active는 진행 중 세션이 있으면 200과 sessionId · status · stateVersion을 돌려준다
- [ ] #139 [web] GET /v1/pickup-sessions/active는 진행 중 세션이 없으면 204를 돌려준다
- [ ] #140 [web] POST /v1/pickup-sessions는 새 세션이면 201과 SessionView를 돌려준다
- [ ] #141 [web] POST /v1/pickup-sessions는 진행 중인 같은 세션이면 200을 돌려준다
- [ ] #142 [web] POST /v1/pickup-sessions가 오늘이 아니면 422 NOT_TODAY를 돌려준다
- [ ] #143 [web] POST /v1/pickup-sessions에 빈 scheduleIds를 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #144 [web] POST /v1/pickup-sessions를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #145 [web] GET /v1/pickup-sessions/{sessionId}는 200과 SessionView 필드를 돌려준다
- [ ] #146 [web] GET /v1/pickup-sessions/{sessionId}의 시각 필드는 +09:00으로 내려간다
- [ ] #147 [web] PUT /v1/pickup-sessions/{sessionId}/route는 200과 SessionView를 돌려준다
- [ ] #148 [web] PUT /v1/pickup-sessions/{sessionId}/route가 저장하지 않은 장소면 422 SPOT_NOT_SAVED를 돌려준다
- [ ] #149 [web] PUT /v1/pickup-sessions/{sessionId}/route의 stateVersion이 다르면 409 STATE_VERSION_CONFLICT와 최신 세션 뷰를 돌려준다
- [ ] #150 [web] POST /v1/pickup-sessions/{sessionId}/spot-changes는 200과 SessionView를 돌려준다
- [ ] #151 [web] POST /v1/pickup-sessions/{sessionId}/spot-changes가 자녀 이동 중이면 409 CHILD_ALREADY_MOVING을 돌려준다
- [ ] #152 [web] POST /v1/pickup-sessions/{sessionId}/spot-changes를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #153 [web] PUT /v1/pickup-sessions/{sessionId}/hold는 200과 status.session ON_HOLD인 SessionView를 돌려준다
- [ ] #154 [web] DELETE /v1/pickup-sessions/{sessionId}/hold는 쿼리 stateVersion을 받아 200과 status.session ACTIVE인 SessionView를 돌려준다
- [ ] #155 [web] POST /v1/pickup-sessions/{sessionId}/cancellation은 200과 status.session CANCELLED인 SessionView를 돌려준다
- [ ] #156 [web] POST /v1/pickup-sessions/{sessionId}/cancellation이 끝난 세션이면 409 SESSION_ALREADY_FINISHED를 돌려준다
- [ ] #157 [web] POST /v1/pickup-sessions/{sessionId}/cancellation을 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #158 [web] GET /v1/pickup-sessions/{sessionId}/stream은 그 세션 차량 토큰에 text/event-stream으로 응답한다
- [ ] #159 [web] GET /v1/pickup-sessions/{sessionId}/stream을 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #160 [web] GET /v1/pickup-sessions/{sessionId}/stream에 Last-Event-ID 41을 보내면 seq 42 이후 저장 이벤트부터 보낸다
- [ ] #161 [web] GET /v1/pickup-sessions/{sessionId}/stream은 이벤트를 event · id(seq) · data(eventId · seq · stateVersion · occurredAt · payload) 형식으로 보낸다
- [ ] #162 [web] GET /v1/pickup-sessions/{sessionId}/stream은 ping 간격마다 빈 주석 `: ping`을 보낸다
- [ ] #163 [web] GET /v1/pickup-sessions/{sessionId}/events는 200과 items[] · hasMore를 돌려준다
- [ ] #164 [web] GET /v1/pickup-sessions/{sessionId}/events에 limit 101을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #165 [web] GET /v1/pickup-sessions/{sessionId}/contacts는 200과 items[](name · role · phone)를 돌려준다
- [ ] #166 [web] POST /v1/pickup-sessions/{sessionId}/completion은 200과 status.session COMPLETED인 SessionView를 돌려준다
- [ ] #167 [web] POST /v1/pickup-sessions/{sessionId}/completion이 MANUAL인데 정차 중이 아니면 422 VEHICLE_NOT_STOPPED를 돌려준다
- [ ] #168 [web] POST /v1/pickup-sessions/{sessionId}/completion이 빠진 자녀가 있으면 422 CHILDREN_NOT_ALL_BOARDED를 돌려준다
- [ ] #169 [web] POST /v1/pickup-sessions/{sessionId}/boarding-rejection은 200과 SessionView를 돌려준다
- [ ] #170 [web] POST /v1/pickup-sessions/{sessionId}/boarding-rejection이 10초가 지났으면 409 REJECTION_WINDOW_CLOSED를 돌려준다
- [ ] #171 [web] PUT /v1/pickup-sessions/{sessionId}/incident는 200과 incident가 채워진 SessionView를 돌려준다
- [ ] #172 [web] PUT /v1/pickup-sessions/{sessionId}/incident가 이미 열린 예외면 409 INCIDENT_ALREADY_OPEN을 돌려준다
- [ ] #173 [web] POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry는 202를 돌려준다
- [ ] #174 [web] POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry에 음수 navEtaSeconds를 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #175 [web] POST /v1/pickup-sessions/{sessionId}/vehicle-events는 202를 돌려준다
- [ ] #176 [web] POST /v1/pickup-sessions/{sessionId}/vehicle-events에 door 없는 DOOR_CYCLE을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #177 [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions는 200과 extraPrepMinutes · callNotBefore를 돌려준다
- [ ] #178 [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions에 minutes 7을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #179 [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions가 누적 15분을 넘으면 422 PREP_EXTENSION_LIMIT을 돌려준다
- [ ] #180 [web] POST /v1/pickup-sessions/{sessionId}/prep-time-extensions를 GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #181 [web] POST /v1/pickup-sessions/{sessionId}/child-locations는 202를 돌려준다
- [ ] #182 [web] POST /v1/pickup-sessions/{sessionId}/child-locations가 동의 없으면 403 LOCATION_CONSENT_REQUIRED를 돌려준다
- [ ] #183 [web] POST /v1/pickup-sessions/{sessionId}/help-requests는 201과 nextAllowedAt을 돌려준다
- [ ] #184 [web] POST /v1/pickup-sessions/{sessionId}/help-requests가 1분 안 재요청이면 429 HELP_REQUEST_TOO_SOON과 nextAllowedAt을 돌려준다
- [ ] #185 [web] PUT /v1/pickup-sessions/{sessionId}/call-hold는 200과 call.held true인 SessionView를 돌려준다
- [ ] #186 [web] PUT /v1/pickup-sessions/{sessionId}/call-hold가 발송 뒤면 409 CALL_ALREADY_SENT를 돌려준다
- [ ] #187 [web] DELETE /v1/pickup-sessions/{sessionId}/call-hold는 쿼리 stateVersion을 받아 200과 call.held false인 SessionView를 돌려준다
- [ ] #188 [web] POST /v1/pickup-sessions/{sessionId}/calls는 201과 callId · status를 돌려준다
- [ ] #189 [web] POST /v1/pickup-sessions/{sessionId}/calls가 가드 미충족이면 422 CALL_GUARD_UNMET과 unmet을 돌려준다
- [ ] #190 [web] POST /v1/pickup-sessions/{sessionId}/calls가 AUTO 가족이면 409 NOT_GUARDIAN_CONFIRM_MODE를 돌려준다
- [ ] #191 [web] POST /v1/calls/{callId}/revocation은 200과 status.call REVOKED인 SessionView를 돌려준다
- [ ] #192 [web] POST /v1/calls/{callId}/revocation이 이미 철회된 호출이면 409 CALL_ALREADY_REVOKED를 돌려준다
- [ ] #193 [web] POST /v1/calls/{callId}/revocation을 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #194 [web] POST /v1/calls/{callId}/acknowledgement는 200과 callId · status · acknowledgedAt을 돌려준다
- [ ] #195 [web] POST /v1/calls/{callId}/acknowledgement를 차량 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #196 [web] GET /v1/children/{childId}/pickup-history는 200과 items[](date · sessionId · status · completedAt)를 돌려준다
- [ ] #197 [web] GET /v1/children/{childId}/pickup-history에 month 2026-13을 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #198 [web] GET /v1/pickup-sessions/{sessionId}/outcome은 200과 completedAt · spotName · boardingMethod · predictedStopAt · actualStopAt · diffSeconds · correction을 돌려준다
- [ ] #199 [web] PUT /v1/pickup-sessions/{sessionId}/outcome/correction은 200과 correction.status REQUESTED를 돌려준다
- [ ] #200 [web] PUT /v1/pickup-sessions/{sessionId}/outcome/correction에 201자 note를 보내면 422 VALIDATION_FAILED를 돌려준다
- [ ] #201 [web] PUT /v1/guardians/me/data-settings는 200과 learningConsent · retentionDays를 돌려준다
- [ ] #202 [web] PUT /v1/guardians/me/data-settings를 CO_GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다
- [ ] #203 [web] Idempotency-Key는 SES-02 · 04~07 · 11~13, SIG-03 · 05, CALL-01~04에만 요구되고 SIG-01 · 02 · 04와 조회 · OUT API에는 요구되지 않는다
- [ ] #207 user · auth · place의 진행 중 픽업 확인 어댑터가 임시 false · 빈 값 대신 pickup ActivePickupQueryUseCase를 부른다
- [ ] #209 [persistence] GuardianDataSetting을 저장하고 읽으면 learningConsent가 같고 행이 없는 보호자는 빈 값을 돌려준다

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
| 「같은 시간대」 묶기 기준(SES-02) | 소스에 없음. 일정의 classEndTime + targetPickup(목표 픽업 시각)이 같으면 같은 시간대로 가정 |
| 「진행 중인 같은 세션」(SES-02 200) | 같은 차량 · 같은 date · 같은 scheduleIds 묶음으로 가정. 다르면 409 ANOTHER_SESSION_ACTIVE |
| DRAFT 일정으로 시작할 때 오류 코드 | 소스는 「픽업 시작 불가」만 있음. 422 SETUP_INCOMPLETE로 가정 |
| 일정 값 스냅샷 컬럼 | 소스는 schedule 값도 스냅샷한다고 했지만 SessionChild에는 scheduleId만 있음. 지연 판정(목표 시각 + 10분)에 쓸 목표 시각을 어디서 읽을지 정해야 함 |
| 합류 안내 방식 스냅샷 | SET-06은 「다음 세션부터 적용」인데 PickupSession에 mode 컬럼이 없음. 지금은 판정 때 Family.joinCallMode를 읽는다고 가정(진행 중에 바뀌면 바로 반영됨) |
| G3 「경로 일치」의 판정 | 소스에 정의 없음. SES-04로 확정한 장소 버전과 지금 spotVersion이 같으면 통과로 가정 |
| G5와 형제 | Call은 세션에 하나라 모든 세션 자녀의 callNotBefore가 지나야 통과로 가정. callNotBefore가 지금과 같은 시각이면 지난 것으로 본다(「이 시각 전에는 호출 안 함」) |
| ETA_3MIN · DIST_700M이 동시에 맞을 때 triggerType | 소스에 없음. ETA_3MIN으로 가정 |
| vehiclePhase APPROACHING 기준 | 「SIG-01 ETA · 거리 기준」만 있음. 기준값을 정해야 함 |
| spotStatus ROUTING · INVALIDATED로 바꾸는 계기 | 이 상태로 바꾸는 API가 없음. 정해야 함 |
| navRouteId 저장 위치 | SES-04 요청에 있으나 컬럼이 없음. 저장하지 않는다고 가정 |
| SES-05 spotVersion 불일치 오류 | 소스에 없음. 409 STATE_VERSION_CONFLICT로 가정 |
| SES-05에서 CALL_ALREADY_SENT와 CHILD_ALREADY_MOVING이 함께 맞을 때 | MEETING이면 호출도 보낸 상태. CHILD_ALREADY_MOVING을 먼저 본다고 가정 |
| SES-06 · 07 reason 저장 위치 | 세션에 사유 컬럼이 없고 보류 이벤트도 19종에 없음. 취소 사유는 자동 철회 Call.revokeReason에 넣고 보류 사유는 저장하지 않는다고 가정 |
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
| incident의 childUserId 저장 | 세션 뷰에는 incident.childUserId가 있으나 컬럼이 없음. INCIDENT_OPENED payload에 저장하고 거기서 읽는다고 가정 |
| 예외 해소(incidentResolvedAt) 방법 | 해소 API가 없음. SES-06 DELETE(보류 해제) 때 채운다고 가정 |
| actualStopAt · predictedStopAt 값 | actualStopAt은 VEHICLE_STOPPED occurredAt. [사람 결정 2026-11-09] predictedStopAt은 SIG-02 STOPPED를 받을 때의 세션 etaAt(정차 직전 마지막 ETA)이고 VEHICLE_STOPPED payload에 담아 완료 때 읽는다. etaAt이 없으면 null |
| SIG-02 STOP_FAILED 처리 | 소스에 없음. 상태 변화 없이 무시로 가정 |
| SIG-02 eventId 저장 위치 | 「Idempotency-Key로 써서 중복 무시」. idempotency_key 테이블에 저장한다고 가정 |
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
| SessionEvent.type | [팀장 결정 2026-11-09] 도메인에서는 19종 enum(SessionEventType), 저장은 문자열 |
| 세션 뷰 조립 위치 | [팀장 결정 2026-11-09] SessionView는 port.in record라 application/service에서 조립(「구조」 절의 domain 조립 규칙 대신) |
| 위치 동의 저장 위치 | 동의 테이블이 없음(05_인증-auth.md에도 기록) |
| SIG-01이 끝난 세션에 올 때 | 소스에 없음. 202로 받고 무시한다고 가정 |
| 세션 행 잠금 범위 | 소스는 SIG-05만 명시. 세션을 바꾸는 모든 요청을 loadForUpdate로 처리한다고 가정 |
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
- 남은 항목 155건부터 계속: /tdd domain pickup → /tdd go
- 다음 도메인: 알림 (09_알림-notification.md). 시작: /tdd domain notification

## 후속 과제
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
