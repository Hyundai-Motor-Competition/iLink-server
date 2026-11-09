package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.OutcomeView;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.application.port.out.LoadOutcomePort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.CorrectionReason;
import com.example.pickupservice.pickup.domain.CorrectionStatus;
import com.example.pickupservice.pickup.domain.PickupOutcome;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GetOutcomeServiceTest {

	private static final LocalDate SESSION_DATE_KST = LocalDate.of(2026, 11, 9);
	// 한국 시간 예상 정차 18:11:00, 실제 정차 18:11:50, 완료 18:12:30
	private static final Instant PREDICTED_STOP_AT = Instant.parse("2026-11-09T09:11:00Z");
	private static final Instant ACTUAL_STOP_AT = Instant.parse("2026-11-09T09:11:50Z");
	private static final Instant COMPLETED_AT = Instant.parse("2026-11-09T09:12:30Z");
	// 보호자가 정정한 정차 시각 18:11:20
	private static final Instant CORRECTED_STOP_AT = Instant.parse("2026-11-09T09:11:20Z");

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SPOT_ID = 21L;
	private static final String SPOT_NAME = "해솔영어학원 정문 앞";

	private static final long OTHER_FAMILY_ID = 13L;
	private static final long OTHER_FAMILY_GUARDIAN_ID = 91L;

	private static final LatLng STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng MEETING_POINT = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private LoadOutcomePort loadOutcomePort;

	private GetOutcomeService getOutcomeService;

	@BeforeEach
	void setUp() {
		getOutcomeService = new GetOutcomeService(loadSessionPort, loadOutcomePort);
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(completedSession()));
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(completedSession()));
	}

	@Test
	@DisplayName("픽업 결과는 예상 18:11:00 · 실제 18:11:50이면 diffSeconds 50을 completedAt · spotName · boardingMethod · 두 시각과 함께 돌려준다")
	void outcomeReturnsDiffSecondsBetweenPredictedAndActualStop() {
		givenOutcome(PickupOutcome.complete(SESSION_ID, COMPLETED_AT, BoardingMethod.DOOR, PREDICTED_STOP_AT,
				ACTUAL_STOP_AT));

		OutcomeView view = getOutcomeService.getOutcome(query(GUARDIAN_ID, FAMILY_ID));

		assertThat(view.sessionId()).isEqualTo(SESSION_ID);
		assertThat(view.completedAt()).isEqualTo(COMPLETED_AT);
		assertThat(view.spotName()).isEqualTo(SPOT_NAME);
		assertThat(view.boardingMethod()).isEqualTo("DOOR");
		assertThat(view.predictedStopAt()).isEqualTo(PREDICTED_STOP_AT);
		assertThat(view.actualStopAt()).isEqualTo(ACTUAL_STOP_AT);
		assertThat(view.diffSeconds()).isEqualTo(50L);
	}

	@Test
	@DisplayName("픽업 결과는 정정 시각이 있어도 diffSeconds를 실제 정차 시각 기준 50으로 주고 correction에 REQUESTED · 정정 시각을 담는다")
	void outcomeKeepsDiffSecondsOnActualStopWhenCorrected() {
		givenOutcome(PickupOutcome.restore(SESSION_ID, COMPLETED_AT, BoardingMethod.DOOR, PREDICTED_STOP_AT,
				ACTUAL_STOP_AT, CORRECTED_STOP_AT, CorrectionReason.WRONG_STOP_TIME, CorrectionStatus.REQUESTED,
				GUARDIAN_ID, "실제로는 18시 11분 20초쯤 섰어요"));

		OutcomeView view = getOutcomeService.getOutcome(query(GUARDIAN_ID, FAMILY_ID));

		assertThat(view.actualStopAt()).isEqualTo(ACTUAL_STOP_AT);
		assertThat(view.diffSeconds()).isEqualTo(50L);
		assertThat(view.correction()).isNotNull();
		assertThat(view.correction().status()).isEqualTo("REQUESTED");
		assertThat(view.correction().correctedStopAt()).isEqualTo(CORRECTED_STOP_AT);
	}

	@Test
	@DisplayName("픽업 결과는 결과가 아직 없는(완료 전) 세션이면 NOT_FOUND로 거절한다")
	void outcomeOfSessionWithoutOutcomeIsNotFound() {
		lenient().when(loadOutcomePort.loadOutcome(SESSION_ID)).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> getOutcomeService.getOutcome(query(GUARDIAN_ID, FAMILY_ID)));

		assertNotFound(exception);
	}

	@Test
	@DisplayName("픽업 결과는 다른 가족의 세션이면 결과가 있어도 NOT_FOUND로 거절한다")
	void outcomeOfOtherFamilySessionIsNotFound() {
		givenOutcome(PickupOutcome.complete(SESSION_ID, COMPLETED_AT, BoardingMethod.DOOR, PREDICTED_STOP_AT,
				ACTUAL_STOP_AT));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> getOutcomeService.getOutcome(query(OTHER_FAMILY_GUARDIAN_ID, OTHER_FAMILY_ID)));

		assertNotFound(exception);
	}

	private void givenOutcome(PickupOutcome outcome) {
		lenient().when(loadOutcomePort.loadOutcome(SESSION_ID)).thenReturn(Optional.of(outcome));
	}

	private static void assertNotFound(BusinessException exception) {
		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
	}

	private static PickupSession completedSession() {
		List<SessionChild> children = List.of(SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID,
				ChildPhase.MEETING, 0, null, null, null, ACTUAL_STOP_AT.minusSeconds(60), ACTUAL_STOP_AT.plusSeconds(20)));
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT, MEETING_POINT,
				SESSION_DATE_KST, SessionStatus.COMPLETED, VehiclePhase.STOPPED, SpotStatus.CONFIRMED, 15, 0, 1, 0,
				false, PREDICTED_STOP_AT, 0, ACTUAL_STOP_AT.minusSeconds(5), ACTUAL_STOP_AT.plusSeconds(20),
				ACTUAL_STOP_AT.plusSeconds(30), BoardingDoor.REAR_RIGHT, null, null, null, null, null, children);
	}

	private static SessionQuery query(long guardianId, long familyId) {
		AuthPrincipal guardian = new AuthPrincipal(PrincipalType.USER, guardianId, familyId, Role.GUARDIAN);
		return new SessionQuery(guardian, SESSION_ID);
	}

}
