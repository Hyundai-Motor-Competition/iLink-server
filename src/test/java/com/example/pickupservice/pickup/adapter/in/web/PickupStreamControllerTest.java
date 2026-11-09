package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.adapter.in.sse.SessionStreamRegistry;
import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.SESSION_ID;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.sessionView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
@TestPropertySource(properties = "pickup.sse.ping-interval=PT0.1S")
class PickupStreamControllerTest extends PickupWebTestSupport {

	private static final String STREAM_PATH = "/v1/pickup-sessions/{sessionId}/stream";
	private static final String LAST_EVENT_ID_HEADER = "Last-Event-ID";
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long LAST_EVENT_SEQ = 41L;
	private static final AuthPrincipal VEHICLE = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
	private static final AuthPrincipal USER = new AuthPrincipal(PrincipalType.USER, GUARDIAN_USER_ID, FAMILY_ID,
			Role.GUARDIAN);
	private static final Duration WAIT_LIMIT = Duration.ofSeconds(5);
	private static final long POLL_MILLIS = 20L;
	private static final Pattern PING_COMMENT = Pattern.compile("(?m)^:\\s*ping\\s*$");
	private static final String CALL_SENT_DATA = """
			{"eventId":9001,"seq":42,"stateVersion":16,"occurredAt":"2026-11-05T18:09:02+09:00",
			 "payload":{"callId":3301,"trigger":"ETA_3MIN"}}
			""";
	private static final String CALL_DELIVERED_DATA = """
			{"eventId":9002,"seq":43,"stateVersion":17,"occurredAt":"2026-11-05T18:09:04+09:00",
			 "payload":{"callId":3301}}
			""";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;
	private final SessionStreamRegistry sessionStreamRegistry;

