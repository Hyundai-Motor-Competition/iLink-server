package com.example.pickupservice.notification.application.port.out;

import com.example.pickupservice.notification.domain.Notification;

public interface SaveNotificationPort {

	Notification save(Notification notification);
}
