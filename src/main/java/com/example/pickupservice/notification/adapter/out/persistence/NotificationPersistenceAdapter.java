package com.example.pickupservice.notification.adapter.out.persistence;

import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class NotificationPersistenceAdapter implements LoadNotificationPort, SaveNotificationPort {

	private final NotificationJpaRepository notificationJpaRepository;

	@Override
	public Optional<Notification> load(long notificationId) {
		return notificationJpaRepository.findById(notificationId).map(NotificationPersistenceAdapter::toDomain);
	}

	@Override
	public List<Notification> loadBySession(long sessionId) {
		return notificationJpaRepository.findBySessionIdOrderByNotificationIdAsc(sessionId).stream()
				.map(NotificationPersistenceAdapter::toDomain)
				.toList();
	}

	@Override
	public List<Notification> loadPending(long sessionId, String type, Long callId) {
		List<NotificationEntity> entities = callId == null
				? notificationJpaRepository.findBySessionIdAndTypeAndCallIdIsNullAndSentAtIsNull(sessionId, type)
				: notificationJpaRepository.findBySessionIdAndTypeAndCallIdAndSentAtIsNull(sessionId, type, callId);
		return entities.stream().map(NotificationPersistenceAdapter::toDomain).toList();
	}

	@Override
	public List<Notification> loadResendTargets(Instant now) {
		return notificationJpaRepository.findResendTargets(now.minusSeconds(60), now.minusSeconds(24 * 60 * 60)).stream()
				.map(NotificationPersistenceAdapter::toDomain)
				.toList();
	}

	@Override
	public int countAttempts(Notification notification) {
		long count = notification.getCallId() == null
				? notificationJpaRepository.countBySessionIdAndCallIdIsNullAndTypeAndDeviceId(
						notification.getSessionId(), notification.getType(), notification.getDeviceId())
				: notificationJpaRepository.countBySessionIdAndCallIdAndTypeAndDeviceId(
						notification.getSessionId(), notification.getCallId(), notification.getType(),
						notification.getDeviceId());
		return Math.toIntExact(count);
	}

	private static Notification toDomain(NotificationEntity entity) {
		return Notification.restore(entity.getNotificationId(), entity.getSessionId(), entity.getCallId(),
				entity.getType(), entity.getDeviceId(), entity.getSentAt(), entity.getDeliveredAt(),
				entity.getOpenedAt(), entity.getCreatedAt());
	}

	@Override
	public Notification save(Notification notification) {
		NotificationEntity entity = new NotificationEntity(notification.getNotificationId(), notification.getSessionId(),
				notification.getCallId(), notification.getType(), notification.getDeviceId(), notification.getSentAt(),
				notification.getDeliveredAt(), notification.getOpenedAt());
		return toDomain(notificationJpaRepository.save(entity));
	}

}
