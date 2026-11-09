package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.ReceiveTelemetryUseCase;
import com.example.pickupservice.pickup.application.port.in.TelemetryCommand;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class ReceiveTelemetryService implements ReceiveTelemetryUseCase {

	private static final String ETA_UPDATED = SessionEventType.ETA_UPDATED.name();

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final CallEngine callEngine;

	@Override
	public void receive(TelemetryCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		if (isOlderThanLatest(locked, command.observedAt())) {
			return;
		}
		locked.applyTelemetry(command.observedAt(), command.navEtaSeconds(), command.remainingMeters());
		PickupSession session = saveSessionPort.save(locked);
		publishEtaUpdated(session, command.observedAt());
		callEngine.triggerIfReady(session, command.navEtaSeconds(), command.remainingMeters());
	}

	// 이미 반영한 값보다 먼저 잰 값이 늦게 오면 버린다
	private static boolean isOlderThanLatest(PickupSession session, Instant observedAt) {
		Instant etaUpdatedAt = session.getEtaUpdatedAt();
		return etaUpdatedAt != null && observedAt.isBefore(etaUpdatedAt);
	}

	// ETA_UPDATED는 저장하지 않아 eventId · seq가 없고 차량 SSE로만 간다
	private void publishEtaUpdated(PickupSession session, Instant observedAt) {
		Map<String, Object> payload = Map.of("etaAt", session.getEtaAt(),
				"remainingMeters", session.getRemainingMeters());
		publishSessionEventPort.publish(new SessionEventMessage(session.getSessionId(), null, null, ETA_UPDATED,
				session.getStateVersion(), observedAt, payload, List.of()));
	}

}
