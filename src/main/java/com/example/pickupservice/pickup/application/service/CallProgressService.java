package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.CallProgressUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class CallProgressService implements CallProgressUseCase {

	private static final String CALL_SENT = SessionEventType.CALL_SENT.name();
	private static final String CALL_DELIVERED = SessionEventType.CALL_DELIVERED.name();

	private final LoadCallPort loadCallPort;
	private final SaveCallPort saveCallPort;
	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;

	@Override
	public void markCallSent(long callId, Instant sentAt) {
		Call first = loadCall(callId);
		if (first.getStatus() == CallStatus.REVOKED) {
			return;
		}
		PickupSession locked = loadLockedSession(first.getSessionId());
		Call call = loadCall(callId);
		if (call.getStatus() == CallStatus.REVOKED || call.getSentAt() != null) {
			return;
		}
		call.markSent(sentAt);
		locked.markChildrenCalled();
		saveCallPort.saveCall(call);
		PickupSession session = saveSessionPort.save(locked);
		publishToGuardians(session, sessionEventPort.append(session.getSessionId(), CALL_SENT,
				session.getStateVersion(), sentAt,
				Map.of("callId", call.getCallId(), "trigger", call.getTriggerType().name())));
	}

	@Override
	public void markCallDelivered(long callId, Instant deliveredAt) {
		Call first = loadCall(callId);
		if (first.getStatus() == CallStatus.REVOKED) {
			return;
		}
		PickupSession session = loadLockedSession(first.getSessionId());
		Call call = loadCall(callId);
		if (call.getStatus() == CallStatus.REVOKED || call.getDeliveredAt() != null) {
			return;
		}
		call.markDelivered(deliveredAt);
		saveCallPort.saveCall(call);
		publishToGuardians(session, sessionEventPort.append(session.getSessionId(), CALL_DELIVERED,
				session.getStateVersion(), deliveredAt, Map.of("callId", call.getCallId())));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isCallAcknowledged(long callId) {
		return loadCallPort.loadCall(callId)
				.map(call -> call.getStatus() == CallStatus.ACKNOWLEDGED)
				.orElse(false);
	}

	private Call loadCall(long callId) {
		return loadCallPort.loadCall(callId).orElseThrow(PickupErrors::notFound);
	}

	private PickupSession loadLockedSession(long sessionId) {
		return loadSessionPort.loadForUpdate(sessionId).orElseThrow(PickupErrors::notFound);
	}

	private void publishToGuardians(PickupSession session, SessionEvent event) {
		publishSessionEventPort.publish(SessionEventMessages.of(event,
				SessionReceivers.guardians(loadFamilyPort.loadMembers(session.getFamilyId()))));
	}

}
