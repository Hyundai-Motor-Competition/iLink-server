package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.ReceiveTelemetryUseCase;
import com.example.pickupservice.pickup.application.port.in.TelemetryCommand;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SessionStatus;
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
		if (locked.getSessionStatus() == SessionStatus.COMPLETED
				|| locked.getSessionStatus() == SessionStatus.CANCELLED) {
			return;
		}
		if (isOlderThanLatest(locked, command.observedAt())) {
			return;
		}
		locked.applyTelemetry(command.observedAt(), command.navEtaSeconds(), command.remainingMeters());
		PickupSession session = saveSessionPort.save(locked);
		publishEtaUpdated(session, command.observedAt());
		callEngine.triggerIfReady(session, command.navEtaSeconds(), command.remainingMeters());
	}

	private static boolean isOlderThanLatest(PickupSession session, Instant observedAt) {
		Instant etaUpdatedAt = session.getEtaUpdatedAt();
		return etaUpdatedAt != null && observedAt.isBefore(etaUpdatedAt);
	}

	private void publishEtaUpdated(PickupSession session, Instant observedAt) {
		Map<String, Object> payload = Map.of("etaAt", TimeUtils.toKst(session.getEtaAt()),
				"remainingMeters", session.getRemainingMeters());
		publishSessionEventPort.publish(new SessionEventMessage(session.getSessionId(), null, null, ETA_UPDATED,
				session.getStateVersion(), observedAt, payload, List.of()));
	}

}
