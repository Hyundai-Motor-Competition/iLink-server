package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendGuardServiceTest {

	private static final long SESSION_ID = 501L;
	private static final long CALL_ID = 3001L;
	private static final Instant ACCEPTED_AT = Instant.parse("2026-11-09T09:09:00Z");

	@Mock
	private LoadSessionPort loadSessionPort;
	@Mock
	private LoadCallPort loadCallPort;

	private ResendGuardService service;

	@BeforeEach
	void setUp() {
		service = new ResendGuardService(loadSessionPort, loadCallPort);
	}

	@ParameterizedTest
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("isSessionEnded는 세션이 완료(COMPLETED)나 취소(CANCELLED)면 true다")
	void sessionIsEndedWhenCompletedOrCancelled(SessionStatus status) {
		givenSession(status);

		assertThat(service.isSessionEnded(SESSION_ID)).isTrue();
	}

	@ParameterizedTest
	@EnumSource(value = SessionStatus.class, names = {"ACTIVE", "ON_HOLD"})
	@DisplayName("isSessionEnded는 세션이 진행 중(ACTIVE)이거나 보류(ON_HOLD)면 false다")
	void sessionIsNotEndedWhenActiveOrOnHold(SessionStatus status) {
		givenSession(status);

		assertThat(service.isSessionEnded(SESSION_ID)).isFalse();
	}

	@Test
	@DisplayName("isCallRevoked는 호출 상태가 REVOKED면 true다")
	void callIsRevokedWhenStatusIsRevoked() {
		givenCall(CallStatus.REVOKED);

		assertThat(service.isCallRevoked(CALL_ID)).isTrue();
	}

	@ParameterizedTest
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED", "ACKNOWLEDGED"})
	@DisplayName("isCallRevoked는 호출 상태가 REVOKED가 아니면 false다")
	void callIsNotRevokedWhenStatusIsNotRevoked(CallStatus status) {
		givenCall(status);

		assertThat(service.isCallRevoked(CALL_ID)).isFalse();
	}

	private void givenSession(SessionStatus status) {
		PickupSession session = mock(PickupSession.class);
		when(session.getSessionStatus()).thenReturn(status);
		when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private void givenCall(CallStatus status) {
		Instant sentAt = status == CallStatus.ACCEPTED ? null : ACCEPTED_AT.plusSeconds(2);
		Instant revokedAt = status == CallStatus.REVOKED ? ACCEPTED_AT.plusSeconds(10) : null;
		String revokeReason = status == CallStatus.REVOKED ? "아이가 아직 준비 중" : null;
		Instant deliveredAt = status == CallStatus.DELIVERED || status == CallStatus.ACKNOWLEDGED
				? ACCEPTED_AT.plusSeconds(5) : null;
		Instant acknowledgedAt = status == CallStatus.ACKNOWLEDGED ? ACCEPTED_AT.plusSeconds(20) : null;
		Call call = Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, status, 1, ACCEPTED_AT, sentAt,
				deliveredAt, acknowledgedAt, revokedAt, revokeReason);
		when(loadCallPort.loadCall(CALL_ID)).thenReturn(Optional.of(call));
	}
}
