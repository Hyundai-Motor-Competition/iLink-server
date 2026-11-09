package com.example.pickupservice.notification.adapter.in.web;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.notification.application.port.in.NotificationView;

import java.time.Instant;

public record NotificationItemResponse(
		long notificationId,
		String type,
		String receiverName,
		String title,
		String sentAt,
		String deliveredAt,
		String openedAt,
		int attempt
) {

	public static NotificationItemResponse from(NotificationView view) {
		return new NotificationItemResponse(
				view.notificationId(),
				view.type(),
				view.receiverName(),
				view.title(),
				toKstOrNull(view.sentAt()),
				toKstOrNull(view.deliveredAt()),
				toKstOrNull(view.openedAt()),
				view.attempt());
	}

	private static String toKstOrNull(Instant instant) {
		return instant == null ? null : TimeUtils.toKst(instant);
	}

}
