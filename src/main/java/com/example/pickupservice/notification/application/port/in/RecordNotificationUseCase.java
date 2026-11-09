package com.example.pickupservice.notification.application.port.in;

import java.util.List;

public interface RecordNotificationUseCase {

	List<Long> record(NotificationRequest request);
}
