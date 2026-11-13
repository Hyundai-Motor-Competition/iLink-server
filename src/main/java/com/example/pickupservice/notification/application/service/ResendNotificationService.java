package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.notification.application.port.in.ResendCommand;
import com.example.pickupservice.notification.application.port.in.ResendNotificationUseCase;
import com.example.pickupservice.notification.application.port.out.CallProgressPort;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.LoadResendGuardPort;
import com.example.pickupservice.notification.application.port.out.LoadSessionFamilyPort;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.application.port.out.SendPushPort;
import com.example.pickupservice.notification.domain.Notification;
import com.example.pickupservice.notification.domain.NotificationRuleException;
import com.example.pickupservice.notification.domain.ResendPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResendNotificationService implements ResendNotificationUseCase {

	private final LoadNotificationPort loadNotificationPort;
	private final SaveNotificationPort saveNotificationPort;
	private final SendPushPort sendPushPort;
	private final LoadPushTargetPort loadPushTargetPort;
	private final LoadSessionFamilyPort loadSessionFamilyPort;
	private final CallProgressPort callProgressPort;
	private final LoadResendGuardPort loadResendGuardPort;
	private final Clock clock;

	@Override
	@Transactional
	public long resend(ResendCommand command) {
		Notification original = loadNotificationPort.load(command.notificationId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		Optional<Long> familyId = loadSessionFamilyPort.loadFamilyId(original.getSessionId());
		if (familyId.isEmpty() || !familyId.get().equals(command.principal().familyId())) {
			throw BusinessException.of(ErrorCode.NOT_FOUND);
		}
		if (loadResendGuardPort.isSessionEnded(original.getSessionId())) {
			throw NotificationErrors.sessionAlreadyFinished();
		}
		if (original.getCallId() != null && loadResendGuardPort.isCallRevoked(original.getCallId())) {
			throw NotificationErrors.callAlreadyRevoked();
		}
		Instant lastSentAt = lastAttemptAt(original);
		if (lastSentAt != null) {
			Instant now = clock.instant();
			try {
				ResendPolicy.ensureResendAllowed(lastSentAt, now);
			} catch (NotificationRuleException e) {
				throw BusinessException.tooManyRequests(ErrorCode.RESEND_TOO_SOON,
						TimeUtils.secondsUntil(now, ResendPolicy.nextResendAllowedAt(lastSentAt)));
			}
		}
		Notification created = saveNotificationPort.save(Notification.create(original.getSessionId(),
				original.getCallId(), original.getType(), original.getDeviceId()));
		ResendDelivery.send(created, loadPushTargetPort, sendPushPort, saveNotificationPort, callProgressPort, clock);
		return created.getNotificationId();
	}

	private Instant lastAttemptAt(Notification original) {
		List<Notification> bundle = new ArrayList<>();
		bundle.add(original);
		for (Notification row : loadNotificationPort.loadBySession(original.getSessionId())) {
			if (Objects.equals(row.getCallId(), original.getCallId()) && row.getType().equals(original.getType())
					&& row.getDeviceId() == original.getDeviceId()) {
				bundle.add(row);
			}
		}
		return ResendPolicy.lastAttemptAt(bundle);
	}
}
