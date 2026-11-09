package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.AcknowledgeCallCommand;
import com.example.pickupservice.pickup.application.port.in.AcknowledgeCallUseCase;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 DB · 실제 빈으로 「철회 커밋 → 세션 잠금을 기다리던 자녀 확인」 순서를 두 트랜잭션으로 만든다
@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class AcknowledgeCallServiceIntegrationTest extends IntegrationTestSupport {

	private static final String CALL_ACKNOWLEDGED = SessionEventType.CALL_ACKNOWLEDGED.name();
	private static final String REVOKE_REASON = "테스트 철회";

	private final AcknowledgeCallUseCase acknowledgeCallUseCase;
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
		// 호출이 나가 자녀가 CALLED인 상태(stateVersion은 그대로 둔다)
		jdbcTemplate.update("update session_child set child_phase = 'CALLED' where session_id = ?", seeded.sessionId());
	}

	@AfterEach
	void tearDown() {
		jdbcTemplate.update("delete from session_event where session_id = ?", seeded.sessionId());
		jdbcTemplate.update("delete from pickup_call where session_id = ?", seeded.sessionId());
		pickupSeed.clear(seeded);
	}

	@Test
	@DisplayName("acknowledge가 첫 loadCall 뒤 세션 잠금을 기다리는 동안 철회가 커밋되면, 잠금을 얻은 뒤 DB의 최신 Call(REVOKED)을 읽어 409 CALL_ALREADY_REVOKED로 거절하고 pickup_call은 REVOKED, 자녀는 WAITING · acknowledged_at 없음 그대로다")
	void acknowledgeAfterRevokeCommittedIsRejectedAsAlreadyRevoked() throws Exception {
		long callId = saveCallPort.saveCall(Call.restore(null, seeded.sessionId(), null, CallTriggerType.ETA_3MIN,
				CallStatus.SENT, 0, null, clock.instant(), null, null, null, null)).getCallId();
		AuthPrincipal minjun = new AuthPrincipal(PrincipalType.USER, seeded.minjunId(), seeded.familyId(), Role.CHILD);

		Throwable thrown = RevokeCommitRace.runWhileRevokeCommits(transactionTemplate, jdbcTemplate,
				revokeCallUseCase, revoke(callId),
				() -> acknowledgeCallUseCase.acknowledge(new AcknowledgeCallCommand(minjun, callId)));

		assertThat(thrown).as("철회가 커밋된 호출에 대한 확인은 거절한다").isInstanceOf(BusinessException.class);
		assertThat(((BusinessException) thrown).code()).isEqualTo("CALL_ALREADY_REVOKED");
		Map<String, Object> call = jdbcTemplate.queryForMap(
				"select status, revoked_at from pickup_call where call_id = ?", callId);
		assertThat(call.get("status")).as("철회가 커밋된 Call의 상태").isEqualTo("REVOKED");
		assertThat(call.get("revoked_at")).as("revoked_at").isNotNull();
		List<Map<String, Object>> children = jdbcTemplate.queryForList(
				"select child_phase, acknowledged_at from session_child where session_id = ?", seeded.sessionId());
		assertThat(children).as("철회로 돌아간 자녀 상태를 MEETING으로 덮어쓰지 않는다").allSatisfy(child -> {
			assertThat(child.get("child_phase")).isEqualTo("WAITING");
			assertThat(child.get("acknowledged_at")).isNull();
		});
		assertThat(jdbcTemplate.queryForList("select type from session_event where session_id = ?", String.class,
				seeded.sessionId())).doesNotContain(CALL_ACKNOWLEDGED);
	}

	private RevokeCallCommand revoke(long callId) {
		AuthPrincipal guardian = new AuthPrincipal(PrincipalType.USER, seeded.guardianId(), seeded.familyId(),
				Role.GUARDIAN);
		return new RevokeCallCommand(guardian, callId, PickupSeed.STATE_VERSION, REVOKE_REASON);
	}

}
