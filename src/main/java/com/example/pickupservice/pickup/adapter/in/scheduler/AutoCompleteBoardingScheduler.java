package com.example.pickupservice.pickup.adapter.in.scheduler;

import com.example.pickupservice.pickup.application.port.in.AutoCompleteBoardingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class AutoCompleteBoardingScheduler {

	private final AutoCompleteBoardingUseCase autoCompleteBoardingUseCase;
	private final Clock clock;

	@Scheduled(fixedDelayString = "${pickup.session.auto-complete-interval}",
			initialDelayString = "${pickup.session.auto-complete-interval}")
	void completeExpiredBoardings() {
		autoCompleteBoardingUseCase.completeExpiredBoardings(clock.instant());
	}

}
