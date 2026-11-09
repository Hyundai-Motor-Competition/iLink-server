package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.pickup.application.port.in.ReceiveTelemetryUseCase;
import com.example.pickupservice.pickup.application.port.in.TelemetryCommand;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

// 서비스 · 영속 어댑터 · 이벤트 발행을 실제 빈으로 연결해 「호출 생성은 한 트랜잭션, 이벤트는 커밋 뒤에만」을 본다
@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class,
		ReceiveTelemetryServiceIntegrationTest.SessionEventCaptureConfiguration.class})
class ReceiveTelemetryServiceIntegrationTest extends IntegrationTestSupport {

	/** 판정 3단계 ETA_3MIN(≤ 180초)을 넘기는 값. DIST_700M(≤ 700m)도 함께 만족한다. */
	private static final int NAV_ETA_SECONDS = 170;
	private static final int REMAINING_METERS = 650;
	private static final String CALL_TRIGGERED = SessionEventType.CALL_TRIGGERED.name();
	private static final String ETA_UPDATED = SessionEventType.ETA_UPDATED.name();

	private final ReceiveTelemetryUseCase receiveTelemetryUseCase;
	private final PickupSeed pickupSeed;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;
	private final SessionEventCapture capture;

	@MockitoSpyBean
	private SessionEventPort sessionEventPort;

	private PickupSeed.Result seeded;

	@BeforeEach
	void setUp() {
		seeded = pickupSeed.seed();
		// AUTO 가족(UserSeed) · 경로 확정(G2 · G3) 상태로 만든다. 준비 시간(G5)은 시드에서 비어 있다
		jdbcTemplate.update("""
				update pickup_session
				set spot_status = 'CONFIRMED', route_version = 1, route_spot_version = spot_version
				where session_id = ?
				""", seeded.sessionId());
		capture.clear();
	}

	@AfterEach
	void tearDown() {
		jdbcTemplate.update("""
				delete from session_event
				where session_id in (select session_id from pickup_session where vehicle_id = ?)
				""", seeded.vehicleId());
		jdbcTemplate.update("""
				delete from pickup_call
				where session_id in (select session_id from pickup_session where vehicle_id = ?)
				""", seeded.vehicleId());
		jdbcTemplate.update("""
				delete from session_child
				where session_id in (select session_id from pickup_session where vehicle_id = ?)
				""", seeded.vehicleId());
		jdbcTemplate.update("delete from pickup_session where vehicle_id = ?", seeded.vehicleId());
		pickupSeed.clear(seeded);
	}

	@Test
	@DisplayName("호출 조건을 채운 SIG-01이 오면 한 트랜잭션으로 Call(ACCEPTED · ETA_3MIN · 지금 spotVersion)과 CALL_TRIGGERED(seq 1)가 저장되고, 커밋 뒤에 CALL_TRIGGERED가 저장된 eventId · seq로 1번 나간다")
	void telemetryMeetingTriggerSavesCallAndPublishesCallTriggeredOnceAfterCommit() {
		Instant observedAt = clock.instant();

		receiveTelemetryUseCase.receive(telemetry(observedAt));

		List<Map<String, Object>> calls = jdbcTemplate.queryForList(
				"select status, trigger_type, spot_version from pickup_call where session_id = ?",
				seeded.sessionId());
		assertThat(calls).singleElement().satisfies(call -> {
			assertThat(call.get("status")).isEqualTo("ACCEPTED");
			assertThat(call.get("trigger_type")).isEqualTo("ETA_3MIN");
			assertThat(((Number) call.get("spot_version")).intValue()).isZero();
		});
		List<Map<String, Object>> events = jdbcTemplate.queryForList(
				"select event_id, seq, type from session_event where session_id = ?", seeded.sessionId());
		assertThat(events).singleElement().satisfies(event -> {
			assertThat(event.get("type")).isEqualTo(CALL_TRIGGERED);
			assertThat(((Number) event.get("seq")).longValue()).isEqualTo(1L);
		});
		long savedEventId = ((Number) events.getFirst().get("event_id")).longValue();

		assertThat(capture.afterCommit())
				.filteredOn(message -> CALL_TRIGGERED.equals(message.type()))
				.singleElement()
				.satisfies(message -> {
					assertThat(message.sessionId()).isEqualTo(seeded.sessionId());
					assertThat(message.eventId()).isEqualTo(savedEventId);
					assertThat(message.seq()).isEqualTo(1L);
				});
	}

