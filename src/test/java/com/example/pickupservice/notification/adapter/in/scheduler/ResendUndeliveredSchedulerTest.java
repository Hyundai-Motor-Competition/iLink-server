package com.example.pickupservice.notification.adapter.in.scheduler;

import com.example.pickupservice.common.fixture.MainApplicationYaml;
import com.example.pickupservice.notification.application.port.in.ResendUndeliveredUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResendUndeliveredSchedulerTest {

	private static final String INTERVAL_PROPERTY = "pickup.notification.resend-interval";
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
	@DisplayName("재발송 스케줄러의 resendUndelivered에는 @Scheduled(fixedDelayString = ${pickup.notification.resend-interval})가 붙어 있어 주기를 설정으로 바꿀 수 있다")
	void isScheduledWithIntervalFromConfiguration() throws NoSuchMethodException {
		Method method = ResendUndeliveredScheduler.class.getDeclaredMethod("resendUndelivered");

		Scheduled scheduled = method.getAnnotation(Scheduled.class);

		assertThat(scheduled).isNotNull();
		assertThat(scheduled.fixedDelayString()).isEqualTo("${" + INTERVAL_PROPERTY + "}");
		assertThat(scheduled.fixedDelay()).isEqualTo(-1L);
	}

	@Test
	@DisplayName("재발송 주기 설정 pickup.notification.resend-interval의 기본값(application.yml)은 지금처럼 1분이고 dev · prod 프로필도 그대로 1분이다")
	void resendIntervalDefaultsToOneMinuteInMainConfiguration() {
		assertThat(MainApplicationYaml.baseDuration(INTERVAL_PROPERTY)).isEqualTo(Duration.ofMinutes(1));
		assertThat(MainApplicationYaml.profileOverBaseDuration("prod", INTERVAL_PROPERTY)).isEqualTo(Duration.ofMinutes(1));
		assertThat(MainApplicationYaml.profileOverBaseDuration("dev", INTERVAL_PROPERTY)).isEqualTo(Duration.ofMinutes(1));
	}
}
