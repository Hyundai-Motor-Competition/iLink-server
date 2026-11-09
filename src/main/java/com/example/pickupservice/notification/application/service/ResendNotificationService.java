package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.notification.application.port.in.ResendCommand;
import com.example.pickupservice.notification.application.port.in.ResendNotificationUseCase;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.PushMessage;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResendNotificationService implements ResendNotificationUseCase {

	private final LoadNotificationPort loadNotificationPort;
	private final SaveNotificationPort saveNotificationPort;
	private final SendPushPort sendPushPort;
	private final LoadPushTargetPort loadPushTargetPort;
	private final com.example.pickupservice.notification.application.port.out.LoadSessionFamilyPort loadSessionFamilyPort;
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
		Instant lastSentAt = lastSentAt(original);
		if (lastSentAt != null) {
			try {
				ResendPolicy.ensureResendAllowed(lastSentAt, clock.instant());
			} catch (NotificationRuleException e) {
				throw BusinessException.tooManyRequests(ErrorCode.RESEND_TOO_SOON, e.retryAfterSeconds());
			}
		}
		Notification created = saveNotificationPort.save(Notification.create(original.getSessionId(),
				original.getCallId(), original.getType(), original.getDeviceId()));
		Optional<PushTarget> target = loadPushTargetPort.loadPushTarget(created.getDeviceId());
		if (target.isPresent()) {
			Map<String, String> data = Map.of("notificationId", String.valueOf(created.getNotificationId()));
			PushMessage message = new PushMessage(target.get(), NotificationTexts.title(created.getType()),
					NotificationTexts.body(created.getType()), data);
			if (sendPushPort.send(message)) {
				created.markSent(clock.instant());
				saveNotificationPort.save(created);
			}
		}
		return created.getNotificationId();
	}

	private Instant lastSentAt(Notification original) {
		Instant latest = original.getSentAt();
		for (Notification row : loadNotificationPort.loadBySession(original.getSessionId())) {
			if (Objects.equals(row.getCallId(), original.getCallId()) && row.getType().equals(original.getType())
					&& row.getDeviceId() == original.getDeviceId() && row.getSentAt() != null
					&& (latest == null || row.getSentAt().isAfter(latest))) {
				latest = row.getSentAt();
			}
		}
		return latest;
	}
}
