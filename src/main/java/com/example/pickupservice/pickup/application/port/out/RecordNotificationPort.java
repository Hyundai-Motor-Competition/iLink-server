package com.example.pickupservice.pickup.application.port.out;

import java.util.List;

public interface RecordNotificationPort {

	List<Long> recordNotifications(NotificationRequest request);

}
