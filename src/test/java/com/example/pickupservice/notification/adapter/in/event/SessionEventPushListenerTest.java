package com.example.pickupservice.notification.adapter.in.event;

import com.example.pickupservice.common.async.AsyncConfig;
import com.example.pickupservice.notification.application.port.in.DispatchSessionEventUseCase;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

// 세션 이벤트를 받아 DispatchSessionEventUseCase를 부르는 시점(커밋 뒤 · 롤백 땐 안 · 다른 스레드)을 본다.
// DB 없이 가벼운 컨텍스트(트랜잭션 동기화 + AsyncConfig + 대역 DispatchSessionEventUseCase)로 확인한다.
@SpringJUnitConfig(SessionEventPushListenerTest.TestConfig.class)
@TestPropertySource(properties = {
		"pickup.async.event.core-pool-size=2",
		"pickup.async.event.max-pool-size=4",
		"pickup.async.event.queue-capacity=100"
})
class SessionEventPushListenerTest {

	private static final long SESSION_ID = 9_200_001L;
	private static final Duration WAIT_LIMIT = Duration.ofSeconds(5);
	private static final long POLL_MILLIS = 20L;
	private static final long SETTLE_MILLIS = 200L;
	private static final Instant OCCURRED_AT = Instant.parse("2026-11-05T09:09:02Z");
	private static final String CALL_SENT = "CALL_SENT";

	@Autowired
	private ApplicationEventPublisher eventPublisher;
	@Autowired
	private TransactionTemplate transactionTemplate;
	@Autowired
	private RecordingDispatch recordingDispatch;

	@BeforeEach
	void clearCalls() {
		recordingDispatch.calls.clear();
	}

	@Test
	@DisplayName("발송 리스너는 트랜잭션이 커밋된 뒤에만 FCM을 부른다")
	void dispatchIsCalledOnlyAfterCommit() {
		transactionTemplate.executeWithoutResult(status -> {
			eventPublisher.publishEvent(callSent(9001L, 42L));
			sleep(SETTLE_MILLIS);
			assertThat(recordingDispatch.calls).as("커밋 전에는 dispatch를 부르지 않는다").isEmpty();
		});

		awaitCalls(1);

		assertThat(recordingDispatch.calls).hasSize(1);
		assertThat(recordingDispatch.calls.getFirst().message())
				.isEqualTo(toNotificationMessage(callSent(9001L, 42L)));
	}

	@Test
	@DisplayName("발송 리스너는 트랜잭션이 롤백되면 FCM을 부르지 않는다")
	void dispatchIsNotCalledWhenTransactionRollsBack() {
		transactionTemplate.executeWithoutResult(status -> {
			eventPublisher.publishEvent(callSent(9000L, 41L));
			status.setRollbackOnly();
		});
		transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(callSent(9001L, 42L)));

		awaitCalls(1);
		sleep(SETTLE_MILLIS);

		assertThat(recordingDispatch.calls).extracting(call -> call.message().eventId()).containsExactly(9001L);
	}

	@Test
	@DisplayName("발송 리스너는 요청 스레드와 다른 스레드에서 실행된다")
	void dispatchRunsOnThreadDifferentFromRequestThread() {
		String requestThread = Thread.currentThread().getName();

		transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(callSent(9001L, 42L)));

		awaitCalls(1);

		String dispatchThread = recordingDispatch.calls.getFirst().threadName();
		assertThat(dispatchThread).isNotEqualTo(requestThread);
		assertThat(dispatchThread).startsWith("event-");
	}

	private static SessionEventMessage callSent(long eventId, long seq) {
		return new SessionEventMessage(SESSION_ID, eventId, seq, CALL_SENT, 16, OCCURRED_AT,
				Map.of("callId", 3301, "trigger", "ETA_3MIN"), List.of(101L));
	}

	private static com.example.pickupservice.notification.application.port.in.SessionEventMessage toNotificationMessage(
			SessionEventMessage m) {
		return new com.example.pickupservice.notification.application.port.in.SessionEventMessage(m.sessionId(),
				m.eventId(), m.seq(), m.type(), m.stateVersion(), m.occurredAt(), m.payload(), m.receiverUserIds());
	}

	private void awaitCalls(int count) {
		long deadline = System.nanoTime() + WAIT_LIMIT.toNanos();
		while (recordingDispatch.calls.size() < count && System.nanoTime() < deadline) {
			sleep(POLL_MILLIS);
		}
		assertThat(recordingDispatch.calls).as("제한 시간 안에 dispatch가 불리지 않았다").hasSizeGreaterThanOrEqualTo(count);
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	record Call(com.example.pickupservice.notification.application.port.in.SessionEventMessage message,
				String threadName) {
	}

	static class RecordingDispatch implements DispatchSessionEventUseCase {

		final List<Call> calls = new CopyOnWriteArrayList<>();

		@Override
		public void dispatch(com.example.pickupservice.notification.application.port.in.SessionEventMessage message) {
			calls.add(new Call(message, Thread.currentThread().getName()));
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
		RecordingDispatch recordingDispatch() {
			return new RecordingDispatch();
		}

		@Bean
		SessionEventPushListener sessionEventPushListener(DispatchSessionEventUseCase dispatchSessionEventUseCase) {
			return new SessionEventPushListener(dispatchSessionEventUseCase);
		}

	}

}
