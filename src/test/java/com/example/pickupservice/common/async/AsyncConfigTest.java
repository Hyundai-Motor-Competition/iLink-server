package com.example.pickupservice.common.async;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class AsyncConfigTest extends IntegrationTestSupport {

	private static final String EVENT_TASK_EXECUTOR_BEAN = "eventTaskExecutor";
	private static final String EVENT_THREAD_NAME_PREFIX = "event-";
	private static final long WAIT_SECONDS = 5;
	private static final long LISTENER_HOLD_SECONDS = 2;

	private final ApplicationContext applicationContext;
	private final ApplicationEventPublisher eventPublisher;
	private final TransactionTemplate transactionTemplate;
	private final AsyncMethodProbe asyncMethodProbe;
	private final AfterCommitListenerRecord afterCommitListenerRecord;

	@Test
	@DisplayName("@Async를 붙인 메서드를 부르면 호출한 스레드가 아니라 이벤트 스레드 풀의 event- 스레드에서 실행된다")
	void asyncMethodRunsOnEventPoolThreadInsteadOfCallerThread() throws Exception {
		String callerThreadName = Thread.currentThread().getName();

		String executedThreadName = asyncMethodProbe.currentThreadName().get(WAIT_SECONDS, SECONDS);

		assertThat(executedThreadName)
				.isNotEqualTo(callerThreadName)
				.startsWith(EVENT_THREAD_NAME_PREFIX);
	}

	@Test
	@DisplayName("@TransactionalEventListener(AFTER_COMMIT)와 @Async를 붙인 리스너는 커밋 뒤 event- 스레드에서 실행되고 커밋한 쪽은 리스너가 끝나기를 기다리지 않는다")
	void afterCommitAsyncListenerRunsOnEventPoolThreadWithoutBlockingCommitter() throws Exception {
		String committerThreadName = Thread.currentThread().getName();

		transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(new ProbeEvent()));
		boolean listenerFinishedBeforeCommitReturned = afterCommitListenerRecord.finished.getCount() == 0;
		afterCommitListenerRecord.release.countDown();

		assertThat(afterCommitListenerRecord.finished.await(WAIT_SECONDS, SECONDS)).isTrue();
		assertThat(afterCommitListenerRecord.threadName)
				.isNotEqualTo(committerThreadName)
				.startsWith(EVENT_THREAD_NAME_PREFIX);
		assertThat(listenerFinishedBeforeCommitReturned)
				.as("커밋한 쪽이 리스너가 끝날 때까지 기다렸다")
				.isFalse();
	}

	@Test
	@DisplayName("이벤트 스레드 풀은 이름이 eventTaskExecutor인 ThreadPoolTaskExecutor 빈이고 @Async 메서드는 이 빈의 스레드 이름 접두사 event-를 가진 스레드에서 실행된다")
	void eventTaskExecutorBeanIsThreadPoolUsedByAsyncMethods() throws Exception {
		assertThat(applicationContext.containsBean(EVENT_TASK_EXECUTOR_BEAN)).isTrue();
		ThreadPoolTaskExecutor eventTaskExecutor =
				applicationContext.getBean(EVENT_TASK_EXECUTOR_BEAN, ThreadPoolTaskExecutor.class);

		String executedThreadName = asyncMethodProbe.currentThreadName().get(WAIT_SECONDS, SECONDS);

		assertThat(eventTaskExecutor.getThreadNamePrefix()).isEqualTo(EVENT_THREAD_NAME_PREFIX);
		assertThat(executedThreadName).startsWith(eventTaskExecutor.getThreadNamePrefix());
	}

	record ProbeEvent() {
	}

	static class AsyncMethodProbe {

		@Async
		public CompletableFuture<String> currentThreadName() {
			return CompletableFuture.completedFuture(Thread.currentThread().getName());
		}

	}

	static class AfterCommitListenerRecord {

		final CountDownLatch release = new CountDownLatch(1);
		final CountDownLatch finished = new CountDownLatch(1);
		volatile String threadName;

	}

	@RequiredArgsConstructor
	static class AfterCommitAsyncListener {

		private final AfterCommitListenerRecord record;

		@Async
		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		public void on(ProbeEvent event) throws InterruptedException {
			record.threadName = Thread.currentThread().getName();
			record.release.await(LISTENER_HOLD_SECONDS, SECONDS);
			record.finished.countDown();
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class AsyncProbeConfiguration {

		@Bean
		AsyncMethodProbe asyncMethodProbe() {
			return new AsyncMethodProbe();
		}

		@Bean
		AfterCommitListenerRecord afterCommitListenerRecord() {
			return new AfterCommitListenerRecord();
		}

		@Bean
		AfterCommitAsyncListener afterCommitAsyncListener(AfterCommitListenerRecord afterCommitListenerRecord) {
			return new AfterCommitAsyncListener(afterCommitListenerRecord);
		}

	}

}
