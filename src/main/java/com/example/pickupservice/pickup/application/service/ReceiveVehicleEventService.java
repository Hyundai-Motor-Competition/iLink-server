package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.ReceiveVehicleEventUseCase;
import com.example.pickupservice.pickup.application.port.in.VehicleEventCommand;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.ProcessedVehicleEventPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class ReceiveVehicleEventService implements ReceiveVehicleEventUseCase {

	private static final String VEHICLE_STOPPED = SessionEventType.VEHICLE_STOPPED.name();
	private static final String VEHICLE_DEPARTED = SessionEventType.VEHICLE_DEPARTED.name();
	private static final String BOARDING_DETECTED = SessionEventType.BOARDING_DETECTED.name();

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadCallPort loadCallPort;
	private final LoadFamilyPort loadFamilyPort;
	private final ProcessedVehicleEventPort processedVehicleEventPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final PickupProperties pickupProperties;

	// 같은 eventId는 한 번만 반영한다. 원본은 저장하지 않고 결과만 남긴다
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
			case "DOOR_CYCLE" -> doorCycle(locked, command);
			default -> {
			}
		}
	}

	// predictedStopAt은 정차 직전 마지막 ETA(없으면 null)
	private void stopped(PickupSession locked, Instant occurredAt) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("predictedStopAt", locked.getEtaAt());
		locked.markStopped();
		PickupSession session = saveSessionPort.save(locked);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), VEHICLE_STOPPED,
				session.getStateVersion(), occurredAt, payload);
		publish(event, guardiansAndSessionChildren(session));
	}

	private void departed(PickupSession locked, Instant occurredAt) {
		locked.markDeparted();
		PickupSession session = saveSessionPort.save(locked);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), VEHICLE_DEPARTED,
				session.getStateVersion(), occurredAt, Map.of());
		publish(event, guardiansAndSessionChildren(session));
	}

	// 합류 안내를 보낸 뒤(철회되지 않은 호출이 ACCEPTED가 아님)이고 정차 중일 때만 탑승 후보
	private void doorCycle(PickupSession locked, VehicleEventCommand command) {
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
