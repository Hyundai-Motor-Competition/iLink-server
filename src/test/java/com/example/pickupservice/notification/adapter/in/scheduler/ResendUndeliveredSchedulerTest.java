package com.example.pickupservice.notification.adapter.in.scheduler;

import com.example.pickupservice.notification.application.port.in.ResendUndeliveredUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResendUndeliveredSchedulerTest {

	private static final Instant NOW = Instant.parse("2026-11-09T06:10:00Z");

	@Test
	@DisplayName("재발송 스케줄러는 clock.instant()로 resendUndelivered를 부른다")
	void callsUseCaseWithClockInstant() {
		ResendUndeliveredUseCase useCase = mock(ResendUndeliveredUseCase.class);
		ResendUndeliveredScheduler scheduler = new ResendUndeliveredScheduler(useCase, Clock.fixed(NOW, ZoneOffset.UTC));

		scheduler.resendUndelivered();

		verify(useCase).resendUndelivered(NOW);
	}

	@Test
	@DisplayName("재발송 스케줄러의 resendUndelivered에는 @Scheduled가 붙어 있고 1분(60000ms)마다 돈다")
	void isScheduledEveryMinute() throws NoSuchMethodException {
		Method method = ResendUndeliveredScheduler.class.getDeclaredMethod("resendUndelivered");

		Scheduled scheduled = method.getAnnotation(Scheduled.class);

		assertThat(scheduled).isNotNull();
		assertThat(scheduled.fixedDelay()).isEqualTo(60_000L);
	}
}
