package com.example.pickupservice.notification.adapter.in.web;

import com.example.pickupservice.notification.application.port.in.NotificationView;

import java.util.List;

public record NotificationsResponse(
		List<NotificationItemResponse> items
) {

	public static NotificationsResponse from(List<NotificationView> views) {
		return new NotificationsResponse(views.stream().map(NotificationItemResponse::from).toList());
	}

}
