package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.user.application.port.in.MemberInfo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

final class DepartureNotice {

	private static final String DEPARTURE_NOTICE_SENT = SessionEventType.DEPARTURE_NOTICE_SENT.name();
	private static final String DEPARTURE_NOTIFICATION = "DEPARTURE";

	private DepartureNotice() {
	}

	static void send(PickupSession session, List<MemberInfo> members, Instant now,
			RecordNotificationPort recordNotificationPort, SessionEventPort sessionEventPort,
			PublishSessionEventPort publishSessionEventPort) {
		List<Long> notificationIds = recordNotificationPort.recordNotifications(
				new NotificationRequest(session.getSessionId(), null, DEPARTURE_NOTIFICATION,
						SessionReceivers.sessionChildren(session)));
		SessionEvent event = sessionEventPort.append(session.getSessionId(), DEPARTURE_NOTICE_SENT,
				session.getStateVersion(), now, Map.of());
		publishSessionEventPort.publish(SessionEventMessages.of(event, SessionReceivers.guardians(members),
				notificationIds));
	}

}
