package com.example.pickupservice.notification.adapter.in.web;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.notification.application.port.in.ListNotificationsUseCase;
import com.example.pickupservice.notification.application.port.in.RecordDeliveryUseCase;
import com.example.pickupservice.notification.application.port.in.ResendNotificationUseCase;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class NotificationWebTestSupport extends IntegrationTestSupport {

	@MockitoBean
	protected RecordDeliveryUseCase recordDeliveryUseCase;

	@MockitoBean
	protected ListNotificationsUseCase listNotificationsUseCase;

	@MockitoBean
	protected ResendNotificationUseCase resendNotificationUseCase;

}
