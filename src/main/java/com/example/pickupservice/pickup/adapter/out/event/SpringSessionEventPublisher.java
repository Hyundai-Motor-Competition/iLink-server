package com.example.pickupservice.pickup.adapter.out.event;

import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SpringSessionEventPublisher implements PublishSessionEventPort {

	private final ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void publish(SessionEventMessage message) {
		applicationEventPublisher.publishEvent(message);
	}

}
