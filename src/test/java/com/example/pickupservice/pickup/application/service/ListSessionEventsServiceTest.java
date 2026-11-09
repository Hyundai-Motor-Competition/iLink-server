package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ListSessionEventsServiceTest {

	// 한국 시간 2026-11-09(월) 17:50
	private static final Instant NOW = Instant.parse("2026-11-09T08:50:00Z");
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final int STATE_VERSION = 8;

	private static final long AFTER_SEQ = 4L;
	private static final int LIMIT = 2;

	// 이 세션을 볼 수 없는 주체용
	private static final long OTHER_FAMILY_ID = 13L;
	private static final long OTHER_FAMILY_VEHICLE_ID = 78L;
	private static final long OTHER_FAMILY_GUARDIAN_ID = 91L;
	private static final long SIBLING_NOT_IN_SESSION_ID = 36L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@ParameterizedTest(name = "{0}")
	@MethodSource("principalsInSession")
	@DisplayName("이벤트 목록은 세션을 볼 수 있는 주체에게 afterSeq 뒤 이벤트를 limit개까지와 hasMore를 SessionEventPort가 준 그대로 돌려준다")
	void listEventsReturnsEventsAfterSeqWithHasMoreForVisiblePrincipal(AuthPrincipal principal) {
		givenSession(session());
		SessionEventPage page = new SessionEventPage(List.of(event(5L, "SPOT_CONFIRMED"),
				event(6L, "VEHICLE_STOPPED")), true);
		lenient().when(sessionEventPort.loadAfter(SESSION_ID, AFTER_SEQ, LIMIT)).thenReturn(page);

		SessionEventPage result = service().listEvents(SESSION_ID, AFTER_SEQ, LIMIT, principal);

		verify(sessionEventPort).loadAfter(SESSION_ID, AFTER_SEQ, LIMIT);
		assertThat(result.items()).extracting(SessionEvent::getSeq).containsExactly(5L, 6L);
		assertThat(result.items())
				.extracting(event -> event.getType().name())
				.containsExactly("SPOT_CONFIRMED", "VEHICLE_STOPPED");
		assertThat(result.hasMore()).isTrue();
	}

	@Test
	@DisplayName("이벤트 목록은 afterSeq 뒤 남은 이벤트가 limit보다 적으면 그 이벤트만과 hasMore false를 돌려준다")
	void listEventsReturnsRemainingEventsWithoutMore() {
		givenSession(session());
		SessionEventPage lastPage = new SessionEventPage(List.of(event(7L, "CALL_TRIGGERED")), false);
		lenient().when(sessionEventPort.loadAfter(SESSION_ID, 6L, LIMIT)).thenReturn(lastPage);

		SessionEventPage result = service().listEvents(SESSION_ID, 6L, LIMIT, vehicle(VEHICLE_ID));

		assertThat(result.items()).extracting(SessionEvent::getSeq).containsExactly(7L);
		assertThat(result.hasMore()).isFalse();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("principalsOutsideSession")
	@DisplayName("이벤트 목록은 세션을 볼 수 없는 주체면 NOT_FOUND로 거절하고 이벤트를 읽지 않는다")
	void listEventsRejectsPrincipalOutsideSessionWithNotFound(AuthPrincipal principal) {
		givenSession(session());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> service().listEvents(SESSION_ID, AFTER_SEQ, LIMIT, principal));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		verifyNoInteractions(sessionEventPort);
	}

	@Test
	@DisplayName("이벤트 목록은 세션이 없으면 NOT_FOUND로 거절하고 이벤트를 읽지 않는다")
	void listEventsRejectsMissingSessionWithNotFound() {
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> service().listEvents(SESSION_ID, AFTER_SEQ, LIMIT, guardian()));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		verifyNoInteractions(sessionEventPort);
	}

	static Stream<Named<AuthPrincipal>> principalsInSession() {
		return Stream.of(
				Named.of("세션의 차량", vehicle(VEHICLE_ID)),
				Named.of("가족 GUARDIAN", guardian()),
				Named.of("세션에 있는 자녀", new AuthPrincipal(PrincipalType.USER, MINJUN_ID, FAMILY_ID, Role.CHILD)));
	}

	// 차량은 vehicleId, 사용자는 familyId, 자녀는 세션에 자기가 있는지로 본다
	static Stream<Named<AuthPrincipal>> principalsOutsideSession() {
		return Stream.of(
				Named.of("다른 가족의 차량", vehicle(OTHER_FAMILY_VEHICLE_ID)),
				Named.of("다른 가족의 GUARDIAN",
						new AuthPrincipal(PrincipalType.USER, OTHER_FAMILY_GUARDIAN_ID, OTHER_FAMILY_ID,
								Role.GUARDIAN)),
				Named.of("세션에 없는 같은 가족 자녀",
						new AuthPrincipal(PrincipalType.USER, SIBLING_NOT_IN_SESSION_ID, FAMILY_ID, Role.CHILD)));
	}

	private ListSessionEventsService service() {
		return new ListSessionEventsService(loadSessionPort, sessionEventPort);
	}

	private void givenSession(PickupSession session) {
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private static AuthPrincipal guardian() {
		return new AuthPrincipal(PrincipalType.USER, GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN);
	}

	private static AuthPrincipal vehicle(long vehicleId) {
		return new AuthPrincipal(PrincipalType.VEHICLE, vehicleId, FAMILY_ID, null);
	}

	private static SessionEvent event(long seq, String type) {
		return SessionEvent.restore(9000L + seq, SESSION_ID, seq, SessionEventType.valueOf(type), STATE_VERSION,
				NOW.minusSeconds(60 - seq), Map.of());
	}

	private static PickupSession session() {
		List<SessionChild> children = List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, 0, 1, 0, false, NOW.plus(Duration.ofMinutes(6)), 2400,
				NOW.minusSeconds(10), null, null, null, null, null, null, null, children);
	}

}
