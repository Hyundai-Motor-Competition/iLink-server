package com.example.pickupservice.common.fixture;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class TestPostgisConfigurationTest extends IntegrationTestSupport {

	private static final Duration IDLE = Duration.ofDays(1);
	private static final List<String> SCHEDULED_METHODS = List.of(
			"IdempotencyKeyCleaner.deleteExpired",
			"AutoCompleteBoardingScheduler.completeExpiredBoardings",
			"RevokeSignalLostCallsScheduler.revokeSignalLostCalls",
			"SessionStreamRegistry.ping",
			"ResendUndeliveredScheduler.resendUndelivered");

	private final ApplicationContext applicationContext;
	private final Environment environment;

	@Test
	@DisplayName("공통 테스트 설정은 Idempotency-Key 정리 · 자동 완료 · 신호 끊김 철회 · SSE ping · 미전달 알림 재발송 주기 설정을 모두 P1D로 둔다")
	void commonTestConfigurationSetsAllSchedulerIntervalsToOneDay() {
		assertThat(environment.getProperty("pickup.idempotency.cleanup-interval", Duration.class)).isEqualTo(IDLE);
		assertThat(environment.getProperty("pickup.session.auto-complete-interval", Duration.class)).isEqualTo(IDLE);
		assertThat(environment.getProperty("pickup.call.signal-lost-check-interval", Duration.class)).isEqualTo(IDLE);
		assertThat(environment.getProperty("pickup.sse.ping-interval", Duration.class)).isEqualTo(IDLE);
		assertThat(environment.getProperty("pickup.notification.resend-interval", Duration.class)).isEqualTo(IDLE);
	}

	@Test
	@DisplayName("공통 테스트 컨텍스트에 등록된 @Scheduled 작업 5개(정리 · 자동 완료 · 신호 끊김 철회 · SSE ping · 재발송)의 실제 실행 주기는 모두 1일 이상이다")
	void everyScheduledTaskInCommonTestContextRunsAtMostOncePerDay() {
		List<ScheduledTask> tasks = scheduledTasks();

		assertThat(tasks).extracting(ScheduledTask::toString)
				.anySatisfy(name -> assertThat(name).contains(SCHEDULED_METHODS.get(0)))
				.anySatisfy(name -> assertThat(name).contains(SCHEDULED_METHODS.get(1)))
				.anySatisfy(name -> assertThat(name).contains(SCHEDULED_METHODS.get(2)))
				.anySatisfy(name -> assertThat(name).contains(SCHEDULED_METHODS.get(3)))
				.anySatisfy(name -> assertThat(name).contains(SCHEDULED_METHODS.get(4)));
		assertThat(tasks).allSatisfy(scheduled -> {
			Task task = scheduled.getTask();
			assertThat(task).as("%s", scheduled).isInstanceOf(IntervalTask.class);
			assertThat(((IntervalTask) task).getIntervalDuration()).as("%s 주기", scheduled)
					.isGreaterThanOrEqualTo(IDLE);
		});
	}

	@Test
	@DisplayName("공통 테스트 컨텍스트에 등록된 @Scheduled 작업은 첫 실행 지연도 1일 이상이라 컨텍스트가 뜨자마자 공유 DB에서 돌지 않는다")
	void everyScheduledTaskInCommonTestContextDoesNotRunRightAfterStartup() {
		List<ScheduledTask> tasks = scheduledTasks();

		assertThat(tasks).isNotEmpty().allSatisfy(scheduled -> {
			Task task = scheduled.getTask();
			assertThat(task).as("%s", scheduled).isInstanceOf(IntervalTask.class);
			assertThat(((IntervalTask) task).getInitialDelayDuration()).as("%s 첫 실행 지연", scheduled)
					.isGreaterThanOrEqualTo(IDLE);
		});
	}

	private List<ScheduledTask> scheduledTasks() {
		return applicationContext.getBeansOfType(ScheduledTaskHolder.class).values().stream()
				.flatMap(holder -> holder.getScheduledTasks().stream())
				.toList();
	}

}
