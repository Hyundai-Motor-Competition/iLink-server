package com.example.pickupservice.notification.application.port.in;

public interface ResendNotificationUseCase {

	long resend(ResendCommand command);
}
