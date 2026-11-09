package com.example.pickupservice.notification.adapter.in.scheduler;

import com.example.pickupservice.notification.application.port.in.ResendUndeliveredUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class ResendUndeliveredScheduler {

	private final ResendUndeliveredUseCase resendUndeliveredUseCase;
	private final Clock clock;

	@Scheduled(fixedDelay = 60_000L)
	void resendUndelivered() {
		resendUndeliveredUseCase.resendUndelivered(clock.instant());
	}
}
