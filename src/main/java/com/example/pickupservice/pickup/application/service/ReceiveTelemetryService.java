package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.ReceiveTelemetryUseCase;
import com.example.pickupservice.pickup.application.port.in.TelemetryCommand;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class ReceiveTelemetryService implements ReceiveTelemetryUseCase {

	private static final String ETA_UPDATED = SessionEventType.ETA_UPDATED.name();
	private static final String VEHICLE_APPROACHING = SessionEventType.VEHICLE_APPROACHING.name();
	private static final String PREPARE_NOTICE_SENT = SessionEventType.PREPARE_NOTICE_SENT.name();
	private static final String PREPARE_NOTIFICATION = "PREPARE";
	private static final int PREPARE_METERS = 700;
	private static final int PREPARE_ETA_SECONDS = 180;

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final CallEngine callEngine;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionEventPort sessionEventPort;
	private final RecordNotificationPort recordNotificationPort;
	private final Clock clock;

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
		VehiclePhase phaseBefore = locked.getVehiclePhase();
		locked.applyTelemetry(command.observedAt(), command.navEtaSeconds(), command.remainingMeters(),
				command.location());
		PickupSession session = saveSessionPort.save(locked);
		List<MemberInfo> members = loadFamilyPort.loadMembers(session.getFamilyId());
		if (phaseBefore == VehiclePhase.ROUTING && session.getVehiclePhase() == VehiclePhase.APPROACHING) {
			SessionEvent approaching = sessionEventPort.append(session.getSessionId(), VEHICLE_APPROACHING,
					session.getStateVersion(), clock.instant(), Map.of());
			publishSessionEventPort.publish(SessionEventMessages.of(approaching,
					SessionReceivers.guardiansAndSessionChildren(session, members)));
		}
		if (isWithinPrepareThreshold(command) && session.getSessionStatus() != SessionStatus.ON_HOLD
				&& sessionEventPort.loadFirst(session.getSessionId(), PREPARE_NOTICE_SENT).isEmpty()) {
			sendPrepareNotice(session, members);
		}
		publishEtaUpdated(session, command.observedAt());
		callEngine.triggerIfReady(session, command.navEtaSeconds(), command.remainingMeters());
	}

	private static boolean isWithinPrepareThreshold(TelemetryCommand command) {
		return command.remainingMeters() <= PREPARE_METERS || command.navEtaSeconds() <= PREPARE_ETA_SECONDS;
	}

	private void sendPrepareNotice(PickupSession session, List<MemberInfo> members) {
		List<Long> notificationIds = recordNotificationPort.recordNotifications(new NotificationRequest(
				session.getSessionId(), null, PREPARE_NOTIFICATION, SessionReceivers.sessionChildren(session)));
		SessionEvent notice = sessionEventPort.append(session.getSessionId(), PREPARE_NOTICE_SENT,
				session.getStateVersion(), clock.instant(), Map.of());
		publishSessionEventPort.publish(
				SessionEventMessages.of(notice, SessionReceivers.guardians(members), notificationIds));
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
