package com.example.pickupservice.notification.application.port.in;

import java.util.List;

public interface ListNotificationsUseCase {

	List<NotificationView> list(NotificationListQuery query);
}
