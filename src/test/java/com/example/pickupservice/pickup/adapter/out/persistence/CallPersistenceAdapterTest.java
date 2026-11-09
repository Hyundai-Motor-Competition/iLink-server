package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class CallPersistenceAdapterTest extends IntegrationTestSupport {

	private static final Instant T0 = Instant.parse("2026-11-09T07:00:00Z");
	private static final String REVOKE_REASON = "장소를 바꿔야 해요";

	private final LoadCallPort loadCallPort;
	private final SaveCallPort saveCallPort;
	private final PickupSeed pickupSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private PickupSeed.Result seeded;
	/** 같은 차량의 어제 세션. 다른 세션의 Call이 섞이지 않는지 볼 때 쓴다. */
	private long otherSessionId;

	@BeforeEach
	void seed() {
		seeded = pickupSeed.seed();
		otherSessionId = persistSession(seeded.vehicleId(), seeded.familyId(), seeded.spotId(), SessionStatus.COMPLETED);
	}

	@AfterEach
	void clear() {
		List<Long> sessionIds = List.of(seeded.sessionId(), otherSessionId);
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("delete from CallEntity c where c.sessionId in :sessionIds")
					.setParameter("sessionIds", sessionIds)
					.executeUpdate();
			entityManager.createQuery("delete from PickupSessionEntity s where s.sessionId = :sessionId")
					.setParameter("sessionId", otherSessionId)
					.executeUpdate();
		});
		pickupSeed.clear(seeded);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = CallStatus.class, names = "REVOKED", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("loadLiveCall은 그 세션의 Call 중 REVOKED가 아닌 가장 최근 Call(ACCEPTED · SENT · DELIVERED · ACKNOWLEDGED)을 모든 값과 함께 돌려주고, 더 오래된 Call과 다른 세션의 더 최근 Call은 돌려주지 않는다")
	void loadLiveCallReturnsMostRecentNotRevokedCallOfThatSession(CallStatus liveStatus) {
		long oldestId = persistCall(callOf(seeded.sessionId(), null, CallStatus.SENT, T0));
		long revokedId = persistCall(callOf(seeded.sessionId(), oldestId, CallStatus.REVOKED, T0.plusSeconds(60)));
		Call latest = Call.restore(null, seeded.sessionId(), revokedId, CallTriggerType.MANUAL, liveStatus, 2,
				T0.plusSeconds(120), sentAtOf(liveStatus, T0.plusSeconds(120)),
				deliveredAtOf(liveStatus, T0.plusSeconds(120)), acknowledgedAtOf(liveStatus, T0.plusSeconds(120)),
				null, null);
		long latestId = persistCall(latest);
		persistCall(callOf(otherSessionId, null, CallStatus.SENT, T0.plusSeconds(180)));

		Call live = loadCallPort.loadLiveCall(seeded.sessionId()).orElseThrow();

		assertThat(live.getCallId()).as("가장 최근의 REVOKED가 아닌 Call").isEqualTo(latestId);
		assertThat(live).usingRecursiveComparison()
				.as("acceptedAt은 created_at에서 읽는다")
				.ignoringFields("callId")
				.isEqualTo(latest);
	}

	@Test
	@DisplayName("loadLiveCall은 그 세션의 Call이 모두 REVOKED이거나 Call이 없으면 빈 값을 돌려준다")
	void loadLiveCallIsEmptyWhenAllCallsAreRevokedOrThereIsNoCall() {
		long firstId = persistCall(callOf(seeded.sessionId(), null, CallStatus.REVOKED, T0));
		persistCall(callOf(seeded.sessionId(), firstId, CallStatus.REVOKED, T0.plusSeconds(60)));

		assertThat(loadCallPort.loadLiveCall(seeded.sessionId())).as("모두 REVOKED인 세션").isEmpty();
		assertThat(loadCallPort.loadLiveCall(otherSessionId)).as("Call이 없는 세션").isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(CallStatus.class)
	@DisplayName("loadCall은 callId로 그 Call을 모든 값(acceptedAt은 created_at)과 함께 돌려주고, 없는 callId는 빈 값이다")
	void loadCallReturnsThatCallWithAllValuesOrEmptyWhenMissing(CallStatus status) {
		long otherCallId = persistCall(callOf(seeded.sessionId(), null, CallStatus.SENT, T0));
		Call target = callOf(seeded.sessionId(), otherCallId, status, T0.plusSeconds(60));
		long targetId = persistCall(target);

		Call loaded = loadCallPort.loadCall(targetId).orElseThrow();

		assertThat(loaded.getCallId()).isEqualTo(targetId);
		assertThat(loaded).usingRecursiveComparison()
				.as("acceptedAt은 created_at에서 읽는다")
				.ignoringFields("callId")
				.isEqualTo(target);
		assertThat(loadCallPort.loadCall(Long.MAX_VALUE)).as("없는 callId").isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(CallStatus.class)
	@DisplayName("loadLatestCall은 REVOKED를 포함해 그 세션의 가장 최근 Call을 모든 값과 함께 돌려주고, 더 오래된 Call과 다른 세션의 더 최근 Call은 돌려주지 않으며, Call이 없는 세션은 빈 값이다")
	void loadLatestCallReturnsMostRecentCallOfThatSessionIncludingRevoked(CallStatus latestStatus) {
		assertThat(loadCallPort.loadLatestCall(otherSessionId)).as("Call이 없는 세션").isEmpty();
		long olderId = persistCall(callOf(seeded.sessionId(), null, CallStatus.SENT, T0));
		Call latest = callOf(seeded.sessionId(), olderId, latestStatus, T0.plusSeconds(60));
		long latestId = persistCall(latest);
		persistCall(callOf(otherSessionId, null, CallStatus.SENT, T0.plusSeconds(120)));

		Call found = loadCallPort.loadLatestCall(seeded.sessionId()).orElseThrow();

		assertThat(found.getCallId()).as("그 세션의 가장 최근 Call").isEqualTo(latestId);
		assertThat(found).usingRecursiveComparison()
				.as("acceptedAt은 created_at에서 읽는다")
				.ignoringFields("callId")
				.isEqualTo(latest);
	}

	@Test
	@DisplayName("saveCall은 callId가 없는 Call(accept · acceptAfter)을 새 행으로 저장해 callId와 acceptedAt(created_at)을 채워 돌려주고, loadCall로 읽으면 sessionId · prevCallId · triggerType · status ACCEPTED · spotVersion · 시각 값이 같다")
	void saveCallInsertsNewCallWithGeneratedIdAndCreatedAtAsAcceptedAt() {
		Call first = Call.accept(seeded.sessionId(), CallTriggerType.ETA_3MIN, 1);

		Call savedFirst = saveCallPort.saveCall(first);

		assertThat(savedFirst.getCallId()).as("첫 Call의 callId").isNotNull();
		Call second = Call.acceptAfter(seeded.sessionId(), savedFirst.getCallId(), CallTriggerType.MANUAL, 2);

		Call savedSecond = saveCallPort.saveCall(second);

		assertThat(savedSecond.getCallId()).as("재호출 Call의 callId").isNotNull()
				.isNotEqualTo(savedFirst.getCallId());
		assertInsertedCall(savedFirst, first, "첫 Call");
		assertInsertedCall(savedSecond, second, "재호출 Call");
		assertThat(countCalls(seeded.sessionId())).as("그 세션의 pickup_call 행 수").isEqualTo(2);
	}

	@Test
	@DisplayName("saveCall은 callId가 있는 Call을 같은 행에 덮어써 status · sentAt · deliveredAt · acknowledgedAt · revokedAt · revokeReason을 바꾸고 다시 읽어도 같으며, 행 수와 acceptedAt(created_at)은 그대로다")
	void saveCallUpdatesExistingCallInPlaceAndKeepsAcceptedAt() {
		long callId = persistCall(callOf(seeded.sessionId(), null, CallStatus.ACCEPTED, T0));
		long sessionId = seeded.sessionId();
		List<Call> stages = List.of(
				Call.restore(callId, sessionId, null, CallTriggerType.ETA_3MIN, CallStatus.SENT, 1, T0,
						T0.plusSeconds(1), null, null, null, null),
				Call.restore(callId, sessionId, null, CallTriggerType.ETA_3MIN, CallStatus.DELIVERED, 1, T0,
						T0.plusSeconds(1), T0.plusSeconds(2), null, null, null),
				Call.restore(callId, sessionId, null, CallTriggerType.ETA_3MIN, CallStatus.ACKNOWLEDGED, 1, T0,
						T0.plusSeconds(1), T0.plusSeconds(2), T0.plusSeconds(30), null, null),
				Call.restore(callId, sessionId, null, CallTriggerType.ETA_3MIN, CallStatus.REVOKED, 1, T0,
						T0.plusSeconds(1), T0.plusSeconds(2), T0.plusSeconds(30), T0.plusSeconds(40), REVOKE_REASON));

		for (Call stage : stages) {
			Call saved = saveCallPort.saveCall(stage);

			assertThat(saved).usingRecursiveComparison()
					.as(stage.getStatus() + " 저장 결과(acceptedAt은 처음 created_at 그대로)")
					.isEqualTo(stage);
			assertThat(loadCallPort.loadCall(callId).orElseThrow()).usingRecursiveComparison()
					.as(stage.getStatus() + " 저장 뒤 다시 읽은 값")
					.isEqualTo(stage);
		}
		assertThat(countCalls(sessionId)).as("네 번 저장한 뒤 그 세션의 pickup_call 행 수").isEqualTo(1);
		assertThat(createdAtOf(callId)).as("created_at은 바뀌지 않는다").isEqualTo(T0);
	}

	/** 새로 저장한 Call: 입력값과 같고, acceptedAt은 DB created_at이며, loadCall로 읽어도 같다. */
	private void assertInsertedCall(Call saved, Call input, String name) {
		assertThat(saved).usingRecursiveComparison()
				.as(name + " 저장 결과는 callId · acceptedAt 말고 입력값과 같다")
				.ignoringFields("callId", "acceptedAt")
				.isEqualTo(input);
		Instant createdAt = createdAtOf(saved.getCallId());
		assertThat(saved.getAcceptedAt()).as(name + " acceptedAt은 created_at(DB는 마이크로초까지 저장)")
				.isNotNull()
				.isCloseTo(createdAt, within(1, ChronoUnit.MICROS));
		Call loaded = loadCallPort.loadCall(saved.getCallId()).orElseThrow();
		assertThat(loaded).usingRecursiveComparison()
				.as(name + " 다시 읽은 값")
				.ignoringFields("acceptedAt")
				.isEqualTo(saved);
		assertThat(loaded.getAcceptedAt()).as(name + " 다시 읽은 acceptedAt은 created_at").isEqualTo(createdAt);
	}

	private Instant createdAtOf(long callId) {
		return jdbcTemplate.queryForObject("select created_at from pickup_call where call_id = ?", Timestamp.class,
				callId).toInstant();
	}

	private int countCalls(long sessionId) {
		return jdbcTemplate.queryForObject("select count(*) from pickup_call where session_id = ?", Integer.class,
				sessionId);
	}

	/** status 단계에 맞게 시각을 채운 Call. acceptedAt은 created_at으로 저장한다. */
	private static Call callOf(long sessionId, Long prevCallId, CallStatus status, Instant acceptedAt) {
		boolean revoked = status == CallStatus.REVOKED;
		return Call.restore(null, sessionId, prevCallId, CallTriggerType.ETA_3MIN, status, 1, acceptedAt,
				revoked ? acceptedAt.plusSeconds(1) : sentAtOf(status, acceptedAt),
				deliveredAtOf(status, acceptedAt), acknowledgedAtOf(status, acceptedAt),
				revoked ? acceptedAt.plusSeconds(20) : null, revoked ? REVOKE_REASON : null);
	}

	private static Instant sentAtOf(CallStatus status, Instant acceptedAt) {
		return status == CallStatus.SENT || status == CallStatus.DELIVERED || status == CallStatus.ACKNOWLEDGED
				? acceptedAt.plusSeconds(1) : null;
	}

	private static Instant deliveredAtOf(CallStatus status, Instant acceptedAt) {
		return status == CallStatus.DELIVERED || status == CallStatus.ACKNOWLEDGED ? acceptedAt.plusSeconds(2) : null;
	}

	private static Instant acknowledgedAtOf(CallStatus status, Instant acceptedAt) {
		return status == CallStatus.ACKNOWLEDGED ? acceptedAt.plusSeconds(30) : null;
	}

	/** saveCall을 거치지 않고 행을 넣는다. created_at은 call.acceptedAt으로 맞춘다. */
	private long persistCall(Call call) {
		Long callId = transactionTemplate.execute(tx -> {
			CallEntity entity = new CallEntity(null, call.getSessionId(), call.getPrevCallId(), call.getTriggerType(),
					call.getStatus(), call.getSpotVersion(), call.getSentAt(), call.getDeliveredAt(),
					call.getAcknowledgedAt(), call.getRevokedAt(), call.getRevokeReason());
			entityManager.persist(entity);
			entityManager.flush();
			return entity.getCallId();
		});
		jdbcTemplate.update("update pickup_call set created_at = ? where call_id = ?",
				call.getAcceptedAt().atOffset(ZoneOffset.UTC), callId);
		return callId;
	}

	private long persistSession(long vehicleId, long familyId, long spotId, SessionStatus status) {
		return transactionTemplate.execute(tx -> {
			PickupSessionEntity session = new PickupSessionEntity(null, vehicleId, familyId, spotId,
					PickupSeed.SPOT_NAME, GeoPoints.toPoint(PickupSeed.STOP_LAT, PickupSeed.STOP_LNG),
					GeoPoints.toPoint(PickupSeed.MEETING_LAT, PickupSeed.MEETING_LNG), seeded.date().minusDays(1),
					status, VehiclePhase.ROUTING, SpotStatus.SELECTED, 0, 0, 0, null, false, null, null, null, null,
					null, null, null, null, null, null);
			entityManager.persist(session);
			entityManager.flush();
			return session.getSessionId();
		});
	}

}
