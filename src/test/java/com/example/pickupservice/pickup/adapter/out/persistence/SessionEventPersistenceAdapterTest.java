package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class SessionEventPersistenceAdapterTest extends IntegrationTestSupport {

	private static final String UNIQUE_SEQ_PER_SESSION = "uk_session_event_session_seq";
	private static final Instant T0 = Instant.parse("2026-11-09T07:00:00Z");
	private static final long CALL_ID = 3301L;
	/** loadAfter 시드: 시드 세션의 seq 1~5 행을 이 순서로 넣는다(eventId 순서와 seq 순서가 다르다). */
	private static final List<Long> SHUFFLED_SEQS = List.of(3L, 1L, 5L, 2L, 4L);
	/** loadAfter 시드: 다른 세션에는 seq 1~7을 넣어 시드 세션보다 seq가 더 크게 남는다. */
	private static final int OTHER_SESSION_EVENT_COUNT = 7;

	private final SessionEventPort sessionEventPort;
	private final PickupSeed pickupSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private PickupSeed.Result seeded;

	@BeforeEach
	void seed() {
		seeded = pickupSeed.seed();
	}

	@AfterEach
	void clear() {
		jdbcTemplate.update("""
				delete from session_event
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
	@DisplayName("append는 세션마다 seq를 1부터 1씩 늘려 저장한다: 한 트랜잭션에서 두 번 부르면 1 · 2, 다음 트랜잭션에서 3이고 사이에 append한 다른 세션은 따로 1 · 2다. 돌려준 SessionEvent와 session_event 행의 eventId · type · stateVersion · occurredAt · payload(JSON 객체)가 같다")
	void appendNumbersSeqFromOneByOnePerSession() {
		long otherSessionId = persistOtherSession();

		List<SessionEvent> firstTx = transactionTemplate.execute(tx -> List.of(
				sessionEventPort.append(seeded.sessionId(), SessionEventType.SPOT_CONFIRMED.name(), 1, T0,
						Map.of("spotId", seeded.spotId())),
				sessionEventPort.append(seeded.sessionId(), SessionEventType.DEPARTURE_NOTICE_SENT.name(), 1,
						T0.plusSeconds(1), Map.of("spotId", seeded.spotId()))));
		SessionEvent otherFirst = transactionTemplate.execute(tx -> sessionEventPort.append(otherSessionId,
				SessionEventType.SESSION_STARTED.name(), 0, T0.plusSeconds(5), Map.of("childUserId", seeded.minjunId())));
		SessionEvent third = transactionTemplate.execute(tx -> sessionEventPort.append(seeded.sessionId(),
				SessionEventType.CALL_TRIGGERED.name(), 2, T0.plusSeconds(10),
				Map.of("callId", CALL_ID, "trigger", "ETA_3MIN")));
		SessionEvent otherSecond = transactionTemplate.execute(tx -> sessionEventPort.append(otherSessionId,
				SessionEventType.VEHICLE_STOPPED.name(), 1, T0.plusSeconds(15), Map.of("childUserId", seeded.minjunId())));

		List<SessionEvent> seededSessionEvents = List.of(firstTx.get(0), firstTx.get(1), third);
		assertThat(seededSessionEvents)
				.as("시드 세션: 같은 트랜잭션 두 번 · 다음 트랜잭션 한 번")
				.extracting(SessionEvent::getSessionId, SessionEvent::getSeq, SessionEvent::getType,
						SessionEvent::getStateVersion, SessionEvent::getOccurredAt)
				.containsExactly(
						tuple(seeded.sessionId(), 1L, SessionEventType.SPOT_CONFIRMED, 1, T0),
						tuple(seeded.sessionId(), 2L, SessionEventType.DEPARTURE_NOTICE_SENT, 1, T0.plusSeconds(1)),
						tuple(seeded.sessionId(), 3L, SessionEventType.CALL_TRIGGERED, 2, T0.plusSeconds(10)));
		assertThat(List.of(otherFirst, otherSecond))
				.as("다른 세션은 시드 세션과 따로 1부터")
				.extracting(SessionEvent::getSessionId, SessionEvent::getSeq, SessionEvent::getType)
				.containsExactly(
						tuple(otherSessionId, 1L, SessionEventType.SESSION_STARTED),
						tuple(otherSessionId, 2L, SessionEventType.VEHICLE_STOPPED));
		assertThat(longValue(third.getPayload(), "callId")).as("돌려준 payload callId").isEqualTo(CALL_ID);
		assertThat(third.getPayload()).as("돌려준 payload trigger").containsEntry("trigger", "ETA_3MIN");

		List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
				select event_id, session_id, seq, type, state_version,
				       cast(extract(epoch from occurred_at) as bigint) as occurred_epoch,
				       jsonb_typeof(payload) as payload_kind
				from session_event
				where session_id in (?, ?)
				""", seeded.sessionId(), otherSessionId);
		assertThat(rows).as("session_event 행")
				.extracting(row -> longOf(row, "event_id"), row -> longOf(row, "session_id"), row -> longOf(row, "seq"),
						row -> row.get("type"), row -> longOf(row, "state_version"), row -> longOf(row, "occurred_epoch"),
						row -> row.get("payload_kind"))
				.containsExactlyInAnyOrder(
						rowOf(firstTx.get(0), "SPOT_CONFIRMED"),
						rowOf(firstTx.get(1), "DEPARTURE_NOTICE_SENT"),
						rowOf(third, "CALL_TRIGGERED"),
						rowOf(otherFirst, "SESSION_STARTED"),
						rowOf(otherSecond, "VEHICLE_STOPPED"));
		assertThat(jdbcTemplate.queryForMap("""
				select payload->>'callId' as call_id, payload->>'trigger' as call_trigger
				from session_event
				where session_id = ? and seq = 3
				""", seeded.sessionId()))
				.as("세 번째 행 payload는 JSON 객체로 저장된다")
				.containsEntry("call_id", String.valueOf(CALL_ID))
				.containsEntry("call_trigger", "ETA_3MIN");
	}

	@Test
	@DisplayName("append로 seq 1을 저장한 세션에 같은 seq 1의 session_event 행을 또 넣으면 (sessionId, seq) 유니크 위반 DataIntegrityViolationException이 나고 그 seq 행은 append가 저장한 1건만 남으며, 다른 세션에는 같은 seq 1을 넣을 수 있다")
	void insertingSameSeqTwiceInOneSessionViolatesUniqueConstraint() {
		long otherSessionId = persistOtherSession();
		SessionEvent appended = transactionTemplate.execute(tx -> sessionEventPort.append(seeded.sessionId(),
				SessionEventType.SESSION_STARTED.name(), 0, T0, Map.of("childUserId", seeded.minjunId())));
		assertThat(appended.getSeq()).as("append가 매긴 첫 seq").isEqualTo(1L);

		Throwable thrown = catchThrowable(() -> insertEventRow(seeded.sessionId(), appended.getSeq(),
				SessionEventType.CALL_SENT, T0.plusSeconds(1)));

		assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
		assertThat(rootCauseOf(thrown).getMessage()).as("원인 메시지에 유니크 제약 이름이 있다")
				.containsIgnoringCase(UNIQUE_SEQ_PER_SESSION);
		assertThat(jdbcTemplate.queryForList("select event_id, type from session_event where session_id = ? and seq = ?",
				seeded.sessionId(), appended.getSeq()))
				.as("시드 세션의 seq 1 행")
				.extracting(row -> longOf(row, "event_id"), row -> row.get("type"))
				.containsExactly(tuple(appended.getEventId(), "SESSION_STARTED"));

		insertEventRow(otherSessionId, appended.getSeq(), SessionEventType.CALL_SENT, T0.plusSeconds(2));

		assertThat(jdbcTemplate.queryForObject("select count(*) from session_event where session_id = ? and seq = ?",
				Integer.class, otherSessionId, appended.getSeq())).as("다른 세션의 seq 1 행 수").isEqualTo(1);
	}

	@Test
	@DisplayName("loadAfter(afterSeq 1, limit 2)는 행을 넣은 순서(eventId)와 관계없이 seq 2 · 3을 오름차순으로 eventId · sessionId · type(SessionEventType) · stateVersion · occurredAt · payload와 함께 돌려주고 seq 4 · 5가 남아 hasMore true다")
	void loadAfterReturnsEventsAfterSeqInSeqOrderWithStoredValues() {
		Map<Long, Long> eventIdBySeq = persistShuffledEventsForLoadAfter();

		SessionEventPage page = sessionEventPort.loadAfter(seeded.sessionId(), 1, 2);

		assertThat(page.items())
				.extracting(SessionEvent::getEventId, SessionEvent::getSessionId, SessionEvent::getSeq,
						SessionEvent::getType, SessionEvent::getStateVersion, SessionEvent::getOccurredAt)
				.containsExactly(
						tuple(eventIdBySeq.get(2L), seeded.sessionId(), 2L, loadAfterTypeOf(2), loadAfterStateVersionOf(2),
								loadAfterOccurredAtOf(2)),
						tuple(eventIdBySeq.get(3L), seeded.sessionId(), 3L, loadAfterTypeOf(3), loadAfterStateVersionOf(3),
								loadAfterOccurredAtOf(3)));
		assertThat(page.items()).extracting(event -> longValue(event.getPayload(), "callId"))
				.as("payload callId")
				.containsExactly(CALL_ID + 2, CALL_ID + 3);
		assertThat(page.items()).extracting(event -> event.getPayload().get("trigger"))
				.as("payload trigger")
				.containsOnly("ETA_3MIN");
		assertThat(page.hasMore()).as("seq 4 · 5가 남는다").isTrue();
	}

	@ParameterizedTest(name = "afterSeq {0} · limit {1} → seq [{2}] · hasMore {3}")
	@CsvSource({
			"0, 4, '1,2,3,4', true",
			"1, 2, '2,3', true",
			"3, 2, '4,5', false",
			"2, 10, '3,4,5', false",
			"0, 5, '1,2,3,4,5', false",
			"5, 2, '', false"
	})
	@DisplayName("loadAfter는 그 세션에서 afterSeq보다 큰 이벤트만 seq 오름차순으로 최대 limit개 돌려주고, 그 뒤에 남은 이벤트가 있을 때만 hasMore true다(남은 수가 limit과 같거나 적으면 false, 다른 세션 이벤트는 보지 않는다)")
	void loadAfterReturnsAtMostLimitEventsAndHasMoreOnlyWhenEventsRemain(long afterSeq, int limit,
			String expectedSeqs, boolean expectedHasMore) {
		persistShuffledEventsForLoadAfter();
		List<Long> expected = expectedSeqs.isBlank() ? List.of()
				: Arrays.stream(expectedSeqs.split(",")).map(String::trim).map(Long::valueOf).toList();

		SessionEventPage page = sessionEventPort.loadAfter(seeded.sessionId(), afterSeq, limit);

		assertThat(page.items()).extracting(SessionEvent::getSeq).as("돌려준 seq").containsExactlyElementsOf(expected);
		assertThat(page.items()).extracting(SessionEvent::getSessionId).as("돌려준 sessionId")
				.allMatch(sessionId -> sessionId == seeded.sessionId());
		assertThat(page.hasMore()).as("hasMore").isEqualTo(expectedHasMore);
	}

	@Test
	@DisplayName("loadLastHelpRequest는 payload의 childUserId로 그 아이의 가장 최근 HELP_REQUESTED를 찾는다: 다른 아이의 더 늦은 HELP_REQUESTED, 같은 아이의 더 늦은 다른 type 이벤트, 다른 세션의 같은 아이 HELP_REQUESTED가 섞여도 그 세션 그 아이의 마지막 것만 돌려준다")
	void loadLastHelpRequestFindsLatestHelpRequestOfThatChildInThatSession() {
		long otherSessionId = persistOtherSession();
		List<Long> eventIds = persistEvents(List.of(
				helpRequest(seeded.sessionId(), 1, seeded.minjunId(), "NEED_HELP", T0),
				helpRequest(seeded.sessionId(), 2, seeded.seoyunId(), "NEED_HELP", T0.plusSeconds(10)),
				helpRequest(seeded.sessionId(), 3, seeded.minjunId(), "CANT_FIND_CAR", T0.plusSeconds(70)),
				prepTimeExtended(seeded.sessionId(), 4, seeded.minjunId(), T0.plusSeconds(80)),
				helpRequest(seeded.sessionId(), 5, seeded.seoyunId(), "CONTACT_PARENT", T0.plusSeconds(90)),
				helpRequest(otherSessionId, 1, seeded.minjunId(), "CONTACT_PARENT", T0.plusSeconds(200))));

		SessionEvent minjunLast = sessionEventPort.loadLastHelpRequest(seeded.sessionId(), seeded.minjunId())
				.orElseThrow();
		SessionEvent seoyunLast = sessionEventPort.loadLastHelpRequest(seeded.sessionId(), seeded.seoyunId())
				.orElseThrow();
		SessionEvent otherSessionMinjunLast = sessionEventPort.loadLastHelpRequest(otherSessionId, seeded.minjunId())
				.orElseThrow();

		assertThat(minjunLast)
				.extracting(SessionEvent::getEventId, SessionEvent::getSessionId, SessionEvent::getSeq,
						SessionEvent::getType, SessionEvent::getOccurredAt)
				.as("민준: seq 3(seq 4 PREP_TIME_EXTENDED · seq 5 서윤 HELP_REQUESTED · 다른 세션 것이 아님)")
				.containsExactly(eventIds.get(2), seeded.sessionId(), 3L, SessionEventType.HELP_REQUESTED,
						T0.plusSeconds(70));
		assertThat(longValue(minjunLast.getPayload(), "childUserId")).isEqualTo(seeded.minjunId());
		assertThat(minjunLast.getPayload()).containsEntry("type", "CANT_FIND_CAR");
		assertThat(seoyunLast)
				.extracting(SessionEvent::getEventId, SessionEvent::getSeq, SessionEvent::getType)
				.as("서윤: seq 5")
				.containsExactly(eventIds.get(4), 5L, SessionEventType.HELP_REQUESTED);
		assertThat(seoyunLast.getPayload()).containsEntry("type", "CONTACT_PARENT");
		assertThat(otherSessionMinjunLast)
				.extracting(SessionEvent::getEventId, SessionEvent::getSessionId, SessionEvent::getSeq)
				.as("다른 세션의 민준")
				.containsExactly(eventIds.get(5), otherSessionId, 1L);
	}

	@Test
	@DisplayName("loadLastHelpRequest는 그 세션에 그 아이의 HELP_REQUESTED가 없으면 빈 값이다: 다른 아이 HELP_REQUESTED만 있거나 그 아이의 다른 type 이벤트만 있을 때")
	void loadLastHelpRequestIsEmptyWhenThatChildHasNoHelpRequestInThatSession() {
		long otherSessionId = persistOtherSession();
		persistEvents(List.of(
				helpRequest(seeded.sessionId(), 1, seeded.minjunId(), "NEED_HELP", T0),
				helpRequest(otherSessionId, 1, seeded.minjunId(), "NEED_HELP", T0.plusSeconds(10)),
				prepTimeExtended(otherSessionId, 2, seeded.seoyunId(), T0.plusSeconds(20))));

		assertThat(sessionEventPort.loadLastHelpRequest(seeded.sessionId(), seeded.seoyunId()))
				.as("시드 세션에 민준 HELP_REQUESTED만 있을 때 서윤")
				.isEmpty();
		assertThat(sessionEventPort.loadLastHelpRequest(otherSessionId, seeded.seoyunId()))
				.as("다른 세션에 서윤 PREP_TIME_EXTENDED만 있을 때 서윤")
				.isEmpty();
	}

	@Test
	@DisplayName("loadFirst는 그 세션에서 그 type의 seq가 가장 작은 이벤트를 돌려주고, 다른 세션의 같은 type은 보지 않으며 그 type이 없으면 빈 값이다")
	void loadFirstReturnsSmallestSeqEventOfThatTypeInThatSession() {
		long otherSessionId = persistOtherSession();
		List<Long> eventIds = persistEvents(List.of(
				event(otherSessionId, 1, SessionEventType.SPOT_CONFIRMED, 0, T0.minusSeconds(60), "{\"spotVersion\":1}"),
				event(seeded.sessionId(), 1, SessionEventType.SESSION_STARTED, 0, T0, "{\"spotVersion\":0}"),
				event(seeded.sessionId(), 2, SessionEventType.SPOT_CONFIRMED, 1, T0.plusSeconds(10),
						"{\"spotVersion\":1}"),
				event(seeded.sessionId(), 3, SessionEventType.DEPARTURE_NOTICE_SENT, 1, T0.plusSeconds(11),
						"{\"spotVersion\":1}"),
				event(seeded.sessionId(), 4, SessionEventType.SPOT_CONFIRMED, 3, T0.plusSeconds(30),
						"{\"spotVersion\":2}")));

		SessionEvent first = sessionEventPort.loadFirst(seeded.sessionId(), SessionEventType.SPOT_CONFIRMED.name())
				.orElseThrow();

		assertThat(first)
				.extracting(SessionEvent::getEventId, SessionEvent::getSessionId, SessionEvent::getSeq,
						SessionEvent::getType, SessionEvent::getStateVersion, SessionEvent::getOccurredAt)
				.containsExactly(eventIds.get(2), seeded.sessionId(), 2L, SessionEventType.SPOT_CONFIRMED, 1,
						T0.plusSeconds(10));
		assertThat(longValue(first.getPayload(), "spotVersion")).isEqualTo(1L);
		assertThat(sessionEventPort.loadFirst(seeded.sessionId(), SessionEventType.CALL_SENT.name()))
				.as("그 세션에 없는 type")
				.isEmpty();
	}

	/** 시드 세션 seq 1~5를 SHUFFLED_SEQS 순서로, 다른 세션 seq 1~7을 넣는다. 시드 세션의 seq → eventId를 돌려준다. */
	private Map<Long, Long> persistShuffledEventsForLoadAfter() {
		long otherSessionId = persistOtherSession();
		List<EventRow> rows = new ArrayList<>();
		for (long seq : SHUFFLED_SEQS) {
			rows.add(event(seeded.sessionId(), seq, loadAfterTypeOf(seq), loadAfterStateVersionOf(seq),
					loadAfterOccurredAtOf(seq), "{\"callId\":%d,\"trigger\":\"ETA_3MIN\"}".formatted(CALL_ID + seq)));
		}
		for (long seq = 1; seq <= OTHER_SESSION_EVENT_COUNT; seq++) {
			rows.add(event(otherSessionId, seq, SessionEventType.CALL_SENT, 99, T0.plusSeconds(seq),
					"{\"callId\":%d,\"trigger\":\"ETA_3MIN\"}".formatted(9900 + seq)));
		}
		List<Long> eventIds = persistEvents(rows);
		Map<Long, Long> eventIdBySeq = new HashMap<>();
		for (int i = 0; i < SHUFFLED_SEQS.size(); i++) {
			eventIdBySeq.put(SHUFFLED_SEQS.get(i), eventIds.get(i));
		}
		return eventIdBySeq;
	}

	private static SessionEventType loadAfterTypeOf(long seq) {
		return List.of(SessionEventType.SESSION_STARTED, SessionEventType.SPOT_CONFIRMED,
				SessionEventType.CALL_TRIGGERED, SessionEventType.CALL_SENT, SessionEventType.CALL_DELIVERED)
				.get((int) seq - 1);
	}

	private static int loadAfterStateVersionOf(long seq) {
		return (int) seq + 10;
	}

	private static Instant loadAfterOccurredAtOf(long seq) {
		return T0.plusSeconds(seq * 10);
	}

	private static EventRow helpRequest(long sessionId, long seq, long childUserId, String helpType, Instant at) {
		return event(sessionId, seq, SessionEventType.HELP_REQUESTED, 0, at,
				"{\"childUserId\":%d,\"type\":\"%s\"}".formatted(childUserId, helpType));
	}

	private static EventRow prepTimeExtended(long sessionId, long seq, long childUserId, Instant at) {
		return event(sessionId, seq, SessionEventType.PREP_TIME_EXTENDED, 1, at,
				"{\"childUserId\":%d,\"extraPrepMinutes\":5}".formatted(childUserId));
	}

	private static EventRow event(long sessionId, long seq, SessionEventType type, int stateVersion, Instant at,
			String payloadJson) {
		return new EventRow(sessionId, seq, type, stateVersion, at, payloadJson);
	}

	/** 어댑터를 거치지 않고 순서대로 넣는다. 넣은 순서의 eventId를 돌려준다. */
	private List<Long> persistEvents(List<EventRow> rows) {
		return transactionTemplate.execute(tx -> {
			List<Long> eventIds = new ArrayList<>();
			for (EventRow row : rows) {
				SessionEventEntity entity = new SessionEventEntity(null, row.sessionId(), row.seq(), row.type().name(),
						row.stateVersion(), row.occurredAt(), row.payloadJson());
				entityManager.persist(entity);
				entityManager.flush();
				eventIds.add(entity.getEventId());
			}
			return eventIds;
		});
	}

	private void insertEventRow(long sessionId, long seq, SessionEventType type, Instant at) {
		jdbcTemplate.update("""
						insert into session_event (session_id, seq, type, state_version, occurred_at, payload,
						                           created_at, updated_at)
						values (?, ?, ?, 0, ?, cast(? as jsonb), now(), now())
						""", sessionId, seq, type.name(), at.atOffset(ZoneOffset.UTC),
				"{\"callId\":%d}".formatted(CALL_ID));
	}

	/** 시드 차량 · 가족의 전날 COMPLETED 세션(민준 포함). 시드 세션과 seq를 따로 매기는지 보는 다른 세션이다. */
	private long persistOtherSession() {
		return transactionTemplate.execute(tx -> {
			PickupSessionEntity session = new PickupSessionEntity(null, seeded.vehicleId(), seeded.familyId(),
					seeded.spotId(), PickupSeed.SPOT_NAME, GeoPoints.toPoint(PickupSeed.STOP_LAT, PickupSeed.STOP_LNG),
					GeoPoints.toPoint(PickupSeed.MEETING_LAT, PickupSeed.MEETING_LNG), seeded.date().minusDays(1),
					SessionStatus.COMPLETED, VehiclePhase.ROUTING, SpotStatus.SELECTED, 0, 0, 0, null, false, null,
					null, null, null, null, null, null, null, null, null);
			entityManager.persist(session);
			entityManager.persist(new SessionChildEntity(null, session.getSessionId(), seeded.minjunId(),
					seeded.minjunScheduleId(), ChildPhase.WAITING, (short) 0, null, null, null, null, null, null));
			entityManager.flush();
			return session.getSessionId();
		});
	}

	private static Tuple rowOf(SessionEvent event, String type) {
		return tuple(event.getEventId(), event.getSessionId(), event.getSeq(), type, (long) event.getStateVersion(),
				event.getOccurredAt().getEpochSecond(), "object");
	}

	private static long longOf(Map<String, Object> row, String column) {
		return ((Number) row.get(column)).longValue();
	}

	/** JSON에서 읽은 숫자는 Integer · Long 어느 쪽일 수 있어 long으로 맞춘다. */
	private static long longValue(Map<String, Object> payload, String key) {
		assertThat(payload).as("payload").containsKey(key);
		return ((Number) payload.get(key)).longValue();
	}

	private static Throwable rootCauseOf(Throwable thrown) {
		Throwable cause = thrown;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause;
	}

	private record EventRow(long sessionId, long seq, SessionEventType type, int stateVersion, Instant occurredAt,
			String payloadJson) {
	}

}
