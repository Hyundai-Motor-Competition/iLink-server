package com.example.pickupservice.pickup.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SessionEventTest {

	private static final long EVENT_ID = 501L;
	private static final long SESSION_ID = 7L;
	private static final long SEQ = 12L;
	private static final int STATE_VERSION = 4;
	private static final Instant OCCURRED_AT = Instant.parse("2026-11-09T09:10:30Z");
	private static final long CHILD_USER_ID = 31L;

	@Test
	@DisplayName("SessionEvent.restore는 eventId · sessionId · seq · type · stateVersion · occurredAt · payload를 그대로 되살린다")
	void restoreKeepsAllFields() {
		Map<String, Object> payload = Map.of("childUserId", CHILD_USER_ID);

		SessionEvent event = SessionEvent.restore(EVENT_ID, SESSION_ID, SEQ, SessionEventType.HELP_REQUESTED,
				STATE_VERSION, OCCURRED_AT, payload);

		assertThat(event.getEventId()).isEqualTo(EVENT_ID);
		assertThat(event.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(event.getSeq()).isEqualTo(SEQ);
		assertThat(event.getType()).isEqualTo(SessionEventType.HELP_REQUESTED);
		assertThat(event.getStateVersion()).isEqualTo(STATE_VERSION);
		assertThat(event.getOccurredAt()).isEqualTo(OCCURRED_AT);
		assertThat(event.getPayload()).containsExactlyEntriesOf(payload);
	}

	@Test
	@DisplayName("SessionEventType은 VEHICLE_APPROACHING · PREPARE_NOTICE_SENT · CHILD_READY를 더한 24종을 설계서 「실시간 이벤트」 표 순서대로 갖는다")
	void sessionEventTypeHasTwentyFourNamesInDesignTableOrder() {
		assertThat(Arrays.stream(SessionEventType.values()).map(Enum::name))
				.containsExactly(
						"SESSION_STARTED",
						"SESSION_COMPLETED",
						"SESSION_CANCELLED",
						"SESSION_HELD",
						"SESSION_RESUMED",
						"INCIDENT_OPENED",
						"SPOT_CONFIRMED",
						"SPOT_CHANGED",
						"DEPARTURE_NOTICE_SENT",
						"ETA_UPDATED",
						"VEHICLE_LOCATION",
						"VEHICLE_APPROACHING",
						"VEHICLE_STOPPED",
						"VEHICLE_DEPARTED",
						"BOARDING_DETECTED",
						"CALL_TRIGGERED",
						"CALL_SENT",
						"CALL_DELIVERED",
						"CALL_ACKNOWLEDGED",
						"CALL_REVOKED",
						"PREP_TIME_EXTENDED",
						"PREPARE_NOTICE_SENT",
						"CHILD_READY",
						"HELP_REQUESTED");
	}
}
