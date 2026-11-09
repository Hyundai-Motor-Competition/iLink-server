package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTrigger;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.Guard;
import com.example.pickupservice.pickup.domain.GuardResult;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.user.domain.JoinCallMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

// 합류 호출 판정(SIG-01 · CALL-01 DELETE)과 호출 생성(판정 6단계, CALL-02 MANUAL도 같이 쓴다)
@Component
@RequiredArgsConstructor
public class CallEngine {

	private static final String CALL_TRIGGERED = SessionEventType.CALL_TRIGGERED.name();
	private static final String CALL_NOTIFICATION = "CALL";

	private final LoadCallPort loadCallPort;
	private final SaveCallPort saveCallPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final RecordNotificationPort recordNotificationPort;
	private final PickupProperties pickupProperties;
	private final Clock clock;

	// 판정 1~5단계를 모두 통과하면 6단계로 호출을 만든다
	public void triggerIfReady(PickupSession session, int navEtaSeconds, int remainingMeters) {
		if (session.getSessionStatus() != SessionStatus.ACTIVE || session.isCallHeld()) {
			return;
		}
		if (loadCallPort.loadLiveCall(session.getSessionId()).isPresent()) {
			return;
		}
		CallTrigger callTrigger = new CallTrigger((int) pickupProperties.callTriggerEta().toSeconds(),
				pickupProperties.callTriggerDistanceMeters());
		Optional<CallTriggerType> triggerType = callTrigger.evaluate(navEtaSeconds, remainingMeters);
		if (triggerType.isEmpty()) {
			return;
		}
		Instant now = clock.instant();
		if (!evaluateGuard(session, now).met()) {
			return;
		}
		long familyId = session.getFamilyId();
		if (loadFamilyPort.loadFamily(familyId).joinCallMode() != JoinCallMode.AUTO) {
			return;
		}
		createCall(session, triggerType.get(), now);
	}

	public GuardResult evaluateGuard(PickupSession session, Instant now) {
		return new Guard(pickupProperties.etaValidFor()).evaluate(session, now);
	}

	// 판정 6단계: Call 저장 → CALL 알림 기록 요청 → CALL_TRIGGERED 저장 · 보호자에게 발행
	public Call createCall(PickupSession session, CallTriggerType triggerType, Instant now) {
		long sessionId = session.getSessionId();
		Call call = saveCallPort.saveCall(newCall(sessionId, triggerType, session.getSpotVersion()));
		recordNotificationPort.recordNotifications(new NotificationRequest(sessionId, call.getCallId(),
				CALL_NOTIFICATION, SessionReceivers.sessionChildren(session)));
		SessionEvent event = sessionEventPort.append(sessionId, CALL_TRIGGERED, session.getStateVersion(), now,
				Map.of("callId", call.getCallId(), "trigger", triggerType.name()));
		publishSessionEventPort.publish(SessionEventMessages.of(event,
				SessionReceivers.guardians(loadFamilyPort.loadMembers(session.getFamilyId()))));
		return call;
	}

	// 가장 최근 호출이 철회됐으면 그 호출을 prevCallId로 잇는다
	private Call newCall(long sessionId, CallTriggerType triggerType, int spotVersion) {
		Optional<Call> revoked = loadCallPort.loadLatestCall(sessionId)
				.filter(latest -> latest.getStatus() == CallStatus.REVOKED);
		if (revoked.isPresent()) {
			return Call.acceptAfter(sessionId, revoked.get().getCallId(), triggerType, spotVersion);
		}
		return Call.accept(sessionId, triggerType, spotVersion);
	}

}
