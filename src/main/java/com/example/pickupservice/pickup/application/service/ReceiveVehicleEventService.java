package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.ReceiveVehicleEventUseCase;
import com.example.pickupservice.pickup.application.port.in.VehicleEventCommand;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.ProcessedVehicleEventPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReceiveVehicleEventService implements ReceiveVehicleEventUseCase {

	private static final String VEHICLE_STOPPED = SessionEventType.VEHICLE_STOPPED.name();
	private static final String VEHICLE_DEPARTED = SessionEventType.VEHICLE_DEPARTED.name();
	private static final String CALL_REVOKED = SessionEventType.CALL_REVOKED.name();
	private static final String BOARDING_DETECTED = SessionEventType.BOARDING_DETECTED.name();

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadCallPort loadCallPort;
	private final LoadFamilyPort loadFamilyPort;
	private final ProcessedVehicleEventPort processedVehicleEventPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final PickupProperties pickupProperties;
	private final SaveCallPort saveCallPort;
	private final CallEngine callEngine;

	@Override
	public void receive(VehicleEventCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		if (!processedVehicleEventPort.markProcessed(command.sessionId(), command.eventId())) {
			return;
		}
		switch (command.type()) {
			case "STOPPED" -> stopped(locked, command.occurredAt());
			case "DEPARTED" -> departed(locked, command.occurredAt());
			case "STOP_FAILED" -> stopFailed(locked, command.occurredAt());
			case "DOOR_CYCLE" -> doorCycle(locked, command);
			default -> {
			}
		}
	}

	private void stopped(PickupSession locked, Instant occurredAt) {
		Map<String, Object> payload = new LinkedHashMap<>();
		Instant etaAt = locked.getEtaAt();
		payload.put("predictedStopAt", etaAt == null ? null : TimeUtils.toKst(etaAt));
		locked.markStopped();
		PickupSession session = saveSessionPort.save(locked);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), VEHICLE_STOPPED,
				session.getStateVersion(), occurredAt, payload);
		publish(event, guardiansAndSessionChildren(session));
		if (hasStoredEta(session)) {
			int navEtaSeconds = (int) Duration.between(session.getEtaUpdatedAt(), session.getEtaAt()).toSeconds();
			callEngine.triggerIfReady(session, navEtaSeconds, session.getRemainingMeters());
		}
	}

	private void departed(PickupSession locked, Instant occurredAt) {
		locked.markDeparted();
		Optional<Call> liveCall = keepsCalls(locked) ? Optional.empty()
				: loadCallPort.loadLiveCall(locked.getSessionId());
		liveCall.ifPresent(call -> revokeCall(call, locked, "VEHICLE_DEPARTED", occurredAt));
		PickupSession session = saveSessionPort.save(locked);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), VEHICLE_DEPARTED,
				session.getStateVersion(), occurredAt, Map.of());
		List<Long> receivers = guardiansAndSessionChildren(session);
		publish(event, receivers);
		if (liveCall.isPresent()) {
			appendAndPublishCallRevoked(session, occurredAt, receivers);
		}
	}

	private void stopFailed(PickupSession locked, Instant occurredAt) {
		if (keepsCalls(locked)) {
			return;
		}
		Optional<Call> liveCall = loadCallPort.loadLiveCall(locked.getSessionId());
		if (liveCall.isEmpty()) {
			return;
		}
		revokeCall(liveCall.get(), locked, "STOP_FAILED", occurredAt);
		PickupSession session = saveSessionPort.save(locked);
		appendAndPublishCallRevoked(session, occurredAt, guardiansAndSessionChildren(session));
	}

	private void revokeCall(Call call, PickupSession session, String reason, Instant at) {
		try {
			call.revoke(reason, at);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
		session.revertChildrenToWaiting();
		saveCallPort.saveCall(call);
	}

	private void appendAndPublishCallRevoked(PickupSession session, Instant at, List<Long> receivers) {
		SessionEvent event = sessionEventPort.append(session.getSessionId(), CALL_REVOKED,
				session.getStateVersion(), at, Map.of());
		publish(event, receivers);
	}

	private static boolean keepsCalls(PickupSession session) {
		return session.isFinished() || session.getBoardingDetectedAt() != null;
	}

	private static boolean hasStoredEta(PickupSession session) {
		return session.getEtaAt() != null && session.getRemainingMeters() != null
				&& session.getEtaUpdatedAt() != null;
	}

	private void doorCycle(PickupSession locked, VehicleEventCommand command) {
		if (locked.isFinished()) {
			return;
		}
		boolean callSent = loadCallPort.loadLiveCall(locked.getSessionId())
				.filter(call -> call.getStatus() != CallStatus.ACCEPTED)
				.isPresent();
		if (!detectBoarding(locked, BoardingDoor.valueOf(command.door()), command.occurredAt(), callSent)) {
			return;
		}
		PickupSession session = saveSessionPort.save(locked);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), BOARDING_DETECTED,
				session.getStateVersion(), command.occurredAt(), Map.of());
		publish(event, guardians(session));
	}

	private boolean detectBoarding(PickupSession session, BoardingDoor door, Instant at, boolean callSent) {
		try {
			return session.detectBoarding(door, at, callSent, pickupProperties.boardingCancelWindow());
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
	}

	private void publish(SessionEvent event, List<Long> receiverUserIds) {
		publishSessionEventPort.publish(SessionEventMessages.of(event, receiverUserIds));
	}

	private List<Long> guardians(PickupSession session) {
		return SessionReceivers.guardians(loadFamilyPort.loadMembers(session.getFamilyId()));
	}

	private List<Long> guardiansAndSessionChildren(PickupSession session) {
		return SessionReceivers.guardiansAndSessionChildren(session,
				loadFamilyPort.loadMembers(session.getFamilyId()));
	}

}