	@AfterEach
	void closeStreams() {
		sessionStreamRegistry.complete(SESSION_ID);
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/stream은 그 세션 차량 토큰에 text/event-stream으로 응답한다")
	void streamRespondsWithEventStreamToSessionVehicle() throws Exception {
		given(getSessionUseCase.getSession(any(SessionQuery.class))).willReturn(sessionView());

		MvcResult result = mockMvc.perform(streamRequest(VEHICLE, null))
				.andExpect(status().isOk())
				.andExpect(request().asyncStarted())
				.andReturn();

		assertThat(result.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
		then(getSessionUseCase).should().getSession(new SessionQuery(VEHICLE, SESSION_ID));
		then(listSessionEventsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/stream을 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void streamWithUserTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(streamRequest(USER, null))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(getSessionUseCase).shouldHaveNoInteractions();
		then(listSessionEventsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/stream에 Last-Event-ID 41을 보내면 seq 42 이후 저장 이벤트부터 보낸다")
	void streamWithLastEventIdSendsStoredEventsAfterSeq() throws Exception {
		given(getSessionUseCase.getSession(any(SessionQuery.class))).willReturn(sessionView());
		given(listSessionEventsUseCase.listEvents(eq(SESSION_ID), eq(LAST_EVENT_SEQ), anyInt(), eq(VEHICLE)))
				.willReturn(new SessionEventPage(List.of(callSentEvent(), callDeliveredEvent()), false));

		MvcResult result = mockMvc.perform(streamRequest(VEHICLE, String.valueOf(LAST_EVENT_SEQ)))
				.andExpect(request().asyncStarted())
				.andReturn();

		then(listSessionEventsUseCase).should().listEvents(eq(SESSION_ID), eq(LAST_EVENT_SEQ), anyInt(), eq(VEHICLE));
		List<Map<String, String>> events = parseEvents(result.getResponse().getContentAsString());
		assertThat(events).extracting(event -> event.get("id")).containsExactly("42", "43");
		assertThat(events).extracting(event -> event.get("event")).containsExactly("CALL_SENT", "CALL_DELIVERED");
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/stream은 이어 받는 이벤트를 event · id(seq) · data(eventId · seq · stateVersion · occurredAt · payload) 형식으로 보낸다")
	void streamSendsStoredEventInEventIdDataFormat() throws Exception {
		given(getSessionUseCase.getSession(any(SessionQuery.class))).willReturn(sessionView());
		given(listSessionEventsUseCase.listEvents(eq(SESSION_ID), eq(LAST_EVENT_SEQ), anyInt(), eq(VEHICLE)))
				.willReturn(new SessionEventPage(List.of(callSentEvent()), false));

		MvcResult result = mockMvc.perform(streamRequest(VEHICLE, String.valueOf(LAST_EVENT_SEQ)))
				.andExpect(request().asyncStarted())
				.andReturn();

		List<Map<String, String>> events = parseEvents(result.getResponse().getContentAsString());
		assertThat(events).hasSize(1);
		assertThat(events.get(0)).containsEntry("event", "CALL_SENT").containsEntry("id", "42");
		JSONAssert.assertEquals(CALL_SENT_DATA, events.get(0).get("data"), true);
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/stream은 연결된 뒤 새로 커밋된 이벤트도 같은 형식으로 보낸다")
	void streamSendsLiveEventInEventIdDataFormat() throws Exception {
		given(getSessionUseCase.getSession(any(SessionQuery.class))).willReturn(sessionView());

		MvcResult result = mockMvc.perform(streamRequest(VEHICLE, null))
				.andExpect(request().asyncStarted())
				.andReturn();
		sessionStreamRegistry.send(SESSION_ID, callSentEvent());

		List<Map<String, String>> events = awaitEvents(result, 1);
		assertThat(events.get(0)).containsEntry("event", "CALL_SENT").containsEntry("id", "42");
		JSONAssert.assertEquals(CALL_SENT_DATA, events.get(0).get("data"), true);
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/stream은 ping 간격마다 빈 주석 `: ping`을 보낸다")
	void streamSendsPingCommentEveryInterval() throws Exception {
		given(getSessionUseCase.getSession(any(SessionQuery.class))).willReturn(sessionView());

		MvcResult result = mockMvc.perform(streamRequest(VEHICLE, null))
				.andExpect(request().asyncStarted())
				.andReturn();

		awaitPings(result, 2);
		assertThat(parseEvents(result.getResponse().getContentAsString())).isEmpty();
	}

	private List<Map<String, String>> awaitEvents(MvcResult result, int count) throws Exception {
		long deadline = System.nanoTime() + WAIT_LIMIT.toNanos();
		List<Map<String, String>> events = parseEvents(result.getResponse().getContentAsString());
		while (events.size() < count && System.nanoTime() < deadline) {
			Thread.sleep(POLL_MILLIS);
			events = parseEvents(result.getResponse().getContentAsString());
		}
		assertThat(events).hasSizeGreaterThanOrEqualTo(count);
		return events;
	}

	private void awaitPings(MvcResult result, int count) throws Exception {
		long deadline = System.nanoTime() + WAIT_LIMIT.toNanos();
		int pings = countPings(result.getResponse().getContentAsString());
		while (pings < count && System.nanoTime() < deadline) {
			Thread.sleep(POLL_MILLIS);
			pings = countPings(result.getResponse().getContentAsString());
		}
		assertThat(pings).isGreaterThanOrEqualTo(count);
	}

	private static int countPings(String body) {
		return (int) PING_COMMENT.matcher(body).results().count();
	}

	/** 빈 줄로 나뉜 블록을 필드 이름(event · id · data) 기준으로 읽는다. 주석(`:`)만 있는 블록은 뺀다. */
	private static List<Map<String, String>> parseEvents(String body) {
		List<Map<String, String>> events = new ArrayList<>();
		for (String block : body.split("\\R\\R")) {
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

	private static SessionEvent callSentEvent() {
		return SessionEvent.restore(9001L, SESSION_ID, 42L, SessionEventType.CALL_SENT, 16,
				Instant.parse("2026-11-05T09:09:02Z"), Map.of("callId", 3301, "trigger", "ETA_3MIN"));
	}

	private static SessionEvent callDeliveredEvent() {
		return SessionEvent.restore(9002L, SESSION_ID, 43L, SessionEventType.CALL_DELIVERED, 17,
				Instant.parse("2026-11-05T09:09:04Z"), Map.of("callId", 3301));
	}

	private MockHttpServletRequestBuilder streamRequest(AuthPrincipal principal, String lastEventId) {
		MockHttpServletRequestBuilder builder = get(STREAM_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.accept(MediaType.TEXT_EVENT_STREAM);
		if (lastEventId != null) {
			builder.header(LAST_EVENT_ID_HEADER, lastEventId);
		}
		return builder;
	}

	private String bearerOf(AuthPrincipal principal) {
		if (principal.type() == PrincipalType.VEHICLE) {
			return bearer(accessTokenFixture.vehicle(principal.id(), principal.familyId()));
		}
		return bearer(accessTokenFixture.user(principal.id(), principal.familyId(), principal.role()));
	}

}
