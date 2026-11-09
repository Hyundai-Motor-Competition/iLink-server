package com.example.pickupservice.pickup.adapter.in.sse;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.domain.SessionEventType;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitterProbe;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

// 서비스가 발행한 SessionEventMessage가 「커밋 뒤에만」 그 세션 차량의 SSE 연결(registry에 등록한 emitter)로 나가는지 본다
@RequiredArgsConstructor
class SessionEventStreamListenerTest extends IntegrationTestSupport {

	private static final long SESSION_ID = 9_100_001L;
	private static final long OTHER_SESSION_ID = 9_100_002L;
	private static final Duration WAIT_LIMIT = Duration.ofSeconds(5);
	private static final long POLL_MILLIS = 20L;
	private static final long SETTLE_MILLIS = 200L;
	private static final Instant OCCURRED_AT = Instant.parse("2026-11-05T09:09:02Z");
	private static final String CALL_SENT = SessionEventType.CALL_SENT.name();
	private static final String CALL_SENT_DATA = """
			{"eventId":9001,"seq":42,"stateVersion":16,"occurredAt":"2026-11-05T18:09:02+09:00",
			 "payload":{"callId":3301,"trigger":"ETA_3MIN"}}
			""";

	private final PublishSessionEventPort publishSessionEventPort;
	private final SessionStreamRegistry sessionStreamRegistry;
	private final TransactionTemplate transactionTemplate;

	private SseEmitterProbe vehicleStream;
	private SseEmitterProbe otherVehicleStream;

	@BeforeEach
	void connectVehicles() {
		vehicleStream = SseEmitterProbe.attach(sessionStreamRegistry.register(SESSION_ID));
		otherVehicleStream = SseEmitterProbe.attach(sessionStreamRegistry.register(OTHER_SESSION_ID));
	}

	@AfterEach
	void closeStreams() {
		sessionStreamRegistry.complete(SESSION_ID);
		sessionStreamRegistry.complete(OTHER_SESSION_ID);
	}

	@Test
	@DisplayName("트랜잭션 안에서 발행한 SessionEventMessage는 커밋 전에는 나가지 않고, 커밋 뒤에 그 세션 연결로 event · id(seq) · data(eventId · seq · stateVersion · occurredAt · payload) 형식 1건이 나간다")
	void committedEventIsSentToSessionStreamAfterCommit() throws Exception {
		transactionTemplate.executeWithoutResult(status -> {
			publishSessionEventPort.publish(callSent(SESSION_ID, 9001L, 42L));
			sleep(SETTLE_MILLIS);
			assertThat(events(vehicleStream)).as("커밋 전에는 아무것도 나가지 않는다").isEmpty();
		});

		List<Map<String, String>> events = awaitEvents(vehicleStream, 1);

		assertThat(events).hasSize(1);
		assertThat(events.getFirst()).containsEntry("event", CALL_SENT).containsEntry("id", "42");
		JSONAssert.assertEquals(CALL_SENT_DATA, events.getFirst().get("data"), true);
	}

	@Test
	@DisplayName("트랜잭션이 롤백되면 발행한 SessionEventMessage는 연결로 나가지 않고, 그 뒤 커밋된 이벤트만 나간다")
	void rolledBackEventIsNotSentAndOnlyLaterCommittedEventIsSent() {
		transactionTemplate.executeWithoutResult(status -> {
			publishSessionEventPort.publish(callSent(SESSION_ID, 9000L, 41L));
			status.setRollbackOnly();
		});
		transactionTemplate.executeWithoutResult(
				status -> publishSessionEventPort.publish(callSent(SESSION_ID, 9001L, 42L)));

		List<Map<String, String>> events = awaitEvents(vehicleStream, 1);
		sleep(SETTLE_MILLIS);

		assertThat(events(vehicleStream)).extracting(event -> event.get("id")).containsExactly("42");
	}

	@Test
	@DisplayName("세션 A의 이벤트는 세션 A의 연결로만 나가고 다른 세션 B의 연결로는 나가지 않는다")
	void eventIsSentOnlyToStreamOfItsOwnSession() {
		transactionTemplate.executeWithoutResult(status -> {
			publishSessionEventPort.publish(callSent(SESSION_ID, 9001L, 42L));
			publishSessionEventPort.publish(callSent(OTHER_SESSION_ID, 9500L, 7L));
		});

		awaitEvents(vehicleStream, 1);
		awaitEvents(otherVehicleStream, 1);
		sleep(SETTLE_MILLIS);

		assertThat(events(vehicleStream)).extracting(event -> event.get("id")).containsExactly("42");
		assertThat(events(otherVehicleStream)).extracting(event -> event.get("id")).containsExactly("7");
	}

	@Test
	@DisplayName("저장하지 않는 이벤트(eventId · seq 없음, ETA_UPDATED)도 커밋 뒤에 그 세션 연결로 이름과 payload를 담아 나가고, id 줄은 없다")
	void eventWithoutEventIdAndSeqIsSentWithoutIdLine() {
		SessionEventMessage etaUpdated = new SessionEventMessage(SESSION_ID, null, null,
				SessionEventType.ETA_UPDATED.name(), 16, OCCURRED_AT, Map.of("etaSeconds", 170), List.of());

		transactionTemplate.executeWithoutResult(status -> publishSessionEventPort.publish(etaUpdated));

		List<Map<String, String>> events = awaitEvents(vehicleStream, 1);

		assertThat(events).hasSize(1);
		assertThat(events.getFirst()).containsEntry("event", SessionEventType.ETA_UPDATED.name());
		assertThat(events.getFirst()).doesNotContainKey("id");
		assertThat(events.getFirst().get("data")).contains("\"etaSeconds\":170").contains("\"stateVersion\":16");
	}

	private static SessionEventMessage callSent(long sessionId, long eventId, long seq) {
		return new SessionEventMessage(sessionId, eventId, seq, CALL_SENT, 16, OCCURRED_AT,
				Map.of("callId", 3301, "trigger", "ETA_3MIN"), List.of(101L));
	}

	private static List<Map<String, String>> awaitEvents(SseEmitterProbe stream, int count) {
		return awaitUntil(() -> events(stream), events -> events.size() >= count);
	}

	private static <T> T awaitUntil(Supplier<T> supplier, java.util.function.Predicate<T> done) {
		long deadline = System.nanoTime() + WAIT_LIMIT.toNanos();
		T value = supplier.get();
		while (!done.test(value) && System.nanoTime() < deadline) {
			sleep(POLL_MILLIS);
			value = supplier.get();
		}
		assertThat(done.test(value)).as("제한 시간 안에 이벤트가 나가지 않았다: %s", value).isTrue();
		return value;
	}

	/** 한 칸(send 한 번)을 필드 이름(event · id · data) 기준으로 읽는다. 주석(`:`)만 있는 칸(ping)은 뺀다. */
	private static List<Map<String, String>> events(SseEmitterProbe stream) {
		List<Map<String, String>> events = new ArrayList<>();
		for (String block : stream.blocks()) {
			Map<String, String> fields = new LinkedHashMap<>();
			for (String line : block.split("\\R")) {
				int colon = line.indexOf(':');
				if (colon <= 0) {
					continue;
				}
				fields.put(line.substring(0, colon), line.substring(colon + 1).strip());
			}
			if (!fields.isEmpty()) {
				events.add(fields);
			}
		}
		return events;
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

}
