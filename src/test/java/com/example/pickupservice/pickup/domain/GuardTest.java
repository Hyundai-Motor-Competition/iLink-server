package com.example.pickupservice.pickup.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuardTest {

	private static final Instant NOW = Instant.parse("2026-11-09T09:10:00Z");
	private static final Duration ETA_VALID_FOR = Duration.ofSeconds(60);
	private static final Guard GUARD = new Guard(ETA_VALID_FOR);

	private static final long SESSION_ID = 1L;
	private static final long VEHICLE_ID = 3L;
	private static final long FAMILY_ID = 7L;
	private static final long SPOT_ID = 11L;
	private static final String SPOT_NAME = "정문 앞 공영주차장";
	private static final LatLng STOP_POINT = new LatLng(37.5665, 126.9780);
	private static final LatLng MEETING_POINT = new LatLng(37.5667, 126.9783);
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);
	private static final int STATE_VERSION = 5;
	private static final int SPOT_VERSION = 2;
	private static final int ROUTE_VERSION = 1;
	private static final int REMAINING_METERS = 650;
	private static final long NAV_ETA_SECONDS = 170;

	private static final Instant FRESH_ETA_UPDATED_AT = NOW.minusSeconds(15);
	private static final Instant STALE_ETA_UPDATED_AT = NOW.minusSeconds(61);
	private static final Instant PASSED_CALL_NOT_BEFORE = NOW.minus(Duration.ofMinutes(2));

	@Test
	@DisplayName("Guard는 ON_HOLD 세션에서 G1을 unmet에 넣는다")
	void evaluatePutsG1IntoUnmetWhenSessionIsOnHold() {
		PickupSession session = session(SessionStatus.ON_HOLD, SpotStatus.CONFIRMED, SPOT_VERSION,
				FRESH_ETA_UPDATED_AT);

		GuardResult result = GUARD.evaluate(session, NOW);

		assertThat(result.met()).isFalse();
		assertThat(result.unmet()).containsExactly(GuardId.G1);
	}

	@ParameterizedTest(name = "spotStatus {0}")
	@EnumSource(value = SpotStatus.class, names = "CONFIRMED", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("Guard는 spotStatus가 CONFIRMED가 아니면 G2를 unmet에 넣는다")
	void evaluatePutsG2IntoUnmetWhenSpotStatusIsNotConfirmed(SpotStatus spotStatus) {
		PickupSession session = session(SessionStatus.ACTIVE, spotStatus, SPOT_VERSION, FRESH_ETA_UPDATED_AT);

		GuardResult result = GUARD.evaluate(session, NOW);

		assertThat(result.met()).isFalse();
		assertThat(result.unmet()).containsExactly(GuardId.G2);
	}

	@Test
	@DisplayName("Guard는 확정 경로의 장소 버전(routeSpotVersion)이 지금 spotVersion과 다르면 G3을 unmet에 넣는다")
	void evaluatePutsG3IntoUnmetWhenRouteSpotVersionDiffersFromSpotVersion() {
		PickupSession session = session(SessionStatus.ACTIVE, SpotStatus.CONFIRMED, SPOT_VERSION - 1,
				FRESH_ETA_UPDATED_AT);

		GuardResult result = GUARD.evaluate(session, NOW);

		assertThat(result.met()).isFalse();
		assertThat(result.unmet()).containsExactly(GuardId.G3);
	}

	@Test
	@DisplayName("Guard는 확정 경로의 장소 버전(routeSpotVersion)이 비어 있으면 G3을 unmet에 넣는다")
	void evaluatePutsG3IntoUnmetWhenRouteSpotVersionIsNull() {
		PickupSession session = session(SessionStatus.ACTIVE, SpotStatus.CONFIRMED, null, FRESH_ETA_UPDATED_AT);

		GuardResult result = GUARD.evaluate(session, NOW);

		assertThat(result.met()).isFalse();
		assertThat(result.unmet()).containsExactly(GuardId.G3);
	}

	@Test
	@DisplayName("Guard는 ETA가 stale이면(etaUpdatedAt이 61초 전) G4를 unmet에 넣는다")
	void evaluatePutsG4IntoUnmetWhenEtaIsStale() {
		PickupSession session = session(SessionStatus.ACTIVE, SpotStatus.CONFIRMED, SPOT_VERSION,
				STALE_ETA_UPDATED_AT);

		GuardResult result = GUARD.evaluate(session, NOW);

		assertThat(result.met()).isFalse();
		assertThat(result.unmet()).containsExactly(GuardId.G4);
	}

	@Test
	@DisplayName("Guard는 ETA를 한 번도 받지 않아 etaUpdatedAt이 없으면 G4를 unmet에 넣는다")
	void evaluatePutsG4IntoUnmetWhenEtaWasNeverReceived() {
		PickupSession session = session(SessionStatus.ACTIVE, SpotStatus.CONFIRMED, SPOT_VERSION, null);

		GuardResult result = GUARD.evaluate(session, NOW);

		assertThat(result.met()).isFalse();
		assertThat(result.unmet()).containsExactly(GuardId.G4);
	}

	@Test
	@DisplayName("Guard는 생성할 때 받은 ETA 유효시간으로 G4를 판정한다(45초 전 ETA는 30초 기준이면 stale, 60초 기준이면 유효)")
	void evaluateJudgesG4WithEtaValidForGivenAtConstruction() {
		PickupSession session = session(SessionStatus.ACTIVE, SpotStatus.CONFIRMED, SPOT_VERSION,
				NOW.minusSeconds(45));

		GuardResult stricter = new Guard(Duration.ofSeconds(30)).evaluate(session, NOW);
		GuardResult standard = new Guard(Duration.ofSeconds(60)).evaluate(session, NOW);

		assertThat(stricter.unmet()).containsExactly(GuardId.G4);
		assertThat(standard.unmet()).doesNotContain(GuardId.G4);
	}

	@Test
	@DisplayName("Guard는 세션 자녀 둘 중 한 명이라도 callNotBefore가 지나지 않았으면(3분 뒤) G5를 unmet에 넣는다")
	void evaluatePutsG5IntoUnmetWhenOneSiblingCallNotBeforeHasNotPassed() {
		SessionChild withoutExtension = waitingChild(41L, 21L, 31L, 0, null);
		SessionChild extensionNotPassed = waitingChild(42L, 22L, 32L, 5, NOW.plus(Duration.ofMinutes(3)));
		PickupSession session = session(List.of(withoutExtension, extensionNotPassed));

		GuardResult result = GUARD.evaluate(session, NOW);

		assertThat(result.met()).isFalse();
		assertThat(result.unmet()).containsExactly(GuardId.G5);
	}

	@Test
	@DisplayName("Guard는 callNotBefore가 지금과 같은 시각이면 지난 것으로 보고 G5를 unmet에 넣지 않는다")
	void evaluateTreatsCallNotBeforeEqualToNowAsPassed() {
		SessionChild withoutExtension = waitingChild(41L, 21L, 31L, 0, null);
		SessionChild extensionEndsNow = waitingChild(42L, 22L, 32L, 5, NOW);
		PickupSession session = session(List.of(withoutExtension, extensionEndsNow));

		GuardResult result = GUARD.evaluate(session, NOW);

		assertThat(result.met()).isTrue();
		assertThat(result.unmet()).isEmpty();
	}

	@Test
	@DisplayName("Guard는 G1~G5 다섯 조건을 모두 만족하면 met true와 빈 unmet을 돌려준다")
	void evaluateReturnsMetWithEmptyUnmetWhenAllFiveGuardsPass() {
		PickupSession session = session(SessionStatus.ACTIVE, SpotStatus.CONFIRMED, SPOT_VERSION,
				FRESH_ETA_UPDATED_AT);

		GuardResult result = GUARD.evaluate(session, NOW);

		assertThat(result.met()).isTrue();
		assertThat(result.unmet()).isEmpty();
	}

	private static PickupSession session(SessionStatus sessionStatus, SpotStatus spotStatus,
			Integer routeSpotVersion, Instant etaUpdatedAt) {
		return session(sessionStatus, spotStatus, routeSpotVersion, etaUpdatedAt, readyChildren());
	}

	private static PickupSession session(List<SessionChild> children) {
		return session(SessionStatus.ACTIVE, SpotStatus.CONFIRMED, SPOT_VERSION, FRESH_ETA_UPDATED_AT, children);
	}

	private static PickupSession session(SessionStatus sessionStatus, SpotStatus spotStatus,
			Integer routeSpotVersion, Instant etaUpdatedAt, List<SessionChild> children) {
		Instant etaAt = etaUpdatedAt == null ? null : etaUpdatedAt.plusSeconds(NAV_ETA_SECONDS);
		Integer remainingMeters = etaUpdatedAt == null ? null : REMAINING_METERS;
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT,
				MEETING_POINT, TODAY, sessionStatus, VehiclePhase.APPROACHING, spotStatus, STATE_VERSION, SPOT_VERSION,
				ROUTE_VERSION, routeSpotVersion, false, etaAt, remainingMeters, etaUpdatedAt, null, null, null, null,
				null, null, null, null, children);
	}

	private static List<SessionChild> readyChildren() {
		SessionChild withoutExtension = waitingChild(41L, 21L, 31L, 0, null);
		SessionChild extensionPassed = waitingChild(42L, 22L, 32L, 5, PASSED_CALL_NOT_BEFORE);
		return List.of(withoutExtension, extensionPassed);
	}

	private static SessionChild waitingChild(long sessionChildId, long childUserId, long scheduleId,
			int extraPrepMinutes, Instant callNotBefore) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, ChildPhase.WAITING, extraPrepMinutes,
				callNotBefore, null, null, null, null);
	}
}
