package com.example.pickupservice.notification.application.port.out;

import com.example.pickupservice.notification.domain.Notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoadNotificationPort {

	Optional<Notification> load(long notificationId);

	List<Notification> loadBySession(long sessionId);

	List<Notification> loadPending(long sessionId, String type, Long callId);

	List<Notification> loadResendTargets(Instant now);

	int countAttempts(Notification notification);
}
