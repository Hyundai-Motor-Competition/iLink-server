package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.CallProgressUseCase;
import com.example.pickupservice.pickup.application.port.in.RevokeCallCommand;
import com.example.pickupservice.pickup.application.port.in.RevokeCallUseCase;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
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
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 DB · 실제 빈으로 「철회 커밋 → 세션 잠금을 기다리던 발송 결과 반영」 순서를 두 트랜잭션으로 만든다
@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class CallProgressServiceIntegrationTest extends IntegrationTestSupport {

	private static final String CALL_SENT = SessionEventType.CALL_SENT.name();
	private static final String CALL_DELIVERED = SessionEventType.CALL_DELIVERED.name();
	private static final String REVOKE_REASON = "테스트 철회";

	private final CallProgressUseCase callProgressUseCase;
	private final RevokeCallUseCase revokeCallUseCase;
	private final SaveCallPort saveCallPort;
	private final PickupSeed pickupSeed;
	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	private PickupSeed.Result seeded;

	@BeforeEach
	void setUp() {
		seeded = pickupSeed.seed();
	}

	@AfterEach
	void tearDown() {
		jdbcTemplate.update("delete from session_event where session_id = ?", seeded.sessionId());
		jdbcTemplate.update("delete from pickup_call where session_id = ?", seeded.sessionId());
		pickupSeed.clear(seeded);
	}

	@Test
	@DisplayName("markCallSent가 첫 loadCall 뒤 세션 잠금을 기다리는 동안 철회가 커밋되면, 잠금을 얻은 뒤 DB의 최신 Call(REVOKED)을 읽어 pickup_call은 REVOKED 그대로, 자녀는 WAITING, CALL_SENT 이벤트는 없다")
	void markCallSentAfterRevokeCommittedKeepsRevoked() throws Exception {
		long callId = saveCallPort.saveCall(Call.accept(seeded.sessionId(), CallTriggerType.ETA_3MIN, 0)).getCallId();

		Throwable thrown = RevokeCommitRace.runWhileRevokeCommits(transactionTemplate, jdbcTemplate,
				revokeCallUseCase, revoke(callId),
				() -> callProgressUseCase.markCallSent(callId, clock.instant()));

		assertThat(thrown).as("늦게 온 발송 결과는 예외 없이 버린다").isNull();
		assertCallStillRevoked(callId);
		assertThat(childPhases()).as("철회로 돌아간 자녀 상태를 CALLED로 덮어쓰지 않는다").containsOnly("WAITING");
		assertThat(eventTypes()).as("철회된 호출에는 CALL_SENT를 남기지 않는다").doesNotContain(CALL_SENT);
	}

	@Test
	@DisplayName("markCallDelivered가 첫 loadCall 뒤 세션 잠금을 기다리는 동안 철회가 커밋되면, 잠금을 얻은 뒤 DB의 최신 Call(REVOKED)을 읽어 pickup_call은 REVOKED 그대로이고 CALL_DELIVERED 이벤트는 없다")
	void markCallDeliveredAfterRevokeCommittedKeepsRevoked() throws Exception {
		long callId = saveCallPort.saveCall(sentCall()).getCallId();

		Throwable thrown = RevokeCommitRace.runWhileRevokeCommits(transactionTemplate, jdbcTemplate,
				revokeCallUseCase, revoke(callId),
				() -> callProgressUseCase.markCallDelivered(callId, clock.instant()));

		assertThat(thrown).as("늦게 온 수신 확인은 예외 없이 버린다").isNull();
		assertCallStillRevoked(callId);
		assertThat(eventTypes()).as("철회된 호출에는 CALL_DELIVERED를 남기지 않는다").doesNotContain(CALL_DELIVERED);
	}

	private Call sentCall() {
		return Call.restore(null, seeded.sessionId(), null, CallTriggerType.ETA_3MIN,
				CallStatus.SENT, 0, null, clock.instant(), null, null, null,
				null);
	}

	private RevokeCallCommand revoke(long callId) {
		AuthPrincipal guardian = new AuthPrincipal(PrincipalType.USER, seeded.guardianId(), seeded.familyId(),
				Role.GUARDIAN);
		return new RevokeCallCommand(guardian, callId, PickupSeed.STATE_VERSION, REVOKE_REASON);
	}

	private void assertCallStillRevoked(long callId) {
		Map<String, Object> call = jdbcTemplate.queryForMap(
				"select status, revoked_at, revoke_reason from pickup_call where call_id = ?", callId);
		assertThat(call.get("status")).as("철회가 커밋된 Call의 상태").isEqualTo("REVOKED");
		assertThat(call.get("revoked_at")).as("revoked_at").isNotNull();
		assertThat(call.get("revoke_reason")).as("revoke_reason").isEqualTo(REVOKE_REASON);
	}

	private List<String> childPhases() {
		return jdbcTemplate.queryForList("select child_phase from session_child where session_id = ?", String.class,
				seeded.sessionId());
	}

	private List<String> eventTypes() {
		return jdbcTemplate.queryForList("select type from session_event where session_id = ?", String.class,
				seeded.sessionId());
	}

}
