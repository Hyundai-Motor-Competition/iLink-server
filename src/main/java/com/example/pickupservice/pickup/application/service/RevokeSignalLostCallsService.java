package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.RevokeSignalLostCallsUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevokeSignalLostCallsService implements RevokeSignalLostCallsUseCase {

	private static final String SIGNAL_LOST = "SIGNAL_LOST";
	private static final String CALL_REVOKED = SessionEventType.CALL_REVOKED.name();

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadCallPort loadCallPort;
	private final SaveCallPort saveCallPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final PickupProperties pickupProperties;
	private final TransactionOperations transactionOperations;

	@Override
	public int revokeSignalLostCalls(Instant now) {
		Instant staleBefore = now.minus(pickupProperties.etaValidFor());
		int revoked = 0;
		for (PickupSession signalLost : loadSessionPort.loadSignalLost(staleBefore)) {
			if (revokeInOwnTransaction(signalLost.getSessionId(), staleBefore, now)) {
				revoked++;
			}
		}
		return revoked;
	}

	private boolean revokeInOwnTransaction(long sessionId, Instant staleBefore, Instant now) {
		try {
			return Boolean.TRUE.equals(
					transactionOperations.execute(status -> revokeIfSignalLost(sessionId, staleBefore, now)));
		} catch (RuntimeException exception) {
			log.warn("signal lost revoke failed sessionId={}", sessionId, exception);
			return false;
		}
	}

	private boolean revokeIfSignalLost(long sessionId, Instant staleBefore, Instant now) {
		Optional<PickupSession> found = loadSessionPort.loadForUpdate(sessionId);
		if (found.isEmpty() || !isSignalLost(found.get(), staleBefore)) {
			return false;
		}
		PickupSession locked = found.get();
		Optional<Call> liveCall = loadCallPort.loadLiveCall(sessionId);
		if (liveCall.isEmpty()) {
			return false;
		}
		Call call = liveCall.get();
		call.revoke(SIGNAL_LOST, now);
		locked.revertChildrenToWaiting();
		saveCallPort.saveCall(call);
		PickupSession session = saveSessionPort.save(locked);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), CALL_REVOKED,
				session.getStateVersion(), now, Map.of());
		publishSessionEventPort.publish(SessionEventMessages.of(event,
				SessionReceivers.guardiansAndSessionChildren(session,
						loadFamilyPort.loadMembers(session.getFamilyId()))));
		return true;
	}

	private static boolean isSignalLost(PickupSession session, Instant staleBefore) {
		return session.getSessionStatus() == SessionStatus.ACTIVE
				&& session.getEtaUpdatedAt() != null
				&& session.getEtaUpdatedAt().isBefore(staleBefore)
				&& session.getBoardingDetectedAt() == null;
	}

}
