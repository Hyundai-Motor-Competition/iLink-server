package com.example.pickupservice.pickup.adapter.out.event;

import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// SessionEventMessage를 그대로 스프링 이벤트로 발행한다. 커밋 뒤 전달은 받는 쪽 @TransactionalEventListener(AFTER_COMMIT)가 맡는다
@Component
@RequiredArgsConstructor
class SpringSessionEventPublisher implements PublishSessionEventPort {

	private final ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void publish(SessionEventMessage message) {
		applicationEventPublisher.publishEvent(message);
	}

}
