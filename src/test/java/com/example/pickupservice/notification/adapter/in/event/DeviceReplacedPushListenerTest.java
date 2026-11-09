package com.example.pickupservice.notification.adapter.in.event;

import com.example.pickupservice.auth.application.port.out.DeviceReplacedEvent;
import com.example.pickupservice.common.async.AsyncConfig;
import com.example.pickupservice.notification.application.port.in.NotifyDeviceReplacedUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

// auth가 RELINK 때 발행하는 DeviceReplacedEvent를 받아 NotifyDeviceReplacedUseCase를 부르는 시점을 본다.
// DB 없이 가벼운 컨텍스트(트랜잭션 동기화 + AsyncConfig + 대역 유스케이스)로 확인한다.
@SpringJUnitConfig(DeviceReplacedPushListenerTest.TestConfig.class)
@TestPropertySource(properties = {
		"pickup.async.event.core-pool-size=2",
		"pickup.async.event.max-pool-size=4",
		"pickup.async.event.queue-capacity=100"
})
class DeviceReplacedPushListenerTest {

	private static final Duration WAIT_LIMIT = Duration.ofSeconds(5);
	private static final long POLL_MILLIS = 20L;
	private static final long SETTLE_MILLIS = 200L;

	@Autowired
	private ApplicationEventPublisher eventPublisher;
	@Autowired
	private TransactionTemplate transactionTemplate;
	@Autowired
	private RecordingNotify recordingNotify;

	@BeforeEach
	void clearCalls() {
		recordingNotify.calls.clear();
	}

	@Test
	@DisplayName("이전 기기 리스너는 커밋 뒤에 DeviceReplacedEvent의 이전 기기 id 목록으로 유스케이스를 부른다")
	void callsUseCaseWithPreviousDeviceIdsAfterCommit() {
		transactionTemplate.executeWithoutResult(status -> {
			eventPublisher.publishEvent(new DeviceReplacedEvent(List.of(701L, 702L)));
			sleep(SETTLE_MILLIS);
			assertThat(recordingNotify.calls).as("커밋 전에는 유스케이스를 부르지 않는다").isEmpty();
		});

		awaitCalls(1);

		assertThat(recordingNotify.calls).hasSize(1);
		assertThat(recordingNotify.calls.getFirst().previousDeviceIds()).containsExactly(701L, 702L);
	}

	@Test
	@DisplayName("이전 기기 리스너는 트랜잭션이 롤백되면 유스케이스를 부르지 않는다")
	void doesNotCallUseCaseWhenTransactionRollsBack() {
		transactionTemplate.executeWithoutResult(status -> {
			eventPublisher.publishEvent(new DeviceReplacedEvent(List.of(700L)));
			status.setRollbackOnly();
		});
		transactionTemplate.executeWithoutResult(
				status -> eventPublisher.publishEvent(new DeviceReplacedEvent(List.of(701L))));

		awaitCalls(1);
		sleep(SETTLE_MILLIS);

		assertThat(recordingNotify.calls).extracting(Call::previousDeviceIds).containsExactly(List.of(701L));
	}

	@Test
	@DisplayName("이전 기기 리스너는 요청 스레드와 다른 스레드에서 실행된다")
	void runsOnThreadDifferentFromRequestThread() {
		String requestThread = Thread.currentThread().getName();

		transactionTemplate.executeWithoutResult(
				status -> eventPublisher.publishEvent(new DeviceReplacedEvent(List.of(701L))));

		awaitCalls(1);

		String listenerThread = recordingNotify.calls.getFirst().threadName();
		assertThat(listenerThread).isNotEqualTo(requestThread);
		assertThat(listenerThread).startsWith("event-");
	}

	private void awaitCalls(int count) {
		long deadline = System.nanoTime() + WAIT_LIMIT.toNanos();
		while (recordingNotify.calls.size() < count && System.nanoTime() < deadline) {
			sleep(POLL_MILLIS);
		}
		assertThat(recordingNotify.calls).as("제한 시간 안에 유스케이스가 불리지 않았다").hasSizeGreaterThanOrEqualTo(count);
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	record Call(List<Long> previousDeviceIds, String threadName) {
	}

	static class RecordingNotify implements NotifyDeviceReplacedUseCase {

		final List<Call> calls = new CopyOnWriteArrayList<>();

		@Override
		public void notifyDeviceReplaced(List<Long> previousDeviceIds) {
			calls.add(new Call(previousDeviceIds, Thread.currentThread().getName()));
		}

	}

	/** 커밋 · 롤백 동기화만 일으키는 가짜 트랜잭션 관리자(DB 없음). */
	static class NoopTransactionManager extends AbstractPlatformTransactionManager {

		@Override
		protected Object doGetTransaction() {
			return new Object();
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) {
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
		}

	}

	@Configuration
	@EnableTransactionManagement
	@Import(AsyncConfig.class)
	static class TestConfig {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new NoopTransactionManager();
		}

		@Bean
		TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}

		@Bean
		RecordingNotify recordingNotify() {
			return new RecordingNotify();
		}

		@Bean
		DeviceReplacedPushListener deviceReplacedPushListener(NotifyDeviceReplacedUseCase notifyDeviceReplacedUseCase) {
			return new DeviceReplacedPushListener(notifyDeviceReplacedUseCase);
		}

	}

}
