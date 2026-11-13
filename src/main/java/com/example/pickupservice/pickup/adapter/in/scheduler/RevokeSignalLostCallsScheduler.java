package com.example.pickupservice.pickup.adapter.in.scheduler;

import com.example.pickupservice.pickup.application.port.in.RevokeSignalLostCallsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class RevokeSignalLostCallsScheduler {

	private final RevokeSignalLostCallsUseCase revokeSignalLostCallsUseCase;
	private final Clock clock;

	@Scheduled(fixedDelayString = "${pickup.call.signal-lost-check-interval}",
			initialDelayString = "${pickup.call.signal-lost-check-interval}")
	void revokeSignalLostCalls() {
		revokeSignalLostCallsUseCase.revokeSignalLostCalls(clock.instant());
	}

}
