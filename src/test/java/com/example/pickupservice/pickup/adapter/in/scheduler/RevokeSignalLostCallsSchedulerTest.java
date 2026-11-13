package com.example.pickupservice.pickup.adapter.in.scheduler;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 DB로 「신호가 1분 넘게 끊기고 철회되지 않은 호출이 있는 ACTIVE 세션만 SIGNAL_LOST로 철회」를 본다. 스케줄러 메서드를 직접 불러 15초를 기다리지 않는다
@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class RevokeSignalLostCallsSchedulerTest extends IntegrationTestSupport {

	private static final String INTERVAL_PROPERTY = "pickup.call.signal-lost-check-interval";

	private final RevokeSignalLostCallsScheduler scheduler;
	private final SaveCallPort saveCallPort;
	private final PickupSeed pickupSeed;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;
	private final Environment environment;

	private PickupSeed.Result seeded;

	@BeforeEach
	void setUp() {
		seeded = pickupSeed.seed();
		jdbcTemplate.update("""
				update session_child
				set child_phase = 'CALLED', last_lat = 37.501350, last_lng = 127.039900, last_located_at = ?
				where session_id = ?
				""", Timestamp.from(clock.instant().minusSeconds(70)), seeded.sessionId());
	}

	@AfterEach
	void tearDown() {
		for (String table : List.of("session_event", "pickup_call")) {
			jdbcTemplate.update("delete from " + table + " where session_id = ?", seeded.sessionId());
		}
		pickupSeed.clear(seeded);
	}

	@Test
	@DisplayName("신호가 1분 넘게 끊기고 철회되지 않은 호출이 있는 ACTIVE 세션은 스케줄러 실행 뒤 호출이 REVOKED(revokeReason SIGNAL_LOST) · 자녀 WAITING · 위치 비움이 되고 CALL_REVOKED가 저장된다")
	void staleSessionWithLiveCallIsRevokedAsSignalLost() {
		markEtaUpdatedAt(clock.instant().minusSeconds(61));
		long callId = saveCallPort.saveCall(Call.accept(seeded.sessionId(), CallTriggerType.ETA_3MIN, 0)).getCallId();

		scheduler.revokeSignalLostCalls();

		Map<String, Object> call = jdbcTemplate.queryForMap(
				"select status, revoked_at, revoke_reason from pickup_call where call_id = ?", callId);
		assertThat(call.get("status")).isEqualTo("REVOKED");
		assertThat(call.get("revoked_at")).isNotNull();
		assertThat(call.get("revoke_reason")).isEqualTo("SIGNAL_LOST");
		List<Map<String, Object>> children = jdbcTemplate.queryForList(
				"select child_phase, last_lat, last_lng, last_located_at from session_child where session_id = ?",
				seeded.sessionId());
		assertThat(children).hasSize(2).allSatisfy(child -> {
			assertThat(child.get("child_phase")).isEqualTo("WAITING");
			assertThat(child.get("last_lat")).isNull();
			assertThat(child.get("last_lng")).isNull();
			assertThat(child.get("last_located_at")).isNull();
		});
		assertThat(sessionStatus()).isEqualTo("ACTIVE");
		assertThat(callRevokedEventCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("etaUpdatedAt이 1분 안인 ACTIVE 세션은 호출이 있어도 스케줄러 실행 뒤 호출이 그대로이고 자녀는 CALLED 그대로이며 CALL_REVOKED가 없다")
	void sessionWithFreshEtaIsNotRevoked() {
		markEtaUpdatedAt(clock.instant().minusSeconds(30));
		long callId = saveCallPort.saveCall(Call.accept(seeded.sessionId(), CallTriggerType.ETA_3MIN, 0)).getCallId();

		scheduler.revokeSignalLostCalls();

		assertThat(callStatus(callId)).isEqualTo("ACCEPTED");
		assertThat(childPhases()).containsOnly("CALLED");
		assertThat(callRevokedEventCount()).isZero();
	}

	@Test
	@DisplayName("신호가 끊겼어도 ON_HOLD 세션은 스케줄러 실행 뒤 호출이 그대로이고 CALL_REVOKED가 없다")
	void onHoldSessionIsNotRevoked() {
		markEtaUpdatedAt(clock.instant().minusSeconds(61));
		jdbcTemplate.update("update pickup_session set session_status = 'ON_HOLD' where session_id = ?",
				seeded.sessionId());
		long callId = saveCallPort.saveCall(Call.accept(seeded.sessionId(), CallTriggerType.ETA_3MIN, 0)).getCallId();

		scheduler.revokeSignalLostCalls();

		assertThat(callStatus(callId)).isEqualTo("ACCEPTED");
		assertThat(childPhases()).containsOnly("CALLED");
		assertThat(callRevokedEventCount()).isZero();
	}

	@Test
	@DisplayName("신호가 끊겼어도 철회되지 않은 호출이 없는 ACTIVE 세션은 스케줄러 실행 뒤 CALL_REVOKED가 없고 이미 REVOKED인 호출의 revokeReason은 그대로이다")
	void sessionWithoutLiveCallIsNotRevokedAgain() {
		markEtaUpdatedAt(clock.instant().minusSeconds(61));
		long callId = saveCallPort.saveCall(Call.restore(null, seeded.sessionId(), null, CallTriggerType.ETA_3MIN,
				CallStatus.REVOKED, 0, null, null, null, null, clock.instant().minusSeconds(90), "STOP_FAILED"))
				.getCallId();

		scheduler.revokeSignalLostCalls();

		assertThat(callStatus(callId)).isEqualTo("REVOKED");
		assertThat(jdbcTemplate.queryForObject("select revoke_reason from pickup_call where call_id = ?", String.class,
				callId)).isEqualTo("STOP_FAILED");
		assertThat(callRevokedEventCount()).isZero();
	}

	@Test
	@DisplayName("revokeSignalLostCalls에는 @Scheduled(fixedDelayString = ${pickup.call.signal-lost-check-interval})가 붙어 있고 그 설정값은 15초이다")
	void revokeSignalLostCallsIsScheduledEveryFifteenSecondsFromConfiguration() throws NoSuchMethodException {
		Method method = ClassUtils.getUserClass(scheduler).getDeclaredMethod("revokeSignalLostCalls");

		Scheduled scheduled = method.getAnnotation(Scheduled.class);

		assertThat(scheduled).isNotNull();
		assertThat(scheduled.fixedDelayString()).isEqualTo("${" + INTERVAL_PROPERTY + "}");
		assertThat(environment.getProperty(INTERVAL_PROPERTY, Duration.class)).isEqualTo(Duration.ofSeconds(15));
	}

	private void markEtaUpdatedAt(Instant etaUpdatedAt) {
		jdbcTemplate.update("update pickup_session set eta_at = ?, eta_updated_at = ? where session_id = ?",
				Timestamp.from(etaUpdatedAt.plusSeconds(40)), Timestamp.from(etaUpdatedAt), seeded.sessionId());
	}

	private String sessionStatus() {
		return jdbcTemplate.queryForObject("select session_status from pickup_session where session_id = ?",
				String.class, seeded.sessionId());
	}

	private String callStatus(long callId) {
		return jdbcTemplate.queryForObject("select status from pickup_call where call_id = ?", String.class, callId);
	}

	private List<String> childPhases() {
		return jdbcTemplate.queryForList("select child_phase from session_child where session_id = ?", String.class,
				seeded.sessionId());
	}

	private long callRevokedEventCount() {
		Long count = jdbcTemplate.queryForObject(
				"select count(*) from session_event where session_id = ? and type = 'CALL_REVOKED'", Long.class,
				seeded.sessionId());
		return count == null ? 0 : count;
	}

}