	@Test
	@DisplayName("호출 저장 중 CALL_TRIGGERED 저장에서 예외가 나면 receive가 그 예외로 끝나고, pickup_call · session_event에 새 행이 없고 세션의 ETA 반영도 되돌려지며, 트랜잭션 안에서 발행한 이벤트(ETA_UPDATED)를 포함해 커밋 뒤에는 아무 이벤트도 나가지 않는다")
	void failureWhileSavingCallRollsBackCallEventAndSessionAndPublishesNothingAfterCommit() {
		IllegalStateException injected = new IllegalStateException("CALL_TRIGGERED 저장 실패(테스트 주입)");
		doThrow(injected).when(sessionEventPort)
				.append(anyLong(), eq(CALL_TRIGGERED), anyInt(), any(Instant.class), anyMap());

		Throwable thrown = catchThrowable(() -> receiveTelemetryUseCase.receive(telemetry(clock.instant())));

		assertThat(thrown).isSameAs(injected);
		assertThat(countRows("select count(*) from pickup_call where session_id = ?")).isZero();
		assertThat(countRows("select count(*) from session_event where session_id = ?")).isZero();
		Map<String, Object> session = jdbcTemplate.queryForMap(
				"select eta_at, remaining_meters, eta_updated_at, state_version from pickup_session where session_id = ?",
				seeded.sessionId());
		assertThat(session.get("eta_at")).isNull();
		assertThat(session.get("remaining_meters")).isNull();
		assertThat(session.get("eta_updated_at")).isNull();
		assertThat(((Number) session.get("state_version")).intValue()).isEqualTo(PickupSeed.STATE_VERSION);
		// 발행 자체는 트랜잭션 안에서 일어났지만(ETA_UPDATED) 롤백돼 커밋 뒤 전달은 하나도 없다
		assertThat(capture.published()).extracting(SessionEventMessage::type).contains(ETA_UPDATED);
		assertThat(capture.afterCommit()).isEmpty();
	}

	private TelemetryCommand telemetry(Instant observedAt) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, seeded.vehicleId(), seeded.familyId(), null);
		return new TelemetryCommand(vehicle, seeded.sessionId(), observedAt, NAV_ETA_SECONDS, REMAINING_METERS,
				null, null, null, null);
	}

	private long countRows(String sql) {
		Long count = jdbcTemplate.queryForObject(sql, Long.class, seeded.sessionId());
		return count == null ? 0 : count;
	}

	/** 발행된 SessionEventMessage를 「발행 즉시」와 「커밋 뒤」로 나눠 모은다. */
	static class SessionEventCapture {

		private final List<SessionEventMessage> published = new CopyOnWriteArrayList<>();
		private final List<SessionEventMessage> afterCommit = new CopyOnWriteArrayList<>();

		@EventListener
		public void onPublished(SessionEventMessage message) {
			published.add(message);
		}

		// fallbackExecution 없이: 트랜잭션 밖에서 발행되면 받지 않는다
		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		public void onAfterCommit(SessionEventMessage message) {
			afterCommit.add(message);
		}

		List<SessionEventMessage> published() {
			return List.copyOf(published);
		}

		List<SessionEventMessage> afterCommit() {
			return List.copyOf(afterCommit);
		}

		void clear() {
			published.clear();
			afterCommit.clear();
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class SessionEventCaptureConfiguration {

		@Bean
		SessionEventCapture sessionEventCapture() {
			return new SessionEventCapture();
		}

	}

}
