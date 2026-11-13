package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// loadSignalLost(staleBefore): ACTIVE · etaUpdatedAt < staleBefore · 철회되지 않은 호출이 있는 세션만. 실제 DB.
// 시드는 한 세트만 만들 수 있어(학원 유니크) 같은 세션을 「돌려주는 상태」로 만들어 확인한 뒤 한 조건만 깨서 빠지는지 본다
@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class PickupSessionSignalLostPersistenceTest extends IntegrationTestSupport {

	private static final Instant STALE_BEFORE = Instant.parse("2026-11-09T09:30:00Z");
	private static final Instant STALE = STALE_BEFORE.minusSeconds(1);
	private static final Instant FRESH = STALE_BEFORE.plusSeconds(30);

	private final LoadSessionPort loadSessionPort;
	private final SaveCallPort saveCallPort;
	private final PickupSeed pickupSeed;
	private final JdbcTemplate jdbcTemplate;

	private PickupSeed.Result seeded;

	@BeforeEach
	void setUp() {
		seeded = pickupSeed.seed();
	}

	@AfterEach
	void tearDown() {
		jdbcTemplate.update("delete from pickup_call where session_id = ?", seeded.sessionId());
		pickupSeed.clear(seeded);
	}

	@Test
	@DisplayName("loadSignalLost는 etaUpdatedAt이 staleBefore보다 앞선 ACTIVE 세션 중 철회되지 않은 호출이 있는 세션을 자녀 전체와 함께 한 번씩 돌려준다")
	void returnsActiveSessionWithLiveCallAndStaleEtaWithChildren() {
		makeSignalLost();

		List<PickupSession> found = loadSessionPort.loadSignalLost(STALE_BEFORE);

		assertThat(found).extracting(PickupSession::getSessionId).containsOnlyOnce(seeded.sessionId());
		PickupSession session = found.stream()
				.filter(candidate -> candidate.getSessionId() == seeded.sessionId())
				.findFirst()
				.orElseThrow();
		assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(session.getEtaUpdatedAt()).isEqualTo(STALE);
		assertThat(session.getChildren()).extracting(SessionChild::getChildUserId)
				.containsExactlyInAnyOrder(seeded.minjunId(), seeded.seoyunId());
	}

	@Test
	@DisplayName("loadSignalLost는 etaUpdatedAt이 staleBefore보다 뒤(1분 안)인 ACTIVE 세션은 호출이 있어도 돌려주지 않는다")
	void skipsSessionWhoseEtaIsStillFresh() {
		makeSignalLost();
		assertThat(returnedSessionIds()).contains(seeded.sessionId());

		markEtaUpdatedAt(FRESH);

		assertThat(returnedSessionIds()).doesNotContain(seeded.sessionId());
	}

	@Test
	@DisplayName("loadSignalLost는 etaUpdatedAt이 staleBefore와 같은 세션은 돌려주지 않는다(앞선 것만)")
	void skipsSessionWhoseEtaEqualsStaleBefore() {
		makeSignalLost();
		assertThat(returnedSessionIds()).contains(seeded.sessionId());

		markEtaUpdatedAt(STALE_BEFORE);

		assertThat(returnedSessionIds()).doesNotContain(seeded.sessionId());
	}

	@Test
	@DisplayName("loadSignalLost는 신호가 끊겼어도 호출이 하나도 없는 ACTIVE 세션은 돌려주지 않는다")
	void skipsSessionWithoutAnyCall() {
		makeSignalLost();
		assertThat(returnedSessionIds()).contains(seeded.sessionId());

		jdbcTemplate.update("delete from pickup_call where session_id = ?", seeded.sessionId());

		assertThat(returnedSessionIds()).doesNotContain(seeded.sessionId());
	}

	@Test
	@DisplayName("loadSignalLost는 신호가 끊겼어도 호출이 모두 REVOKED인 ACTIVE 세션은 돌려주지 않는다")
	void skipsSessionWhoseCallsAreAllRevoked() {
		makeSignalLost();
		assertThat(returnedSessionIds()).contains(seeded.sessionId());

		jdbcTemplate.update("""
				update pickup_call
				set status = 'REVOKED', revoked_at = ?, revoke_reason = 'VEHICLE_DEPARTED'
				where session_id = ?
				""", STALE.atOffset(ZoneOffset.UTC), seeded.sessionId());

		assertThat(returnedSessionIds()).doesNotContain(seeded.sessionId());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"ON_HOLD", "COMPLETED", "CANCELLED"})
	@DisplayName("loadSignalLost는 신호가 끊기고 철회되지 않은 호출이 있어도 ON_HOLD · COMPLETED · CANCELLED 세션은 돌려주지 않는다")
	void skipsSessionsThatAreNotActive(SessionStatus notActive) {
		makeSignalLost();
		assertThat(returnedSessionIds()).contains(seeded.sessionId());

		jdbcTemplate.update("update pickup_session set session_status = ? where session_id = ?", notActive.name(),
				seeded.sessionId());

		assertThat(returnedSessionIds()).doesNotContain(seeded.sessionId());
	}

	private List<Long> returnedSessionIds() {
		return loadSessionPort.loadSignalLost(STALE_BEFORE).stream().map(PickupSession::getSessionId).toList();
	}

	/** ACTIVE · etaUpdatedAt이 staleBefore보다 1초 앞 · 철회되지 않은 호출 1건. */
	private void makeSignalLost() {
		markEtaUpdatedAt(STALE);
		saveCallPort.saveCall(Call.accept(seeded.sessionId(), CallTriggerType.ETA_3MIN, 0));
	}

	private void markEtaUpdatedAt(Instant etaUpdatedAt) {
		jdbcTemplate.update("update pickup_session set eta_at = ?, eta_updated_at = ? where session_id = ?",
				etaUpdatedAt.plusSeconds(40).atOffset(ZoneOffset.UTC), etaUpdatedAt.atOffset(ZoneOffset.UTC),
				seeded.sessionId());
	}

}
