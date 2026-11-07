package com.example.pickupservice.auth.adapter.out.event;

import com.example.pickupservice.auth.application.port.out.DeviceReplacedEvent;
import com.example.pickupservice.auth.application.port.out.PublishAuthEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class AuthEventPublisher implements PublishAuthEventPort {

	private final ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void publishDeviceReplaced(List<Long> previousDeviceIds) {
		applicationEventPublisher.publishEvent(new DeviceReplacedEvent(previousDeviceIds));
	}
}
