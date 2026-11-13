package com.example.pickupservice.common.async;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class SchedulingConfigTest extends IntegrationTestSupport {

	private static final int EXPECTED_SCHEDULING_POOL_SIZE = 4;
	private static final long WAIT_SECONDS = 5;
	private static final long OTHER_JOB_WAIT_SECONDS = 3;
	private static final long BLOCKING_JOB_MAX_HOLD_SECONDS = 10;

	private final ThreadPoolTaskScheduler taskScheduler;
	private final SchedulingProbeRecord schedulingProbeRecord;

	@Test
	@DisplayName("@Scheduled 작업을 돌리는 ThreadPoolTaskScheduler의 스레드 풀 크기는 spring.task.scheduling.pool.size 값인 4다")
	void scheduledTaskSchedulerPoolSizeIsFour() {
		int configuredPoolSize = taskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize();

		assertThat(configuredPoolSize).isEqualTo(EXPECTED_SCHEDULING_POOL_SIZE);
	}

	@Test
	@DisplayName("한 @Scheduled 작업이 막혀 있는 동안에도 다른 @Scheduled 작업이 다른 스레드에서 제때 실행된다")
	void otherScheduledJobRunsOnAnotherThreadWhileOneJobIsBlocked() throws Exception {
		assertThat(schedulingProbeRecord.blockingStarted.await(WAIT_SECONDS, SECONDS))
				.as("막히는 작업이 시작되지 않았다")
				.isTrue();

		boolean otherRanWhileBlocked = schedulingProbeRecord.otherRanWhileBlocked.await(OTHER_JOB_WAIT_SECONDS, SECONDS);
		schedulingProbeRecord.release.countDown();

		assertThat(otherRanWhileBlocked)
				.as("한 작업이 막혀 있는 동안 다른 작업이 실행되지 않았다(스케줄러 스레드를 나눠 쓴다)")
				.isTrue();
		assertThat(schedulingProbeRecord.otherThreadName)
				.isNotNull()
				.isNotEqualTo(schedulingProbeRecord.blockingThreadName);
	}

	static class SchedulingProbeRecord {

		final AtomicBoolean blockingClaimed = new AtomicBoolean(false);
		final CountDownLatch blockingStarted = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final CountDownLatch otherRanWhileBlocked = new CountDownLatch(1);
		volatile String blockingThreadName;
		volatile String otherThreadName;

	}

	@RequiredArgsConstructor
	static class BlockingScheduledJob {

		private final SchedulingProbeRecord record;

		@Scheduled(fixedDelay = 100L)
		public void run() throws InterruptedException {
			if (!record.blockingClaimed.compareAndSet(false, true)) {
				return;
			}
			record.blockingThreadName = Thread.currentThread().getName();
			record.blockingStarted.countDown();
			record.release.await(BLOCKING_JOB_MAX_HOLD_SECONDS, SECONDS);
		}

	}

	@RequiredArgsConstructor
	static class OtherScheduledJob {

		private final SchedulingProbeRecord record;

		@Scheduled(fixedDelay = 100L)
		public void run() {
			boolean blockingJobIsHolding = record.blockingStarted.getCount() == 0 && record.release.getCount() == 1;
			if (blockingJobIsHolding && record.otherRanWhileBlocked.getCount() == 1) {
				record.otherThreadName = Thread.currentThread().getName();
				record.otherRanWhileBlocked.countDown();
			}
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class SchedulingProbeConfiguration {

		@Bean
		SchedulingProbeRecord schedulingProbeRecord() {
			return new SchedulingProbeRecord();
		}

		@Bean
		BlockingScheduledJob blockingScheduledJob(SchedulingProbeRecord schedulingProbeRecord) {
			return new BlockingScheduledJob(schedulingProbeRecord);
		}

		@Bean
		OtherScheduledJob otherScheduledJob(SchedulingProbeRecord schedulingProbeRecord) {
			return new OtherScheduledJob(schedulingProbeRecord);
		}

	}

}
